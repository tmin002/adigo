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

    // Expose ranging status
    val isRangingActive: StateFlow<Boolean> = uwbService.isRangingActiveFlow

    var isController by mutableStateOf(true)
        private set

    companion object {
        const val DEFAULT_PEER_ADDRESS = 1234
        const val DEFAULT_CONFIG_CHANNEL = 9
        const val DEFAULT_CONFIG_PREAMBLE = 11
    }

    init {
        setControllerState(isController)
    }

    fun startUwb(peerAddressStr: String, configChannelStr: String, configPreambleStr: String) {
        val peerAddr = peerAddressStr.toIntOrNull() ?: DEFAULT_PEER_ADDRESS
        val configChan = configChannelStr.toIntOrNull() ?: DEFAULT_CONFIG_CHANNEL
        val configPreamble = configPreambleStr.toIntOrNull() ?: DEFAULT_CONFIG_PREAMBLE

        viewModelScope.launch(Dispatchers.IO) {
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
            stopUwb()

            viewModelScope.launch {
                suspendCancellableCoroutine<Unit> { continuation ->
                    uwbService.setRoleAsync(newControllerState) {
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                    continuation.invokeOnCancellation {
                        // Handle cancellation
                    }
                }
            }
        }
    }
}