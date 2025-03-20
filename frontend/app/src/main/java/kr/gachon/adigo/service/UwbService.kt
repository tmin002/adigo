package kr.gachon.adigo.service

import android.content.Context
import androidx.core.uwb.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import android.util.Log;
import com.google.common.primitives.Shorts

class UwbService(private val context: Context) {


    private val uwbManager: UwbManager = UwbManager.createInstance(context)
    private val currentSessionScope = AtomicReference<UwbClientSessionScope?>(null)

    private val _distance = MutableStateFlow(1f)
    val distance: StateFlow<Float> = _distance

    private val _angle = MutableStateFlow(0f)
    val angle: StateFlow<Float> = _angle


    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    suspend fun startUwbRanging(address: String, channel: String, isController: Boolean) {

        Log.d("tag", "startUwbRanging() called with address=$address, channel=$channel")

        // Convert address string to UwbAddress
        val addressBytes = ByteBuffer.allocate(2).putShort(address.toShort()).array()
        val partnerAddress = UwbAddress(addressBytes)

        // Create UwbComplexChannel with provided channel and a default preamble index
        val uwbComplexChannel = UwbComplexChannel(Integer.parseInt(channel), 9)

        val my8ByteSessionKey = ByteArray(8) { 0x00 }


        withContext(Dispatchers.IO) {
            try {
                Log.d("tag", "Before creating sessionScope")
                // Initialize the appropriate session scope
                val sessionScope = if (isController) {
                    uwbManager.controllerSessionScope()

                } else {
                    uwbManager.controleeSessionScope()
                }

                currentSessionScope.set(sessionScope)
                Log.d("tag", "Created sessionScope: $sessionScope")


                val partnerParameters = RangingParameters(
                    uwbConfigType = RangingParameters.CONFIG_UNICAST_DS_TWR,
                    sessionId = 12345,
                    subSessionId = 0,
                    sessionKeyInfo = my8ByteSessionKey,
                    subSessionKeyInfo = null,
                    complexChannel = uwbComplexChannel,
                    peerDevices = listOf(UwbDevice(partnerAddress)),
                    updateRateType = RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC
                )
                Log.d("tag", "Before prepareSession(...) call")
                // Prepare the ranging session
                val sessionFlow = sessionScope.prepareSession(partnerParameters)

                Log.d("tag", "sessionFlow acquired: $sessionFlow")

                // Collect ranging results
                coroutineScope.launch {
                    Log.d("tag","my address:"+ Shorts.fromByteArray(sessionScope.localAddress.address))

                    sessionFlow.collect { rangingResult ->
                        when (rangingResult) {
                            is RangingResult.RangingResultPosition -> {
                                rangingResult.position.distance?.let {
                                    _distance.value = it.value
                                }
                                rangingResult.position.azimuth?.let {
                                    _angle.value = it.value
                                }
                            }
                            is RangingResult.RangingResultPeerDisconnected -> {
                                // Handle peer disconnection
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("tag", "Exception in startUwbRanging: ${e.message}", e)
            }
        }
    }

    fun modifyAngle(angle: Float) {
        _angle.value = angle
    }

    fun modifyDistance(distance: Float) {
        _distance.value = distance
    }
}
