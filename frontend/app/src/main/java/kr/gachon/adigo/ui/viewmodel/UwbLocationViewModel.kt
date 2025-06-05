package kr.gachon.adigo.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kr.gachon.adigo.service.uwbService
import kotlin.coroutines.resume // Import resume

class UwbLocationViewModel(
    private val uwbService: uwbService
) : ViewModel() {

    val distance: StateFlow<Float> = uwbService.distance
    val angle: StateFlow<Float> = uwbService.angle

    // New flows for local UWB info
    val localUwbAddress: StateFlow<String> = uwbService.localUwbAddressFlow
    val localUwbChannel: StateFlow<String> = uwbService.localUwbChannelFlow
    val localUwbPreambleIndex: StateFlow<String> = uwbService.localUwbPreambleIndexFlow

    var isController by mutableStateOf(true)
        private set

    init {
        // Set the initial role when ViewModel is created
        // This will also populate the local UWB info flows
        setControllerState(isController) // isController is true by default
    }

    fun startUwb(address: String = "1234", channel: String = "5") { // channel here is used as Preamble for Controlee
        val addr = address.toIntOrNull() ?: 1234
        val preambleForControlee = channel.toIntOrNull() ?: 11 // Default preamble for controlee if parsing fails

        viewModelScope.launch { // Ensure service calls are off the main thread if they are blocking
            if (isController) {
                // Controller uses its own preamble (e.g., 11 as default, or whatever uwbService.setRole sets)
                // The second parameter to startRanging is the preambleIndex for the *peer* if it's a Controlee.
                // For a Controller starting ranging, this preambleIndex parameter in startRanging
                // is used to configure the Controlee's expected preamble.
                // Let's assume the controller uses a fixed preamble or one derived internally.
                // The '11' here might be what the *CONTROLEE* should be using.
                // If the Controller itself determines its preamble, it might not need it here.
                // For now, let's assume '11' is a sensible default for the peer (controlee) preamble.
                uwbService.startRanging(addr, 11) // This '11' would be the preamble index for the controlee device it's trying to range with.
                // The controller itself will use its pre-configured channel/preamble.
            } else {
                // Controlee uses channel 9, and the preamble 'chan' (preambleForControlee)
                uwbService.startRanging(addr, preambleForControlee)
            }
        }
    }

    fun stopUwb() {
        viewModelScope.launch {
            uwbService.stopRanging()
        }
    }

    fun setControllerState(isNewControllerState: Boolean) {
        // Only update and re-initialize if the state actually changes
        if (this.isController != isNewControllerState || localUwbAddress.value == "N/A") {
            this.isController = isNewControllerState
            stopUwb() // Stop any previous ranging before changing role

            viewModelScope.launch {
                suspendCancellableCoroutine<Unit> { continuation ->
                    uwbService.setRoleAsync(isNewControllerState) {
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                    continuation.invokeOnCancellation {
                        // Handle cancellation if needed
                    }
                }
                // Optionally, auto-start ranging after role change if a target is already defined
                // For now, user has to press "확인" again.
            }
        }
    }
}