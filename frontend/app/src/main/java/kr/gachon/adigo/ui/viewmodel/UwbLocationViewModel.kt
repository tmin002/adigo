package kr.gachon.adigo.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kr.gachon.adigo.service.uwbService
import kotlin.coroutines.resume

class UwbLocationViewModel(
    private val uwbService: uwbService
) : ViewModel() {

    val distance: StateFlow<Float> = uwbService.distance
    val angle: StateFlow<Float> = uwbService.angle

    val localUwbAddress: StateFlow<String> = uwbService.localUwbAddressFlow
    val localUwbChannel: StateFlow<String> = uwbService.localUwbChannelFlow
    val localUwbPreambleIndex: StateFlow<String> = uwbService.localUwbPreambleIndexFlow

    var isController by mutableStateOf(true)
        private set

    // Default values for UWB parameters
    companion object {
        const val DEFAULT_PEER_ADDRESS = 1234
        const val DEFAULT_CONFIG_CHANNEL = 9 // Common UWB channel
        const val DEFAULT_CONFIG_PREAMBLE = 11 // Common UWB preamble index
    }

    init {
        setControllerState(isController) // Initialize with default role
    }

    // peerAddressStr: Address of the other device.
    // configChannelStr: If Controller, expected peer channel. If Controlee, own listening channel.
    // configPreambleStr: If Controller, expected peer preamble. If Controlee, own listening preamble.
    fun startUwb(peerAddressStr: String, configChannelStr: String, configPreambleStr: String) {
        val peerAddr = peerAddressStr.toIntOrNull() ?: DEFAULT_PEER_ADDRESS
        val configChan = configChannelStr.toIntOrNull() ?: DEFAULT_CONFIG_CHANNEL
        val configPreamble = configPreambleStr.toIntOrNull() ?: DEFAULT_CONFIG_PREAMBLE

        viewModelScope.launch(Dispatchers.IO) { // Perform service calls on IO dispatcher
            uwbService.startRanging(peerAddr, configChan, configPreamble)
        }
    }

    fun stopUwb() {
        viewModelScope.launch(Dispatchers.IO) {
            uwbService.stopRanging()
        }
    }

    fun setControllerState(newControllerState: Boolean) {
        if (this.isController != newControllerState || localUwbAddress.value == "N/A" || localUwbAddress.value == "Error") {
            this.isController = newControllerState
            stopUwb() // Stop any previous ranging

            viewModelScope.launch { // setRoleAsync handles its own thread
                suspendCancellableCoroutine<Unit> { continuation ->
                    uwbService.setRoleAsync(newControllerState) {
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                    continuation.invokeOnCancellation {
                        // Handle cancellation if needed, e.g., log
                    }
                }
            }
        }
    }
}