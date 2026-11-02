package com.novavpn.app.engine

import android.content.Context
import android.net.VpnService

class EngineBootstrapper(
    private val context: Context
) {
    private val engineManager = EngineManager()

    fun initDefaultEngines(): List<VpnEngine> {
        val engines = mutableListOf<VpnEngine>()
        engines += engineManager.createEngine(EngineType.SING_BOX)
        engines += engineManager.createEngine(EngineType.WIRE_GUARD)
        return engines.onEach { engine ->
            val result = engine.initialize(context)
            if (result is EngineOperationResult.Failure) {
                android.util.Log.w("EngineBootstrapper", result.message)
            }
        }
    }

    fun startEngine(type: EngineType, config: EngineConfig, service: VpnService): EngineOperationResult {
        val engine = engineManager.createEngine(type)
        return engine.start(service, config)
    }
}
