package com.novavpn.app.engine

import android.content.Context
import android.net.VpnService

enum class EngineType {
    SING_BOX,
    WIRE_GUARD,
    OPEN_VPN,
    WARP,
    SOCKS_PROXY,
    HTTP_PROXY,
    UNKNOWN
}

data class EngineConfig(
    val id: String,
    val name: String,
    val engineType: EngineType,
    val protocol: String,
    val address: String,
    val port: Int,
    val userName: String? = null,
    val password: String? = null,
    val configText: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

sealed class EngineOperationResult {
    data class Success(val message: String = "OK") : EngineOperationResult()
    data class Failure(val message: String, val cause: Throwable? = null) : EngineOperationResult()
}

data class TunnelState(
    val connected: Boolean,
    val message: String,
    val transport: String = "unknown"
)

interface VpnEngine {
    val type: EngineType
    fun initialize(context: Context): EngineOperationResult
    fun start(service: VpnService, config: EngineConfig): EngineOperationResult
    fun stop(): EngineOperationResult
    fun status(): TunnelState
    fun validateConfig(config: EngineConfig): EngineOperationResult
}
