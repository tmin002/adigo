package kr.gachon.adigo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.MainActivity
import kr.gachon.adigo.R
import kr.gachon.adigo.data.model.dto.newPushTokenDto

class PushNotificationService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "default_channel"
        private const val TAG = "PushNotificationService"
    }

    // 백그라운드 작업을 위한 코루틴 스코프
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** FCM 알림 수신 */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "알림"
        val body = remoteMessage.notification?.body ?: "내용 없음"
        sendNotification(title, body)

        serviceScope.launch {
            try {
                val container = AdigoApplication.AppContainer
                container.userDatabaseRepo.updateFriendsFromServer()
                Log.d(TAG, "Friend list refreshed from push")
            } catch (e: Exception) {
                Log.e(TAG, "Push-triggered refresh failed", e)
            }
        }
    }


    /** 시스템 알림 표시 */
    private fun sendNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_arrow)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
        manager.notify(0, builder.build())
    }

    /** FCM 토큰이 새로 발급될 때 호출 */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AdigoApplication.AppContainer.tokenManager.saveDeviceToken(token)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // 서비스가 종료될 때 코루틴 스코프 정리
    }
}