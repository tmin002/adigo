package kr.gachon.adigo.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kr.gachon.adigo.AdigoApplication

/**
 * 네트워크가 연결되어 있을 때만 GPS 좌표를 백엔드로 전송하는 포그라운드 서비스입니다.
 * 오프라인 시에는 즉시 위치 업데이트를 중단하므로 배터리 소모를 크게 줄일 수 있습니다.
 */
class UserLocationProviderService : Service() {

    companion object { private const val TAG = "UserLocationProvider" }

    /* ---------- 코루틴 스코프 ---------- */
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /* ---------- 상태 ---------- */
    /** 데이터 네트워크 연결 여부 */
    private val networkState = MutableStateFlow(false)

    /** 현재 GPS 업데이트 중인지 여부 */
    private var tracking = false

    /* ---------- 위치 ---------- */
    private lateinit var fused : FusedLocationProviderClient
    private lateinit var locCb  : LocationCallback

    /* ---------- 네트워크 ---------- */
    private lateinit var cm    : ConnectivityManager
    private lateinit var netCb : ConnectivityManager.NetworkCallback

    /* ---------- 생명주기 ---------- */
    override fun onCreate() {
        super.onCreate()

        startAsForeground()        // 포그라운드 노티 생성 및 시작
        initNetworkMonitoring()    // 네트워크 콜백 등록
        initLocationCallback()     // 위치 콜백 생성
        observeTrackingFlow()      // 연결 상태 변화 감시 → GPS ON/OFF

        ensureWebSocketConnection() // STOMP 재연결 루프
        startFriendLocationRequests() // 친구 위치 주기적 요청
        AdigoApplication.AppContainer.wsReceiver.startListening()
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fused.removeLocationUpdates(locCb)          // GPS 콜백 해제
        cm.unregisterNetworkCallback(netCb)         // 네트워크 콜백 해제
        serviceScope.cancel()                       // 코루틴 취소
        AdigoApplication.AppContainer.wsReceiver.stopListening()
        super.onDestroy()
    }

    /* ---------- 포그라운드 노티 ---------- */
    private fun startAsForeground() {
        val channelId = "location_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_LOW).apply {
                description = "백그라운드 위치 전송"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Adigo 위치 전송 중")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notif)
    }

    /* ---------- 네트워크 모니터링 ---------- */
    private fun initNetworkMonitoring() {
        cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkState.value = cm.activeNetwork != null // 초기값 설정
        netCb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { networkState.value = true }
            override fun onLost(network: Network)     { networkState.value = false }
        }
        cm.registerDefaultNetworkCallback(netCb)
    }

    /* ---------- 위치 콜백 ---------- */
    private fun initLocationCallback() {
        fused = LocationServices.getFusedLocationProviderClient(this)
        locCb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { 
                    sendLocation(it)
                    AdigoApplication.AppContainer.userLocationRepo.updateCurrentLocation(it)
                }
            }
        }
    }

    /* ---------- 연결 상태 변화 감시 ---------- */
    private fun observeTrackingFlow() = serviceScope.launch {
        networkState.collect { online ->
            when {
                online && !tracking -> startLocationUpdates()
                !online && tracking -> stopLocationUpdates()
            }
        }
    }

    /* ---------- GPS ON ---------- */
    private fun startLocationUpdates() {
        if (!hasFineLocation()) return
        val req = LocationRequest.Builder(2_000) // 2초마다
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
        try {
            fused.requestLocationUpdates(req, locCb, Looper.getMainLooper())
            tracking = true
            Log.i(TAG, "GPS ON")
        } catch (se: SecurityException) {
            Log.e(TAG, "requestLocationUpdates()", se)
        }
    }

    /* ---------- GPS OFF ---------- */
    private fun stopLocationUpdates() {
        try { fused.removeLocationUpdates(locCb) } catch (se: SecurityException) {
            Log.e(TAG, "removeLocationUpdates()", se)
        }
        tracking = false
        Log.i(TAG, "GPS OFF")
    }

    /* ---------- 퍼미션 ---------- */
    private fun hasFineLocation() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /* ---------- 위치 전송 ---------- */
    private fun sendLocation(loc: Location) = serviceScope.launch {
        if (!networkState.value) return@launch                           // 오프라인이면 전송하지 않음
        if (!AdigoApplication.AppContainer.stompClient.stompConnected.value) return@launch // WebSocket 미연결 시 스킵
        runCatching {
            AdigoApplication.AppContainer.wsSender.sendMyLocation(loc.latitude, loc.longitude)
        }.onFailure { Log.e(TAG, "sendLocation()", it) }
    }

    /* ---------- WebSocket 연결 유지 ---------- */
    private fun ensureWebSocketConnection() = serviceScope.launch {
        while (isActive) {
            if (!AdigoApplication.AppContainer.stompClient.stompConnected.value) {
                runCatching { AdigoApplication.AppContainer.stompClient.connect() }
            }
            delay(5_000)
        }
    }

    /* ---------- 친구 위치 주기적 요청 ---------- */
    private var friendJob: Job? = null
    private fun startFriendLocationRequests() {
        friendJob?.cancel()
        friendJob = serviceScope.launch {
            while (isActive) {
                if (AdigoApplication.AppContainer.stompClient.stompConnected.value) {
                    AdigoApplication.AppContainer.wsSender.requestFriendLocations()
                }
                delay(5_000)
            }
        }
    }
}