package com.namirai.nikai.data.remote

enum class BackendConnectionMode {
    SameDevice,
    HotspotOrLocalClient,
    Offline,
}

internal sealed interface BackendResolution<out T> {
    data class SameDevice<T>(
        val endpoint: T,
        val proof: SameDeviceSettingsProof,
    ) : BackendResolution<T>

    data class Gateway<T>(
        val endpoint: T,
    ) : BackendResolution<T>

    data object PermissionRequired : BackendResolution<Nothing>

    data object Offline : BackendResolution<Nothing>
}

internal class BackendResolutionOrder<T>(
    private val sameDeviceResolution: suspend () -> BackendResolution.SameDevice<T>?,
    private val gatewayResolver: suspend () -> T?,
) {
    suspend fun resolve(gatewayAllowed: Boolean): BackendResolution<T> {
        sameDeviceResolution()?.let {
            return it
        }
        if (!gatewayAllowed) {
            return BackendResolution.PermissionRequired
        }
        return gatewayResolver()
            ?.let { BackendResolution.Gateway(it) }
            ?: BackendResolution.Offline
    }
}
