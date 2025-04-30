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
import kr.gachon.adigo.ui.viewmodel.AuthViewModel

import kr.gachon.adigo.ui.viewmodel.FriendLocationViewModel
import android.content.Context
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
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import android.os.Looper
import androidx.compose.foundation.gestures.awaitFirstDown
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.ui.viewmodel.FriendListViewModel


@OptIn(ExperimentalMaterialApi::class)
@Composable

fun MapScreen(authViewModel: AuthViewModel, navController  : NavController) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(initialValue = Collapsed)
    )
    val scope = rememberCoroutineScope()
    val friendLocationViewModel = FriendLocationViewModel(AdigoApplication.userLocationRepo)


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
    }

    // 앱 시작 시 권한 요청
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        authViewModel.sendDeviceToken()
    }

    //내 현재 위치, 위치추적 여부
    var currentLocation by remember { mutableStateOf<LatLng?>(null)}
    var isTracking by remember {mutableStateOf(false)}
    //사용자 마지막 시간을 저장(클릭 후 움직임 없을 때 고정)
    val lastUserInteractionTime = remember { mutableStateOf(System.currentTimeMillis()) }


    //지도가 움직이면 추적 해제
    LaunchedEffect(cameraPositionState.position) {
        if(isTracking){
            isTracking = false
        }
    }

    //만약 위치 권한이 있으면 마지막 위치 받아오기
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedLocationClient.lastLocation
            location.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                }
            }
        }
    }



    //위치 업데이트 요청
    val fusedLocationClient = remember{ LocationServices.getFusedLocationProviderClient(context)}
    val locationRequest = remember{
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100L).apply {
            setMinUpdateIntervalMillis(50L)
        }.build()
    }

    //실시간으로 위치 추적(지도 중심으로 이동)
    DisposableEffect(Unit) {
        if (hasLocationPermission) {
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val loc = locationResult.lastLocation ?: return
                    val newLatLng = LatLng(loc.latitude, loc.longitude)
                    currentLocation = newLatLng

                    // 내 위치 추적이 활성화된 경우만 지도 중심 이동
                    if (isTracking) {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(newLatLng, 18f)
                            )
                        }
                    }
                }
            }


            // 위치 업데이트 요청
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            // 컴포저블이 dispose될 때 위치 업데이트 해제
            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } else {
            onDispose {}
        }
    }

    //마커 클릭후 조작 없을때 카메라 가운데로 위치
    LaunchedEffect(isTracking) {
        while(isTracking){
            delay(1000)
            val now = System.currentTimeMillis()
            if(now - lastUserInteractionTime.value > 3000){
                currentLocation?.let{ location ->
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(location, 18f)
                    )
                }
                break
            }

        }
    }



    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 144.dp,
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetContent = {
                when (selectedContent) {
                    BottomSheetContentType.FRIENDS -> {
                        FriendsBottomSheetContent(
                            friendScreenState = friendScreenState,
                            onSelectFriend = { friendId ->
                                friendScreenState = FriendScreenState.Profile(friendId)
                            },
                            onClickManage = {
                                friendScreenState = FriendScreenState.Manage
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
                                // 1) 토큰 삭제
                                authViewModel.logout {
                                    // 2) onboard 화면으로 라우팅
                                    navController.navigate("onboard") {
                                        popUpTo("map") { inclusive = true }
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
                contentAlignment = Alignment.BottomCenter

            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission // 내 위치 표시
                    ),
                    onMapClick = {
                        isTracking = false // 터치가 될때 추적 종료
                        lastUserInteractionTime.value = System.currentTimeMillis()
                    },

                ) {
                    friends.forEach { user ->
                        Marker(
                            state = MarkerState(position = LatLng(user.lat, user.lng)),
                            title = user.id,
                            onClick = {

                                scope.launch {
                                    cameraPositionState.animate(
                                        update = CameraUpdateFactory.newLatLngZoom(
                                            LatLng(user.lat, user.lng),
                                            18f // 친구 위치 확대 레벨
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
        // 하단 고정 버튼 바
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.White)
                .zIndex(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FixedBottomButton(
                icon = Icons.Default.Person,
                label = "친구",
                onClick = {
                    selectedContent = BottomSheetContentType.FRIENDS
                    friendScreenState = FriendScreenState.List
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