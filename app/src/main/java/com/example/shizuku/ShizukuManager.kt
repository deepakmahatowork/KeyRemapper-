package com.example.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku

enum class ShizukuBridgeStatus {
    NOT_INSTALLED,
    STOPPED,
    NEEDS_PERMISSION,
    READY,
}

data class ShizukuUiSnapshot(
    val status: ShizukuBridgeStatus = ShizukuBridgeStatus.STOPPED,
    val essentialSpaceDisabled: Boolean? = null,
    val essentialRecorderDisabled: Boolean? = null,
    val lastCommandOutput: String = "",
)

object ShizukuManager {
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val NOTHING_ESSENTIAL_SPACE = "com.nothing.ntessentialspace"
    const val NOTHING_ESSENTIAL_RECORDER = "com.nothing.ntessentialrecorder"
    private const val REQUEST_CODE_SHIZUKU = 1042

    private val _snapshot = MutableStateFlow(ShizukuUiSnapshot())
    val snapshot: StateFlow<ShizukuUiSnapshot> = _snapshot.asStateFlow()

    private val _userService = MutableStateFlow<IUserService?>(null)
    private val bindMutex = Mutex()
    private var listenersRegistered = false
    private var appContext: Context? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _userService.value = null
        refreshStatus()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                refreshStatus()
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.isBinderAlive) {
                _userService.value = IUserService.Stub.asInterface(binder)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _userService.value = null
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        if (!listenersRegistered) {
            listenersRegistered = true
            try {
                Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
                Shizuku.addBinderDeadListener(binderDeadListener)
                Shizuku.addRequestPermissionResultListener(permissionResultListener)
            } catch (_: Throwable) {
            }
        }
        refreshStatus()
    }

    fun isReady(): Boolean = isBinderAliveSafe() && hasPermissionSafe()

    fun refreshStatus() {
        val ctx = appContext ?: return
        val installed = isPackageInstalled(ctx, SHIZUKU_PACKAGE)
        val status = when {
            !installed -> ShizukuBridgeStatus.NOT_INSTALLED
            !isBinderAliveSafe() -> ShizukuBridgeStatus.STOPPED
            !hasPermissionSafe() -> ShizukuBridgeStatus.NEEDS_PERMISSION
            else -> ShizukuBridgeStatus.READY
        }
        _snapshot.value = _snapshot.value.copy(status = status)
    }

    fun requestPermission() {
        try {
            if (isBinderAliveSafe() && !hasPermissionSafe()) {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            }
        } catch (_: Throwable) {
            refreshStatus()
        }
    }

    suspend fun disableNothingEssentialPackages(): String = withContext(Dispatchers.IO) {
        val results = mutableListOf<String>()
        for (pkg in listOf(NOTHING_ESSENTIAL_SPACE, NOTHING_ESSENTIAL_RECORDER)) {
            val res = runCommand("pm disable-user --user 0 $pkg")
            results += "$pkg: $res"
        }
        val combined = results.joinToString("\n")
        refreshNothingPackageStates()
        _snapshot.value = _snapshot.value.copy(lastCommandOutput = combined)
        combined
    }

    suspend fun restoreNothingEssentialPackages(): String = withContext(Dispatchers.IO) {
        val results = mutableListOf<String>()
        for (pkg in listOf(NOTHING_ESSENTIAL_SPACE, NOTHING_ESSENTIAL_RECORDER)) {
            val res = runCommand("pm enable $pkg")
            results += "$pkg: $res"
        }
        val combined = results.joinToString("\n")
        refreshNothingPackageStates()
        _snapshot.value = _snapshot.value.copy(lastCommandOutput = combined)
        combined
    }

    suspend fun refreshNothingPackageStates() = withContext(Dispatchers.IO) {
        if (!isBinderAliveSafe() || !hasPermissionSafe()) {
            refreshStatus()
            return@withContext
        }
        val disabledList = runCommand("pm list packages -d")
        val spaceDisabled = if (disabledList.startsWith("ERR")) null else disabledList.contains(NOTHING_ESSENTIAL_SPACE)
        val recorderDisabled = if (disabledList.startsWith("ERR")) null else disabledList.contains(NOTHING_ESSENTIAL_RECORDER)
        _snapshot.value = _snapshot.value.copy(
            essentialSpaceDisabled = spaceDisabled,
            essentialRecorderDisabled = recorderDisabled,
        )
    }

    suspend fun injectKeyEvent(keyCode: Int): Result<String> = withContext(Dispatchers.IO) {
        if (!isBinderAliveSafe() || !hasPermissionSafe()) {
            return@withContext Result.failure(IllegalStateException("Shizuku bridge not ready"))
        }
        val output = runCommand("input keyevent $keyCode")
        if (output.startsWith("ERR")) {
            Result.failure(IllegalStateException(output))
        } else {
            Result.success(output)
        }
    }

    suspend fun runCommand(command: String): String = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext "ERR: context not initialized"
        if (!isBinderAliveSafe()) return@withContext "ERR: Shizuku service not running"
        if (!hasPermissionSafe()) return@withContext "ERR: Shizuku permission denied"
        return@withContext try {
            val service = getOrBindService(ctx)
            service.executeCommand(command)
        } catch (t: Throwable) {
            "ERR: ${t.message ?: "Shizuku command failed"}"
        }
    }

    private suspend fun getOrBindService(context: Context): IUserService {
        return bindMutex.withLock {
            val current = _userService.value
            if (current != null && current.asBinder().isBinderAlive) {
                return@withLock current
            }
            val args = Shizuku.UserServiceArgs(
                ComponentName(context.packageName, UserService::class.java.name),
            )
                .daemon(false)
                .processNameSuffix("keyremap_shizuku")
                .tag("KeyRemapUserService")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)

            Shizuku.bindUserService(args, serviceConnection)
            withTimeout(5_000L) {
                _userService.filterNotNull().first()
            }
        }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isBinderAliveSafe(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    private fun hasPermissionSafe(): Boolean = try {
        if (Shizuku.isPreV11()) false
        else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }
}
