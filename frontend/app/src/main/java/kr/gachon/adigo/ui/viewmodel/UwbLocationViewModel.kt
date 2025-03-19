package kr.gachon.adigo.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.ViewModel
import kr.gachon.adigo.service.UwbService


class UwbLocationViewModel(private val uwbService: UwbService) : ViewModel() {

    val distance: StateFlow<Float> = uwbService.distance
    val angle: StateFlow<Float> = uwbService.angle
    //var myAddress: StateFlow<Float> = uwbService.myAddress;

    var isController by mutableStateOf(true)
        private set

    fun setControllerState(newState: Boolean) {
        isController = newState
    }



    suspend fun startUwb(address: String, channel: String) {
        uwbService.startUwbRanging(address, channel, isController)
    }

    fun modifyAngle() {
        val randomAngle = Random.nextFloat() * 360f // 0 ~ 360 random angle
        uwbService.modifyAngle(randomAngle)
    }
    fun modifyDistance() {
        val randomDistance = Random.nextFloat() * 100f // 0 ~ 100 random distance
        uwbService.modifyDistance(randomDistance)
    }
}
