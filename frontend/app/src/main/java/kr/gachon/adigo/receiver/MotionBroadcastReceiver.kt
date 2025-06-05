package kr.gachon.adigo.receiver;


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import kr.gachon.adigo.background.UserLocationProviderService

class MotionBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ActivityRecognitionResult.extractResult(intent)?.let { res ->
                val most = res.mostProbableActivity
            val moving = when (most.type) {
                DetectedActivity.WALKING,
                        DetectedActivity.RUNNING,
                        DetectedActivity.ON_BICYCLE,
                        DetectedActivity.IN_VEHICLE -> most.confidence >= 50
                else -> false
            }
            (context as? UserLocationProviderService)?.motionState?.value = moving
            Log.d("MotionReceiver", "moving=$moving (${most.type}, conf=${most.confidence})")
        }
    }
}