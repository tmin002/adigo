package kr.gachon.adigo.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.BottomSheetValue.Collapsed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import kr.gachon.adigo.ui.viewmodel.AuthViewModel
import kr.gachon.adigo.ui.viewmodel.FriendLocationViewModel
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.gachon.adigo.data.model.global.UserLocation
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.coroutineContext
import com.google.android.gms.location.LocationServices
import android.location.Location
import android.net.Uri
import android.os.Build
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import android.os.Looper
import androidx.compose.foundation.gestures.awaitFirstDown
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.ui.viewmodel.FriendListViewModel
import android.util.Log
import kr.gachon.adigo.background.UserLocationProviderService
import java.net.URLEncoder
import java.util.Locale
import com.google.maps.android.compose.CameraMoveStartedReason

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MapScreen(authViewModel: AuthViewModel, navController: NavController) {
    // 1. Declare state variables at the top
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var isTracking by remember { mutableStateOf(false) }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(initialValue = Collapsed)
    )
    val scope = rememberCoroutineScope()

    // Get WebSocket components from Application
    val stompClient = remember { AdigoApplication.AppContainer.stompClient }
    val locationReceiver = remember { AdigoApplication.AppContainer.wsReceiver }
    val locationSender = remember { AdigoApplication.AppContainer.wsSender }

    val friendLocationViewModel = FriendLocationViewModel(AdigoApplication.AppContainer.userLocationRepo)
    val friends by friendLocationViewModel.friends.collectAsState()

    var selectedContent by remember { mutableStateOf(BottomSheetContentType.FRIENDS) }
    var friendScreenState by remember { mutableStateOf<FriendScreenState>(FriendScreenState.List) }

    // 지도 카메라 위치 예시 (서울)
    val seoul = LatLng(37.56, 126.97)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(seoul, 10f)
    }

    //위치권한
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            // If permission is granted after requesting, try to get last location and start tracking
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedLocationClient.lastLocation
            location.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                    isTracking = true // Start tracking once permission is granted and location is available
                }
            }
        }
    }

    // 앱 시작 시 권한 요청 및 WebSocket 연결/구독/요청
    LaunchedEffect(Unit) {
        // Request Location Permission if not granted
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Send Device Token (already in MainActivity, but maybe ensure here too)
        // authViewModel.sendDeviceToken() // Consider where this is best placed - once per app install/login is typical

        // Connect WebSocket
        Log.d("MapScreen", "Connecting WebSocket client")
        stompClient.connect()

        // Start receiving friend locations
        Log.d("MapScreen", "Starting location receiver listening")
        locationReceiver.startListening()

        // Request initial friend locations after a delay to allow connection
        // Start UserLocationProviderService
        val serviceIntent = Intent(context, kr.gachon.adigo.background.UserLocationProviderService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
        Log.i("MapScreen", "UserLocationProviderService started from MapScreen")
        scope.launch {
            delay(2000) // Give WebSocket a moment to connect
            if (stompClient.stompConnected) { // Check if STOMP is connected
                Log.d("MapScreen", "Requesting initial friend locations")
                locationSender.requestFriendLocations()
            } else {
                Log.w("MapScreen", "STOMP not connected, skipping initial friend location request")
            }
        }


    }

    // Clean up WebSocket connection on dispose
    DisposableEffect(Unit) {
        onDispose {
            Log.d("MapScreen", "MapScreen disposed. Stopping location receiver and disconnecting WebSocket client.")
            locationReceiver.stopListening()
            stompClient.disconnect()
        }
    }

    // 지도가 움직이면 내 위치 추적 해제
    LaunchedEffect(cameraPositionState.position) {
        if (isTracking) {
            isTracking = false
            Log.d("MapScreen", "Camera moved after user interaction. Tracking disabled.")
        }
    }

    //만약 위치 권한이 있으면 마지막 위치 받아오기 (Initial location)
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            // Use GlobalScope or applicationScope if this needs to survive beyond MapScreen lifecycle
            // But typically, you only need last known location when the map screen is active.
            // For this case, LaunchedEffect tied to MapScreen is fine.
            try {
                val location = fusedLocationClient.lastLocation
                location.addOnSuccessListener { loc: Location? ->
                    loc?.let {
                        currentLocation = LatLng(it.latitude, it.longitude)
                        // Option: Start tracking automatically when location is found and permission is granted
                        // isTracking = true
                        Log.d("MapScreen", "Got last known location: ${it.latitude}, ${it.longitude}")
                    } ?: Log.w("MapScreen", "Last known location is null.")
                }
                    .addOnFailureListener { e ->
                        Log.e("MapScreen", "Failed to get last known location", e)
                    }
            } catch (e: SecurityException) {
                // This should not happen if hasLocationPermission is true, but good practice to catch
                Log.e("MapScreen", "Security exception getting last location", e)
            }
        }
    }

    //위치 업데이트 요청 Callback
    val fusedLocationClient = remember{ LocationServices.getFusedLocationProviderClient(context)}
    val locationRequest = remember{
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L) // Update every 5 seconds
            .setMinUpdateIntervalMillis(1000L) // No faster than every 1 second
            .build()
    }

    // 실시간 위치 업데이트 리스너
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val loc = locationResult.lastLocation ?: return
                val newLatLng = LatLng(loc.latitude, loc.longitude)
                currentLocation = newLatLng
                Log.v("MapScreen", "Location updated: ${newLatLng.latitude}, ${newLatLng.longitude}")

                // My location tracking: move camera if enabled
                if (isTracking) {
                    scope.launch {
                        try {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(newLatLng, 18f) // Adjust zoom as needed
                            )
                            Log.d("MapScreen", "Camera animated to current location.")
                        } catch (e: Exception) {
                            Log.e("MapScreen", "Error animating camera to current location", e)
                        }
                    }
                }
            }
        }
    }

    // Start/Stop Location Updates based on hasLocationPermission
    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper() // Use main looper for callbacks
                )
                Log.d("MapScreen", "Location updates requested.")
            } catch (e: SecurityException) {
                Log.e("MapScreen", "Security exception requesting location updates", e)
                // Handle gracefully - maybe set hasLocationPermission to false
            }
        } else {
            // Permission revoked or not granted
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("MapScreen", "Location updates removed (permission denied).")
            isTracking = false // Stop tracking if permission is lost
        }

        // Clean up listener when the effect is disposed
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("MapScreen", "Location updates removed on DisposeEffect.")
        }
    }

    // Logic to automatically re-center map on user if tracking is true and user hasn't interacted
    // This requires tracking user interaction, which is tricky with GoogleMap compose currently.
    // A simpler approach is to have a dedicated button to toggle `isTracking`.
    // Let's add a button for "My Location" that sets isTracking=true and moves the camera.
    // The onMapClick listener will set isTracking=false.

    Box(modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding() // Handle system bars
    ) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 144.dp, // Height when collapsed (adjust based on bottom bar + padding)
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetContent = {
                // Content changes based on selected tab
                when (selectedContent) {
                    BottomSheetContentType.FRIENDS -> {
                        FriendsBottomSheetContent(
                            friendScreenState = friendScreenState,
                            onSelectFriend = { friendId ->
                                friendScreenState = FriendScreenState.Profile(friendId)
                                // Optional: Move camera to friend's location if available
                                friends.firstOrNull { it.id == friendId }?.let { friend ->
                                    scope.launch {
                                        cameraPositionState.animate(
                                            update = CameraUpdateFactory.newLatLngZoom(
                                                LatLng(friend.lat, friend.lng),
                                                18f
                                            )
                                        )
                                    }
                                }
                            },
                            onClickBack = {
                                friendScreenState = FriendScreenState.List
                            }
                        )
                    }

                    BottomSheetContentType.MYPAGE -> {
                        MyPageBottomSheetContent()
                    }

                    BottomSheetContentType.SETTINGS -> {
                        SettingsBottomSheetContent(
                            onLogout = {
                                // 1) Token and Device Token 삭제
                                authViewModel.logout {
                                    // 2) Disconnect WebSocket before navigating
                                    stompClient.disconnect() // Explicitly disconnect

                                    // 3) Navigate to onboard screen
                                    navController.navigate("onboard") {
                                        popUpTo("map") { inclusive = true } // Remove map from back stack
                                        launchSingleTop = true // Avoid multiple copies
                                    }
                                }
                            }
                        )
                    }
                }
            }

        ) { innerPadding ->
            // 지도 부분
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter // Change to TopCenter for button
            ) {
                // WebSocket 테스트로 이동하는 버튼
                Button(
                    onClick = {
                        navController.navigate("websocket_test")
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .zIndex(2f)
                ) {
                    Text("WebSocket 테스트")
                }
                // 지도는 버튼 아래에 위치
                Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = hasLocationPermission,
                            mapType = MapType.NORMAL
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = true,
                            mapToolbarEnabled = false
                        ),
                        onMyLocationButtonClick = {
                            if (hasLocationPermission && currentLocation != null) {
                                isTracking = true
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(currentLocation!!, 18f)
                                    )
                                    Log.d("MapScreen", "My Location button clicked. Tracking enabled and camera moved.")
                                }
                                true
                            } else {
                                false
                            }
                        },
                        onMapClick = {
                            isTracking = false
                            Log.d("MapScreen", "Map clicked. Tracking disabled.")
                        },
                        onMapLongClick = {
                            isTracking = false
                            Log.d("MapScreen", "Map long clicked. Tracking disabled.")
                        }
                    ) {
                        friends.forEach { user ->
                            Marker(
                                state = MarkerState(position = LatLng(user.lat, user.lng)),
                                title = user.id,
                                snippet = user.id,
                                onClick = { marker ->
                                    Log.d("MapScreen", "Friend marker clicked: ${marker.title}")
                                    scope.launch {
                                        cameraPositionState.animate(
                                            update = CameraUpdateFactory.newLatLngZoom(
                                                marker.position,
                                                18f
                                            )
                                        )
                                        
                                        // 현재 위치와 마커 위치로 네이버 지도 길 찾기 실행
                                        if (currentLocation != null) {
                                            searchLoadToNaverMap(
                                                context = context,
                                                slat = currentLocation!!.latitude,
                                                slng = currentLocation!!.longitude,
                                                dlat = marker.position.latitude,
                                                dlng = marker.position.longitude
                                            )
                                        }
                                    }
                                    isTracking = false
                                    true
                                }
                            )
                        }
                    }
                }
            }
        }
        // 하단 고정 버튼 바
        // Ensure this doesn't overlap with the bottom sheet when expanded past peek height
        // zIndex helps, but proper layout/padding or adjusting sheet max height is better.
        // For now, zIndex + navigationBarsPadding helps keep it above some UI.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp) // Fixed height for the bar
                .background(Color.White)
                .zIndex(1f) // Ensure it's above the map but below the sheet content
                .padding(bottom = 16.dp), // Add padding below buttons inside the bar
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FixedBottomButton(
                icon = Icons.Default.Person,
                label = "친구",
                onClick = {
                    selectedContent = BottomSheetContentType.FRIENDS
                    friendScreenState = FriendScreenState.List // Reset to list when selecting friends tab
                    scope.launch { scaffoldState.bottomSheetState.expand() }
                }
            )
            FixedBottomButton(
                icon = Icons.Default.AccountCircle,
                label = "마이페이지",
                onClick = {
                    selectedContent = BottomSheetContentType.MYPAGE
                    scope.launch { scaffoldState.bottomSheetState.expand() }
                }
            )
            FixedBottomButton(
                icon = Icons.Default.Settings,
                label = "설정",
                onClick = {
                    selectedContent = BottomSheetContentType.SETTINGS
                    scope.launch { scaffoldState.bottomSheetState.expand() }
                }
            )
        }
    }
}

private fun searchLoadToNaverMap(context: Context, slat: Double, slng: Double, dlat: Double, dlng: Double) {
    // 위도, 경도를 주소로 변환
    val geocoder = Geocoder(context, Locale.KOREAN)
    val startLocationAddress = geocoder.getFromLocation(slat, slng, 1)
    val endLocationAddress = geocoder.getFromLocation(dlat, dlng, 1)
    val encodedStartAddress = encodeAddress(startLocationAddress?.get(0)?.getAddressLine(0).toString().replace("대한민국 ",""))
    val encodedEndAddress = encodeAddress(endLocationAddress?.get(0)?.getAddressLine(0).toString().replace("대한민국 ",""))

    val url = "nmap://route/car?slat=${slat}&slng=${slng}&sname=${encodedStartAddress}&dlat=${dlat}&dlng=${dlng}&dname=${encodedEndAddress}"

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addCategory(Intent.CATEGORY_BROWSABLE)

    val installCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.GET_META_DATA
        )
    }

    // 네이버맵이 설치되어 있다면 앱으로 연결, 설치되어 있지 않다면 스토어로 이동
    if (installCheck.isEmpty()) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.nhn.android.nmap")))
    } else {
        context.startActivity(intent)
    }
}

private fun encodeAddress(address: String): String {
    return try {
        URLEncoder.encode(address, "UTF-8")
    } catch (e: Exception) {
        ""
    }
}