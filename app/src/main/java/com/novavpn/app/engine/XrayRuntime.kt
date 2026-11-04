package com.novavpn.app.engine

import android.content.Context
import android.provider.Settings
import android.util.Log
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

/**
 * Owns the embedded Xray core lifecycle. A successful start means the native
 * core accepted and started the runtime configuration, not merely that Android
 * created a VPN TUN interface.
 */
object XrayRuntime {
    private const val TAG = "NovaXrayRuntime"
    @Volatile private var controller: CoreController? = null
    @Volatile private var initialized = false

    @Synchronized
    fun start(context: Context, rawConfig: String, tunFd: Int) {
        require(tunFd > 0) { "Android VPN interface is not ready." }
        val appContext = context.applicationContext
        if (!initialized) {
            Seq.setContext(appContext)
            val deviceId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
            Libv2ray.initCoreEnv(appContext.filesDir.absolutePath, deviceId)
            initialized = true
        }

        val runtimeConfig = XrayConfigBuilder.build(rawConfig)
        val core = controller ?: Libv2ray.newCoreController(object : CoreCallbackHandler {
            override fun startup(): Long {
                Log.i(TAG, "Xray core startup callback")
                return 0
            }

            override fun shutdown(): Long {
                Log.i(TAG, "Xray core shutdown callback")
                return 0
            }

            override fun onEmitStatus(code: Long, message: String?): Long {
                Log.i(TAG, "Xray status $code: $message")
                return 0
            }
        }).also { controller = it }

        check(!core.isRunning) { "Xray core is already running." }
        core.startLoop(runtimeConfig, tunFd)
        check(core.isRunning) { "Xray core did not enter the running state." }
    }

    @Synchronized
    fun stop() {
        val core = controller ?: return
        if (core.isRunning) {
            core.stopLoop()
        }
    }

    fun isRunning(): Boolean = controller?.isRunning == true
}
