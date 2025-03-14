package kr.gachon.adigo.service

import android.content.Context
import androidx.core.uwb.*
import androidx.core.uwb.rxjava3.controleeSessionScopeSingle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicReference

class UwbService(private val context: Context) {


    private val uwbManager: UwbManager = UwbManager.createInstance(context)
    private val currentSessionScope = AtomicReference<UwbClientSessionScope?>(null)

    private val _distance = MutableStateFlow(1f)
    val distance: StateFlow<Float> = _distance

    private val _angle = MutableStateFlow(0f)
    val angle: StateFlow<Float> = _angle

    var isControllerSwitch = true;

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    suspend fun startUwbRanging(address: String, channel: Int) {
        withContext(Dispatchers.IO) {

            val partnerAddress = UwbAddress(address.toByteArray()) // 주소 변환
            val uwbComplexChannel = UwbComplexChannel(channel, 9)


        }

    }

    fun modifyAngle(angle: Float) {
        _angle.value = angle
    }

    fun modifyDistance(distance: Float) {
        _distance.value = distance
    }
}
