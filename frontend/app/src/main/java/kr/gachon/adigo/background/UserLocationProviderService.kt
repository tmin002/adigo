package kr.gachon.adigo.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.gachon.adigo.AdigoApplication

class UserLocationProviderService : Service() {

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val intervalMillis = 5000L

    override fun onCreate() {
        super.onCreate()
        Log.i("UserLocationProvider", "Foreground Service started")
        startForegroundService()
        initLocationUpdates()
    }

    private fun startForegroundService() {
        val channelId = "location_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Adigo 위치 전송 중")
            .setContentText("앱이 종료되어도 위치가 전송됩니다.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)
    }

    private fun initLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.i("UserLocationProvider", "Sending location: ${location.latitude}, ${location.longitude}")
                AdigoApplication.userLocationWebSocketSender.sendMyLocation(
                    location.latitude,
                    location.longitude
                )
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                intervalMillis,
                0f,
                locationListener
            )
        } catch (e: SecurityException) {
            Log.e("UserLocationProvider", "위치 권한이 필요합니다.", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        locationManager.removeUpdates(locationListener)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}