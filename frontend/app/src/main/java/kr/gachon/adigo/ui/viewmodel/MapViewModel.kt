package kr.gachon.adigo.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.gachon.adigo.data.local.repository.UserLocationRepository
import kr.gachon.adigo.data.remote.websocket.StompWebSocketClient
import kr.gachon.adigo.data.remote.websocket.UserLocationWebSocketReceiver
import kr.gachon.adigo.data.remote.websocket.UserLocationWebSocketSender
import kr.gachon.adigo.data.model.global.UserLocationDto

class MapViewModel(
    application: Application,
    private val userLocationRepo: UserLocationRepository,
    private val stompClient: StompWebSocketClient,
    private val locationSender: UserLocationWebSocketSender,
    private val locationReceiver: UserLocationWebSocketReceiver
) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext

    // 위치 권한 상태
    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    // 내 위치 상태
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    // 내 위치 추적 상태
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // 친구 위치 목록 (StateFlow로 변환)
    private val _friends = MutableStateFlow<List<UserLocationDto>>(emptyList())
    val friends: StateFlow<List<UserLocationDto>> = _friends.asStateFlow()

    // 위치 서비스
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    init {
        checkLocationPermission()
        // Flow를 StateFlow로 변환
        viewModelScope.launch {
            userLocationRepo.friends.collect { list ->
                _friends.value = list
            }
        }
    }

    fun checkLocationPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        _hasLocationPermission.value = granted
    }

    fun requestLocationUpdates() {
        if (!_hasLocationPermission.value) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                _currentLocation.value = LatLng(loc.latitude, loc.longitude)
            }
        }
        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            null
        )
    }

    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    fun setTracking(enabled: Boolean) {
        _isTracking.value = enabled
    }

    // WebSocket 연결/구독/전송
    fun connectWebSocket() {
        stompClient.connect()
        locationReceiver.startListening()
    }
    fun disconnectWebSocket() {
        locationReceiver.stopListening()
        stompClient.disconnect()
    }
    fun sendMyLocation() {
        val loc = _currentLocation.value ?: return
        locationSender.sendMyLocation(loc.latitude, loc.longitude)
    }
    fun requestFriendLocations() {
        locationSender.requestFriendLocations()
    }
} 