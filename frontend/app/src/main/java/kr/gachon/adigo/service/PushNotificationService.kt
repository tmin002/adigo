package kr.gachon.adigo.service;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import kr.gachon.adigo.AdigoApplication;
import kr.gachon.adigo.MainActivity;
import kr.gachon.adigo.R;

public class PushNotificationService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "default_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // 푸시 알림에서 데이터(payload)나 메시지 내용을 확인
        String title = remoteMessage.getNotification() != null ?
                remoteMessage.getNotification().getTitle() : "알림";
        String body = remoteMessage.getNotification() != null ?
                remoteMessage.getNotification().getBody() : "내용 없음";

        // 알림을 표시하는 메서드 호출
        sendNotification(title, body);
    }

    private void sendNotification(String title, String messageBody) {
        // 알림 클릭 시 열릴 인텐트 구성 (예, MainActivity)
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        // NotificationCompat 빌더로 알림 생성
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_arrow) // 앱 내 알림 아이콘
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        // NotificationManager로 알림 발송
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Android Oreo 이상에서는 채널 생성 필요
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);

        notificationManager.notify(0, notificationBuilder.build());
    }

    // 토큰 갱신 시 호출되는 메서드
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // 갱신된 토큰을 서버로 전송하거나 로컬 저장
        // TokenManager 가져오기
        var tokenManager = AdigoApplication.Companion.getTokenManager();
        tokenManager.saveDeviceToken(token);

    }
}

