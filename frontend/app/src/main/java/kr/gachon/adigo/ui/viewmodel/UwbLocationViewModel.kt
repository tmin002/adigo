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

class UwbLocationViewModel(
    private val uwbService: uwbService
) : ViewModel() {

    val distance: StateFlow<Float> = uwbService.distance
    val angle: StateFlow<Float> = uwbService.angle

    var isController by mutableStateOf(true)
        private set


    fun startUwb(address: String = "1234", channel: String = "5") {
        val addr = address.toIntOrNull() ?: 1234
        val chan = channel.toIntOrNull() ?: 5
        if (isController) {
            uwbService.startRanging(addr,11)
        } else {
            uwbService.startRanging(addr, chan)
        }
    }

    fun stopUwb() {
        uwbService.stopRanging()
    }

    fun setControllerState(isController: Boolean) {
        this.isController = isController

        viewModelScope.launch {
            suspendCancellableCoroutine { continuation ->
                uwbService.setRoleAsync(isController) {
                    continuation.resume(Unit, null)
                }
            }
        }
    }




}
