package kr.gachon.adigo.background

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.data.remote.websocket.UserLocationWebSocketReceiver

class UserLocationProviderService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val intervalMillis = 5000L
    private var friendLocationRequestJob: Job? = null
    private var locationUpdateJob: Job? = null
    private var lastLocation: Location? = null
    private lateinit var locationReceiver: UserLocationWebSocketReceiver

    override fun onCreate() {
        super.onCreate()

        if (!hasAllPermissions()) {
            Log.w("UserLocationProvider", "Required permissions not granted. Service will not work properly in background.")
            // 권한이 없어도 서비스는 계속 실행하되, 로그를 남깁니다.
        }

        Log.i("UserLocationProvider", "Foreground Service started")
        startForegroundService()
        initLocationUpdates()
        startFriendLocationRequests()
        ensureWebSocketConnection()
        locationReceiver = AdigoApplication.AppContainer.wsReceiver
        locationReceiver.startListening()
    }

    private fun hasAllPermissions(): Boolean {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 9 이하에서는 백그라운드 권한이 필요 없음
        }

        val hasForegroundService = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return hasFineLocation && hasBackgroundLocation && hasForegroundService
    }

    private fun startForegroundService() {
        val channelId = "location_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "백그라운드에서 위치 정보를 전송합니다."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Adigo 위치 전송 중")
            .setContentText("앱이 종료되어도 위치가 전송됩니다.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun initLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    lastLocation = location
                    Log.i("UserLocationProvider", "Location updated: ${location.latitude}, ${location.longitude}")
                    sendLocationUpdate(location)
                }
            }
        }

        try {
            val locationRequest = LocationRequest.Builder(intervalMillis)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(0f)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("UserLocationProvider", "위치 권한이 필요합니다.", e)
        }
    }

    private fun sendLocationUpdate(location: Location) {
        serviceScope.launch {
            try {
                if (AdigoApplication.AppContainer.stompClient.stompConnected) {
                    AdigoApplication.AppContainer.wsSender.sendMyLocation(
                        location.latitude,
                        location.longitude
                    )
                } else {
                    Log.w("UserLocationProvider", "WebSocket not connected, cannot send location update")
                }
            } catch (e: Exception) {
                Log.e("UserLocationProvider", "Failed to send location update", e)
            }
        }
    }

    private fun ensureWebSocketConnection() {
        serviceScope.launch {
            while (isActive) {
                try {
                    if (!AdigoApplication.AppContainer.stompClient.stompConnected) {
                        Log.i("UserLocationProvider", "WebSocket disconnected, attempting to reconnect...")
                        AdigoApplication.AppContainer.stompClient.connect()
                        delay(2000) // Wait for connection attempt
                    }
                    delay(5000) // Check connection status every 5 seconds
                } catch (e: Exception) {
                    Log.e("UserLocationProvider", "Error in WebSocket connection check", e)
                    delay(5000)
                }
            }
        }
    }

    private fun startFriendLocationRequests() {
        friendLocationRequestJob?.cancel()
        friendLocationRequestJob = serviceScope.launch {
            while (isActive) {
                try {
                    if (AdigoApplication.AppContainer.stompClient.stompConnected) {
                        Log.i("UserLocationProvider", "Requesting friend locations...")
                        AdigoApplication.AppContainer.wsSender.requestFriendLocations()
                    } else {
                        Log.w("UserLocationProvider", "WebSocket not connected, skipping friend location request")
                    }
                } catch (e: Exception) {
                    Log.e("UserLocationProvider", "Error requesting friend locations", e)
                }
                delay(intervalMillis)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        friendLocationRequestJob?.cancel()
        locationUpdateJob?.cancel()
        serviceScope.cancel()
        locationReceiver.stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}