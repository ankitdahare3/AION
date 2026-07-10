package com.aion.host.automation

/** Instantiated by Shizuku inside its privileged process — see [IShellUserService]. */
class ShellUserService : IShellUserService.Stub() {
    override fun exec(command: String): String {
        val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output
    }
}
