package com.novavpn.app.engine

import android.content.Context
import android.net.VpnService
import android.util.Log

class SingBoxEngine : VpnEngine {
    override val type: EngineType = EngineType.SING_BOX

    companion object {
        private const val TAG = "SingBoxEngine"
    }

    private var initialized = false

    override fun initialize(context: Context): EngineOperationResult {
        return try {
            // This layer intentionally does not claim live connectivity.
            // The sing-box/libbox integration must be evaluated against the final Android build,
            // compatible SDKs, and the licensing constraints before any real tunnel is started.
            initialized = true
            Log.i(TAG, "SingBox engine initialized in abstraction layer")
            EngineOperationResult.Success("sing-box engine abstraction initialized")
        } catch (throwable: Throwable) {
            EngineOperationResult.Failure("Failed to initialize sing-box engine", throwable)
        }
    }

    override fun start(service: VpnService, config: EngineConfig): EngineOperationResult {
        val validation = validateConfig(config)
        if (validation is EngineOperationResult.Failure) {
            return validation
        }

        return if (!initialized) {
            EngineOperationResult.Failure("Sing-box engine is not initialized")
        } else {
            EngineOperationResult.Failure(
                "sing-box/libbox integration is not yet active; real connectivity has not been verified in this stage."
            )
        }
    }

    override fun stop(): EngineOperationResult {
        initialized = false
        return EngineOperationResult.Success("sing-box engine stopped")
    }

    override fun status(): TunnelState {
        return TunnelState(
            connected = false,
            message = "sing-box engine not connected: readiness check only",
            transport = "sing-box"
        )
    }

    override fun validateConfig(config: EngineConfig): EngineOperationResult {
        return when {
            config.engineType != EngineType.SING_BOX -> {
                EngineOperationResult.Failure("Config is not a sing-box engine config")
            }
            config.address.isBlank() -> {
                EngineOperationResult.Failure("Missing server address for sing-box config")
            }
            config.port <= 0 -> {
                EngineOperationResult.Failure("Invalid server port for sing-box config")
            }
            else -> EngineOperationResult.Success("sing-box config validation passed")
        }
    }
}

class WireGuardEngine : VpnEngine {
    override val type: EngineType = EngineType.WIRE_GUARD

    private var initialized = false

    override fun initialize(context: Context): EngineOperationResult {
        initialized = true
        return EngineOperationResult.Success("WireGuard engine abstraction initialized")
    }

    override fun start(service: VpnService, config: EngineConfig): EngineOperationResult {
        val validation = validateConfig(config)
        if (validation is EngineOperationResult.Failure) {
            return validation
        }
        return if (!initialized) {
            EngineOperationResult.Failure("WireGuard engine is not initialized")
        } else {
            EngineOperationResult.Failure("WireGuard implementation is not active in this stage")
        }
    }

    override fun stop(): EngineOperationResult {
        initialized = false
        return EngineOperationResult.Success("WireGuard engine stopped")
    }

    override fun status(): TunnelState {
        return TunnelState(
            connected = false,
            message = "WireGuard engine not connected: readiness check only",
            transport = "wireguard"
        )
    }

    override fun validateConfig(config: EngineConfig): EngineOperationResult {
        return when {
            config.engineType != EngineType.WIRE_GUARD -> {
                EngineOperationResult.Failure("Config is not a WireGuard engine config")
            }
            config.configText.isNullOrBlank() -> {
                EngineOperationResult.Failure("Missing WireGuard configuration payload")
            }
            else -> EngineOperationResult.Success("WireGuard config validation passed")
        }
    }
}

class EngineManager {
    fun createEngine(type: EngineType): VpnEngine {
        return when (type) {
            EngineType.SING_BOX -> SingBoxEngine()
            EngineType.WIRE_GUARD -> WireGuardEngine()
            EngineType.OPEN_VPN -> UnsupportedEngine(type)
            EngineType.WARP -> UnsupportedEngine(type)
            EngineType.SOCKS_PROXY -> UnsupportedEngine(type)
            EngineType.HTTP_PROXY -> UnsupportedEngine(type)
            EngineType.UNKNOWN -> UnsupportedEngine(type)
        }
    }
}

class UnsupportedEngine(
    override val type: EngineType
) : VpnEngine {
    override fun initialize(context: Context): EngineOperationResult {
        return EngineOperationResult.Failure("Engine type $type is not available yet")
    }

    override fun start(service: VpnService, config: EngineConfig): EngineOperationResult {
        return EngineOperationResult.Failure("Engine type $type has not been implemented yet")
    }

    override fun stop(): EngineOperationResult {
        return EngineOperationResult.Success("Unsupported engine safely stopped")
    }

    override fun status(): TunnelState {
        return TunnelState(false, "Unsupported engine", type.name.lowercase())
    }

    override fun validateConfig(config: EngineConfig): EngineOperationResult {
        return EngineOperationResult.Failure("Config validation unavailable for engine type $type")
    }
}
