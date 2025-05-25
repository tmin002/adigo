package kr.gachon.adigo.ui.screen.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.*
import kr.adigo.adigo.database.entity.UserEntity
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.background.UserLocationProviderService
import kr.gachon.adigo.ui.viewmodel.AuthViewModel
import kr.gachon.adigo.ui.viewmodel.FriendLocationViewModel
import java.net.URLEncoder
import java.util.Locale
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.IOException
import androidx.compose.material3.*
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(authViewModel: AuthViewModel, navController: NavController) {
    // 1. Declare state variables at the top
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var isTracking by remember { mutableStateOf(false) }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true,
            confirmValueChange = { value ->
                value != SheetValue.Hidden
            }
        )
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

    // 프로필 이미지 캐시를 위한 상태 맵
    val profileImageCache = remember { mutableStateMapOf<Long, BitmapDescriptor>() }

    Box(// navigationBarsPadding 대신 systemBarsPadding 사용
    ) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 96.dp,
            topBar = null,  // 상단 영역 제거
            sheetContainerColor = MaterialTheme.colorScheme.surface,  // 바텀 시트 배경색 설정
            sheetDragHandle = null,  // 드래그 핸들 완전히 제거
            sheetContent = {
                // Content changes based on selected tab
                when (selectedContent) {
                    BottomSheetContentType.FRIENDS -> {
                        FriendsBottomSheetContent(
                            friendScreenState = friendScreenState,
                            onSelectFriend = { friend: UserEntity ->
                                friendScreenState = FriendScreenState.Profile(friend)
                                friends.firstOrNull { it.id == friend.id }?.let { friend ->
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
                            onNavigateToFriend = { friend: UserEntity ->
                                val friendLocation = friends.firstOrNull { it.id == friend.id }
                                if (friendLocation != null && currentLocation != null) {
                                    scope.launch {
                                        searchLoadToNaverMap(
                                            context = context,
                                            slat = currentLocation!!.latitude,
                                            slng = currentLocation!!.longitude,
                                            dlat = friendLocation.lat,
                                            dlng = friendLocation.lng
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
            },
            sheetSwipeEnabled = true,
            modifier = Modifier.fillMaxSize()
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
                        .zIndex(2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
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
                        friends.forEach { userLocation ->
                            // 1. id로 UserEntity 조회
                            val userEntity = AdigoApplication.AppContainer.userDatabaseRepo.getUserById(userLocation.id)
                            val profileUrl = userEntity?.profileImageURL

                            // 2. 프로필 이미지를 비트맵으로 변환 (캐시 사용)
                            val context = LocalContext.current
                            val markerIcon by remember(userLocation.id, profileUrl) {
                                derivedStateOf {
                                    if (profileUrl != null) {
                                        profileImageCache.getOrPut(userLocation.id) {
                                            // 캐시에 없으면 로드
                                            val bitmap = runBlocking { loadProfileBitmap(context, profileUrl) }
                                            bitmap?.let { BitmapDescriptorFactory.fromBitmap(it) }
                                                ?: BitmapDescriptorFactory.defaultMarker()
                                        }
                                    } else {
                                        BitmapDescriptorFactory.defaultMarker()
                                    }
                                }
                            }

                            Marker(
                                state = MarkerState(position = LatLng(userLocation.lat, userLocation.lng)),
                                title = userEntity?.name ?: userLocation.id.toString(),
                                icon = markerIcon,
                                onClick = { marker ->
                                    Log.d("MapScreen", "Friend marker clicked: ${marker.title}")
                                    scope.launch {
                                        cameraPositionState.animate(
                                            update = CameraUpdateFactory.newLatLngZoom(
                                                marker.position,
                                                18f
                                            )
                                        )
                                    }
                                    true
                                }
                            )
                        }
                    }
                }
            }
        }
        // 하단 고정 버튼 바
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(MaterialTheme.colorScheme.surface)
                .zIndex(1f)
                .padding(bottom = 16.dp),
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

suspend fun searchLoadToNaverMap(context: Context, slat: Double, slng: Double, dlat: Double, dlng: Double) {
    val geocoder = Geocoder(context, Locale.KOREAN)

    val startLocationAddress = withContext(Dispatchers.IO) {
        try {
            geocoder.getFromLocation(slat, slng, 1)
        } catch (e: IOException) {
            Log.e("Geocoder", "getFromLocation (start) failed: ${e.message}")
            null
        }
    }

    val endLocationAddress = withContext(Dispatchers.IO) {
        try {
            geocoder.getFromLocation(dlat, dlng, 1)
        } catch (e: IOException) {
            Log.e("Geocoder", "getFromLocation (end) failed: ${e.message}")
            null
        }
    }

    val encodedStartAddress = encodeAddress(startLocationAddress?.getOrNull(0)?.getAddressLine(0)?.replace("대한민국 ", "") ?: "출발지")
    val encodedEndAddress = encodeAddress(endLocationAddress?.getOrNull(0)?.getAddressLine(0)?.replace("대한민국 ", "") ?: "도착지")

    val url = "nmap://route/car?slat=$slat&slng=$slng&sname=$encodedStartAddress&dlat=$dlat&dlng=$dlng&dname=$encodedEndAddress"

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }

    val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.GET_META_DATA
        )
    }

    if (installed.isEmpty()) {
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

// 프로필 이미지를 비트맵으로 변환하는 함수
private suspend fun loadProfileBitmap(context: Context, url: String): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
    try {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .apply(RequestOptions()
                .transform(CircleCrop())
                .override(100, 100)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false))
            .submit()
            .get()
    } catch (e: Exception) {
        null
    }
}