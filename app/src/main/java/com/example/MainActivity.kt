package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.preferences.RemapperPreferences.Companion.GLOBAL_LABEL
import com.example.data.preferences.RemapperPreferences.Companion.GLOBAL_PACKAGE
import com.example.model.ActionType
import com.example.model.RemapRule
import com.example.ui.components.ActionPickerSheet
import com.example.ui.components.AppPickerSheet
import com.example.ui.components.KeyCaptureDialog
import com.example.ui.navigation.DashboardRoute
import com.example.ui.navigation.SetupRoute
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.theme.KeyRemapTheme
import com.example.ui.viewmodel.EditingRuleTarget
import com.example.ui.viewmodel.RemapperViewModel

private data class TopNavItem(
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

class MainActivity : ComponentActivity() {

    private val viewModel: RemapperViewModel by viewModels {
        RemapperViewModel.Factory(application)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)

            KeyRemapTheme(dynamicColor = uiState.settings.dynamicColorEnabled) {
                KeyRemapApp(
                    viewModel = viewModel,
                    widthSizeClass = windowSizeClass.widthSizeClass,
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && viewModel.onActivityHardwareKeyDown(event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyRemapApp(
    viewModel: RemapperViewModel,
    widthSizeClass: WindowWidthSizeClass,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingRuleTarget by remember { mutableStateOf<EditingRuleTarget?>(null) }
    var showAppPickerSheet by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSystemHealth()
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navItems = remember {
        listOf(
            TopNavItem("Remap", DashboardRoute, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
            TopNavItem("Setup", SetupRoute, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_setup"),
        )
    }

    val useNavRail = widthSizeClass != WindowWidthSizeClass.Compact

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "KeyRemap",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (!useNavRail) {
                NavigationBar {
                    navItems.forEach { item ->
                        val selected = backStackEntry?.destination?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag(item.testTag),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (useNavRail) {
                NavigationRail {
                    navItems.forEach { item ->
                        val selected = backStackEntry?.destination?.hasRoute(item.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag(item.testTag),
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = DashboardRoute,
                ) {
                    composable<DashboardRoute> {
                        DashboardScreen(
                            uiState = uiState,
                            onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings(context) },
                            onOpenSetupTab = { navigateToTab(SetupRoute) },
                            onStartKeyCapture = { viewModel.setKeyCaptureMode(true) },
                            onSimulateGesture = { gesture -> viewModel.simulateGestureTest(gesture) },
                            onEditGlobalRule = { gesture, existing ->
                                editingRuleTarget = EditingRuleTarget(
                                    profilePackage = GLOBAL_PACKAGE,
                                    profileLabel = GLOBAL_LABEL,
                                    rule = existing ?: RemapRule(
                                        profilePackage = GLOBAL_PACKAGE,
                                        profileLabel = GLOBAL_LABEL,
                                        gesture = gesture,
                                        actionType = ActionType.NONE,
                                        isEnabled = false,
                                    ),
                                )
                            },
                            onToggleGlobalRule = { gesture, enabled ->
                                viewModel.toggleRuleEnabled(
                                    profilePackage = GLOBAL_PACKAGE,
                                    profileLabel = GLOBAL_LABEL,
                                    gesture = gesture,
                                    enabled = enabled,
                                )
                            },
                            onOpenAddAppSheet = {
                                viewModel.loadInstalledApps()
                                showAppPickerSheet = true
                            },
                            onToggleAppProfile = { pkg, enabled -> viewModel.toggleAppProfile(pkg, enabled) },
                            onDeleteAppProfile = { profile -> viewModel.deleteAppProfile(profile) },
                            onEditProfileRule = { profile, gesture, existing ->
                                editingRuleTarget = EditingRuleTarget(
                                    profilePackage = profile.packageName,
                                    profileLabel = profile.appName,
                                    rule = existing ?: RemapRule(
                                        profilePackage = profile.packageName,
                                        profileLabel = profile.appName,
                                        gesture = gesture,
                                        actionType = ActionType.NONE,
                                        isEnabled = false,
                                    ),
                                )
                            },
                            onToggleProfileRule = { profile, gesture, enabled ->
                                viewModel.toggleRuleEnabled(
                                    profilePackage = profile.packageName,
                                    profileLabel = profile.appName,
                                    gesture = gesture,
                                    enabled = enabled,
                                )
                            },
                        )
                    }

                    composable<SetupRoute> {
                        SetupScreen(
                            uiState = uiState,
                            onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings(context) },
                            onOpenDefaultAssistantSettings = { viewModel.openDefaultAssistantSettings(context) },
                            onOpenAppInfoSettings = { viewModel.openAppDetailsSettings(context) },
                            onOpenDndPolicySettings = { viewModel.openDndPolicySettings(context) },
                            onOpenShizukuOrStore = { viewModel.openShizukuOrStore(context) },
                            onRequestShizukuPermission = { viewModel.requestShizukuPermission() },
                            onDisableNothingEssential = { viewModel.disableNothingDefaultHandlers() },
                            onRestoreNothingEssential = { viewModel.restoreNothingDefaultHandlers() },
                            onStartKeyCapture = { viewModel.setKeyCaptureMode(true) },
                            onUpdateDoublePressDelay = { viewModel.updateDoublePressDelay(it) },
                            onUpdateLongPressThreshold = { viewModel.updateLongPressThreshold(it) },
                            onSetHapticFeedback = { viewModel.setHapticFeedback(it) },
                            onSetBlockDefaultKey = { viewModel.setBlockDefaultKey(it) },
                            onSetWorkWhenScreenOff = { viewModel.setWorkWhenScreenOff(it) },
                            onSetDynamicColor = { viewModel.setDynamicColor(it) },
                            onResetDefaults = { viewModel.resetToDefaults() },
                        )
                    }
                }
            }
        }
    }

    editingRuleTarget?.let { target ->
        ActionPickerSheet(
            target = target,
            installedApps = uiState.installedApps,
            onDismiss = { editingRuleTarget = null },
            onSave = { actionType, payload, payloadLabel, constraint ->
                viewModel.updateRule(
                    profilePackage = target.profilePackage,
                    profileLabel = target.profileLabel,
                    gesture = target.rule.gesture,
                    actionType = actionType,
                    actionPayload = payload,
                    actionPayloadLabel = payloadLabel,
                    constraint = constraint,
                    isEnabled = actionType != ActionType.NONE,
                )
                editingRuleTarget = null
            },
        )
    }

    if (showAppPickerSheet) {
        AppPickerSheet(
            installedApps = uiState.installedApps,
            existingProfilePackages = uiState.appProfiles.map { it.packageName }.toSet(),
            isLoading = uiState.appsLoading,
            onDismiss = { showAppPickerSheet = false },
            onSelectApp = { app ->
                viewModel.addAppProfile(app)
                showAppPickerSheet = false
            },
        )
    }

    if (uiState.isCapturingKey) {
        KeyCaptureDialog(
            currentKeyCode = uiState.settings.targetKeyCode,
            currentKeyLabel = uiState.settings.targetKeyLabel,
            onSelectPreset = { code ->
                viewModel.setTargetKeyPreset(code)
                viewModel.setKeyCaptureMode(false)
            },
            onDismiss = { viewModel.setKeyCaptureMode(false) },
        )
    }
}
