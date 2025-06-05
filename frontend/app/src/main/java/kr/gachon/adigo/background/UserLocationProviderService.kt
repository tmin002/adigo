package kr.gachon.adigo.background

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.ActivityRecognition
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.receiver.MotionBroadcastReceiver

class UserLocationProviderService : Service() {

    companion object { private const val TAG = "UserLocationProvider" }

    /* ---------- 상태 변수 ---------- */
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val motionState      = MutableStateFlow(false)  // 움직임 여부
    private val networkState     = MutableStateFlow(true)   // 데이터망 연결 여부
    private var requestingLocation = false

    /* ---------- 위치 ---------- */
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var locCallback: LocationCallback

    /* ---------- 네트워크 ---------- */
    private lateinit var cm: ConnectivityManager
    private lateinit var netCallback: ConnectivityManager.NetworkCallback

    /* ---------- 생명주기 ---------- */
    override fun onCreate() {
        super.onCreate()

        startForegroundService()

        initNetworkMonitoring()
        initMotionDetection()
        initLocationCallback()
        observeStates()     // 두 StateFlow를 합쳐서 GPS on/off 제어
        ensureWebSocketConnection()
        startFriendLocationRequests()
        AdigoApplication.AppContainer.wsReceiver.startListening()
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int =
        START_STICKY

    override fun onDestroy() {
        // 정리
        if (::locCallback.isInitialized) fused.removeLocationUpdates(locCallback)
        if (::netCallback.isInitialized) cm.unregisterNetworkCallback(netCallback)
        try {
            if (hasActivityRecognition()) {
                activityClient.removeActivityUpdates(activityPi)
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "removeActivityUpdates SecurityException", se)
        }
        serviceScope.cancel()
        AdigoApplication.AppContainer.wsReceiver.stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    /* ---------- Foreground Notification ---------- */
    private fun startForegroundService() {
        val chId = "location_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(chId, "Location Service",
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "백그라운드 위치 전송"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val notif = NotificationCompat.Builder(this, chId)
            .setContentTitle("Adigo 위치 전송 중")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notif)
    }

    /* ---------- Activity Recognition ---------- */
    private lateinit var activityClient: ActivityRecognitionClient
    private lateinit var activityPi: PendingIntent

    @SuppressLint("MissingPermission")  // Permission check는 별도 수행
    private fun initMotionDetection() {
        activityClient = ActivityRecognition.getClient(this)

        if (!hasActivityRecognition()) {
            Log.w(TAG, "ACTIVITY_RECOGNITION permission not granted; motion detection disabled.")
            return
        }

        val intent = Intent(this, MotionBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        activityPi = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or flags
        )

        try {
            activityClient.requestActivityUpdates(3_000, activityPi)
                .addOnFailureListener { Log.e(TAG, "AR request 실패", it) }
        } catch (se: SecurityException) {
            Log.e(TAG, "Missing ACTIVITY_RECOGNITION permission", se)
        }
    }

    /* ---------- Network Monitoring ---------- */
    private fun initNetworkMonitoring() {
        cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkState.value = cm.activeNetwork != null
        netCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network)  { networkState.value = true }
            override fun onLost(network: Network)      { networkState.value = false }
        }
        cm.registerDefaultNetworkCallback(netCallback)
    }

    /* ---------- GPS 초기화 (콜백만) ---------- */
    private fun initLocationCallback() {
        fused = LocationServices.getFusedLocationProviderClient(this)
        locCallback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                res.lastLocation?.let { sendLocationUpdate(it) }
            }
        }
    }

    @SuppressLint("MissingPermission")  // we do an explicit check below
    private fun startLocationUpdates() {
        if (!hasFineLocation()) {
            Log.w(TAG, "ACCESS_FINE_LOCATION not granted; GPS updates skipped.")
            return
        }
        try {
            val req = LocationRequest.Builder(2000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()
            fused.requestLocationUpdates(req, locCallback, Looper.getMainLooper())
        } catch (se: SecurityException) {
            Log.e(TAG, "requestLocationUpdates SecurityException", se)
        }
    }

    private fun stopLocationUpdates() {
        try {
            fused.removeLocationUpdates(locCallback)
        } catch (se: SecurityException) {
            Log.e(TAG, "removeLocationUpdates SecurityException", se)
        }
        Log.i(TAG, "GPS OFF")
    }

    /* ---------- permissions ---------- */
    private fun hasFineLocation() =
        ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun hasActivityRecognition(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

    /* ---------- 상태 변화 감시해서 GPS on/off ---------- */
    private fun observeStates() = serviceScope.launch {
        combine(motionState, networkState) { moving, net -> moving && net }
            .collect { shouldRequest ->
                if (shouldRequest && !requestingLocation) {
                    startLocationUpdates(); requestingLocation = true
                } else if (!shouldRequest && requestingLocation) {
                    stopLocationUpdates();  requestingLocation = false
                }
            }
    }

    /* ---------- 위치 전송 ---------- */
    private fun sendLocationUpdate(loc: Location) = serviceScope.launch {
        try {
            if (networkState.value &&
                AdigoApplication.AppContainer.stompClient.stompConnected) {
                AdigoApplication.AppContainer.wsSender
                    .sendMyLocation(loc.latitude, loc.longitude)
            }
        } catch (e: Exception) { Log.e(TAG, "sendLocationUpdate error", e) }
    }

    /* ---------- WebSocket / 친구 위치 ---------- */
    private fun ensureWebSocketConnection() = serviceScope.launch {
        while (isActive) {
            try {
                if (!AdigoApplication.AppContainer.stompClient.stompConnected) {
                    AdigoApplication.AppContainer.stompClient.connect()
                }
            } catch (_: Exception) { /* retry */ }
            delay(5_000)
        }
    }

    private var friendJob: Job? = null
    private fun startFriendLocationRequests() {
        friendJob?.cancel()
        friendJob = serviceScope.launch {
            while (isActive) {
                if (AdigoApplication.AppContainer.stompClient.stompConnected) {
                    AdigoApplication.AppContainer.wsSender.requestFriendLocations()
                }
                delay(5_000)
            }
        }
    }
}