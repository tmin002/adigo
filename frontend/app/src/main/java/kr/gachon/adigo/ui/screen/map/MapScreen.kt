package kr.gachon.adigo.ui.screen.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
import kr.gachon.adigo.AdigoApplication
import kr.gachon.adigo.background.UserLocationProviderService
import kr.gachon.adigo.ui.screen.Screens
import kr.gachon.adigo.ui.viewmodel.AuthViewModel
import kr.gachon.adigo.ui.viewmodel.FriendLocationViewModel
import kr.gachon.adigo.ui.components.UwbPrecisionLocationPopup
import kr.gachon.adigo.ui.viewmodel.UwbLocationViewModel
import kr.gachon.adigo.service.uwbService
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(authViewModel: AuthViewModel, navController: NavController) {
    // ---------- State ----------
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var isTracking by remember { mutableStateOf(false) }
    var showUwbPopup by remember { mutableStateOf(false) }

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

    val friendLocationViewModel = remember {
        FriendLocationViewModel(AdigoApplication.AppContainer.userLocationRepo)
    }
    val friends by friendLocationViewModel.friends.collectAsState()

    var selectedContent by remember { mutableStateOf(BottomSheetContentType.FRIENDS) }
    var friendScreenState by remember { mutableStateOf<FriendScreenState>(FriendScreenState.List) }

    // ---------- Map camera ----------
    val seoul = LatLng(37.56, 126.97)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(seoul, 10f)
    }

    // ---------- Permission ----------
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
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                    isTracking = true
                }
            }
        }
    }

    // ---------- Side-effects ----------
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(cameraPositionState.position) {
        if (isTracking) {
            isTracking = false // user moved the map
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { loc ->
                        loc?.let {
                            currentLocation = LatLng(it.latitude, it.longitude)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MapScreen", "Failed lastLocation", e)
                    }
            } catch (se: SecurityException) {
                Log.e("MapScreen", "SecurityException", se)
            }
        }
    }

    // ---------- Real-time location updates ----------
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationRequest = remember {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
    }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val newPos = LatLng(loc.latitude, loc.longitude)
                currentLocation = newPos
                if (isTracking) {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(newPos, 18f))
                    }
                }
            }
        }
    }

    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (se: SecurityException) {
                Log.e("MapScreen", "requestLocationUpdates", se)
            }
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // ---------- Image cache ----------
    val profileImageCache = remember { mutableStateMapOf<Long, BitmapDescriptor>() }

    val uwbVm = remember {
        UwbLocationViewModel(uwbService(context))
    }

    // ---------- UI ----------
    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = with(LocalDensity.current) {
                80.dp + WindowInsets.navigationBars.getBottom(LocalDensity.current).toDp()
            },
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            sheetDragHandle = null,
            sheetSwipeEnabled = true,
            sheetContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f) // 화면 높이의 60%로 제한
                ) {
                    when (selectedContent) {
                        BottomSheetContentType.FRIENDS -> {
                            FriendsBottomSheetContent(
                                friendScreenState = friendScreenState,
                                onSelectFriend = { friend ->
                                    friendScreenState = FriendScreenState.Profile(friend)
                                    friends.firstOrNull { it.id == friend.id }?.let { fl ->
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(
                                                        fl.lat,
                                                        fl.lng
                                                    ), 18f
                                                )
                                            )
                                        }
                                    }
                                },
                                onNavigateToFriend = { friend ->
                                    val dest = friends.firstOrNull { it.id == friend.id }
                                    if (dest != null && currentLocation != null) {
                                        scope.launch {
                                            searchLoadToNaverMap(
                                                context,
                                                currentLocation!!.latitude,
                                                currentLocation!!.longitude,
                                                dest.lat,
                                                dest.lng
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
                                    authViewModel.logout {
                                        navController.navigate(Screens.OnBoard.name) {
                                            popUpTo(Screens.Main.name) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                if (!showUwbPopup) {
                    Button(
                        onClick = { showUwbPopup = true },
                        modifier = Modifier
                            .padding(16.dp)
                            .zIndex(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("어디고") }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(3f)
                ) {
                    UwbPrecisionLocationPopup(
                        isVisible = showUwbPopup,
                        onDismissRequest = { showUwbPopup = false },
                        viewModel = uwbVm
                    )
                }

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
                                }
                                true
                            } else false
                        },
                        onMapClick = { isTracking = false },
                        onMapLongClick = { isTracking = false }
                    ) {
                        friends.forEach { userLocation ->
                            val userEntity = AdigoApplication.AppContainer.userDatabaseRepo.getUserById(userLocation.id)
                            val profileUrl = userEntity?.profileImageURL

                            val markerIcon by remember(userLocation.id, profileUrl) {
                                derivedStateOf {
                                    if (profileUrl != null) {
                                        profileImageCache.getOrPut(userLocation.id) {
                                            val bmp = runBlocking { loadProfileBitmap(context, profileUrl) }
                                            bmp?.let { BitmapDescriptorFactory.fromBitmap(it) }
                                                ?: BitmapDescriptorFactory.defaultMarker()
                                        }
                                    } else BitmapDescriptorFactory.defaultMarker()
                                }
                            }

                            Marker(
                                state = MarkerState(position = LatLng(userLocation.lat, userLocation.lng)),
                                title = userEntity?.name ?: userLocation.id.toString(),
                                icon = markerIcon,
                                onClick = { marker ->
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(marker.position, 18f)
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

        // ---------- Fixed bottom button bar ----------
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(with(LocalDensity.current) {
                    (60.dp + WindowInsets.navigationBars.getBottom(LocalDensity.current).toDp())
                })
                .background(MaterialTheme.colorScheme.surface)
                .zIndex(1f)
                .padding(
                    bottom = with(LocalDensity.current) {
                        WindowInsets.navigationBars.getBottom(LocalDensity.current).toDp() + 16.dp
                    }
                ),
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

// ---------------------------------------------------------------------------
// Helper functions
// ---------------------------------------------------------------------------

suspend fun searchLoadToNaverMap(
    context: Context,
    slat: Double,
    slng: Double,
    dlat: Double,
    dlng: Double
) {
    val geocoder = Geocoder(context, Locale.KOREAN)

    val startAddr = withContext(Dispatchers.IO) {
        try { geocoder.getFromLocation(slat, slng, 1) } catch (e: IOException) {
            Log.e("Geocoder", "start", e); null
        }
    }
    val endAddr = withContext(Dispatchers.IO) {
        try { geocoder.getFromLocation(dlat, dlng, 1) } catch (e: IOException) {
            Log.e("Geocoder", "end", e); null
        }
    }

    val sName = encodeAddress(startAddr?.getOrNull(0)?.getAddressLine(0)?.replace("대한민국 ", "") ?: "출발지")
    val dName = encodeAddress(endAddr?.getOrNull(0)?.getAddressLine(0)?.replace("대한민국 ", "") ?: "도착지")

    val uri = Uri.parse("nmap://route/car?slat=$slat&slng=$slng&sname=$sName&dlat=$dlat&dlng=$dlng&dname=$dName")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { addCategory(Intent.CATEGORY_BROWSABLE) }

    val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    } else {
        context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.GET_META_DATA)
    }

    if (installed.isEmpty()) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.nhn.android.nmap")))
    } else {
        context.startActivity(intent)
    }
}

private fun encodeAddress(addr: String): String = try {
    URLEncoder.encode(addr, "UTF-8")
} catch (e: Exception) { "" }

private suspend fun loadProfileBitmap(context: Context, url: String): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
    try {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .apply(
                RequestOptions()
                    .transform(CircleCrop())
                    .override(100, 100)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
            )
            .submit()
            .get()
    } catch (e: Exception) {
        null
    }
}
