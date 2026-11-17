package com.example.shizuku

import android.os.Bundle
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

@Keep
class UserService : IUserService.Stub {
    constructor() : super()
    constructor(@Suppress("UNUSED_PARAMETER") bundle: Bundle?) : super()

    override fun destroy() {
        exitProcess(0)
    }

    override fun executeCommand(command: String?): String {
        if (command.isNullOrBlank()) return "ERR: empty command"
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readText()
            }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                output.trim().ifEmpty { "OK" }
            } else {
                "ERR($exitCode): ${output.trim()}"
            }
        } catch (t: Throwable) {
            "ERR: ${t.message ?: "execution failed"}"
        }
    }
}
