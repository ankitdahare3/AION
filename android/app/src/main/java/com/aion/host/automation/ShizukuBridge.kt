package com.aion.host.automation

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

sealed class ShizukuResult {
    data class Success(
        val stdout: String,
        val exitCode: Int,
    ) : ShizukuResult()

    object Unavailable : ShizukuResult()

    object PermissionDenied : ShizukuResult()

    data class Failure(
        val reason: String,
    ) : ShizukuResult()
}

/**
 * DOC-009 §3 — Shizuku-backed `input`/`am`/`pm` shell execution, used only when a11y is
 * insufficient (T-041's ActionDispatcher is the primary path). Shizuku itself is an optional
 * third-party app almost no real user will have installed, so every entry point degrades
 * gracefully instead of throwing — callers treat [ShizukuResult.Unavailable] as "fall back to a11y".
 */
@Singleton
class ShizukuBridge
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun isAvailable(): Boolean =
            try {
                Shizuku.pingBinder()
            } catch (e: Throwable) {
                false
            }

        fun hasPermission(): Boolean =
            isAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

        suspend fun requestPermission(): Boolean {
            if (!isAvailable()) return false
            if (hasPermission()) return true
            return suspendCancellableCoroutine { cont ->
                val listener =
                    object : Shizuku.OnRequestPermissionResultListener {
                        override fun onRequestPermissionResult(
                            requestCode: Int,
                            grantResult: Int,
                        ) {
                            Shizuku.removeRequestPermissionResultListener(this)
                            if (cont.isActive) {
                                cont.resumeWith(Result.success(grantResult == PackageManager.PERMISSION_GRANTED))
                            }
                        }
                    }
                Shizuku.addRequestPermissionResultListener(listener)
                Shizuku.requestPermission(REQUEST_CODE)
            }
        }

        /**
         * Runs [command] via `sh -c`, e.g. `"input tap 500 900"`, `"am start -n pkg/.Activity"`.
         * Binds a fresh [ShellUserService] per call (Shizuku privatized direct shell exec) —
         * ponytail: no persistent cached connection, add one if per-call bind latency matters.
         */
        suspend fun exec(command: String): ShizukuResult {
            if (!isAvailable()) return ShizukuResult.Unavailable
            if (!hasPermission()) return ShizukuResult.PermissionDenied
            return try {
                val service =
                    suspendCancellableCoroutine<IShellUserService?> { cont ->
                        val connection =
                            object : ServiceConnection {
                                override fun onServiceConnected(
                                    name: ComponentName?,
                                    binder: IBinder?,
                                ) {
                                    val svc = binder?.let { IShellUserService.Stub.asInterface(it) }
                                    if (cont.isActive) cont.resumeWith(Result.success(svc))
                                }

                                override fun onServiceDisconnected(name: ComponentName?) {
                                    if (cont.isActive) cont.resumeWith(Result.success(null))
                                }
                            }
                        val args =
                            Shizuku
                                .UserServiceArgs(ComponentName(context, ShellUserService::class.java))
                                .daemon(false)
                                .processNameSuffix("shell")
                                .debuggable(false)
                                .version(1)
                        Shizuku.bindUserService(args, connection)
                    } ?: return ShizukuResult.Failure("failed to bind Shizuku user service")
                val output = withContext(Dispatchers.IO) { service.exec(command) }
                ShizukuResult.Success(output, 0)
            } catch (e: Exception) {
                ShizukuResult.Failure(e.message ?: "unknown Shizuku exec error")
            }
        }

        private companion object {
            const val REQUEST_CODE = 1
        }
    }
