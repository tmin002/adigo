package kr.gachon.adigo.ui.components

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.launch
import kr.gachon.adigo.R
import kr.gachon.adigo.service.uwbService
import kr.gachon.adigo.ui.viewmodel.UwbLocationViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape // For Text background

@Composable
fun UwbPrecisionLocationPopup(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    viewModel: UwbLocationViewModel

) {
    val coroutineScope = rememberCoroutineScope()

    var addressInput by remember { mutableStateOf("1234") } // Default UWB Address
    var channelInput by remember { mutableStateOf("11") }  // Default Preamble Index (for Controlee)

    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    val distance by viewModel.distance.collectAsState()
    val angle by viewModel.angle.collectAsState()

    // Collect local UWB info
    val localAddress by viewModel.localUwbAddress.collectAsState()
    val localChannel by viewModel.localUwbChannel.collectAsState()
    val localPreamble by viewModel.localUwbPreambleIndex.collectAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            distance <= 1f -> Color(0xFF006400) // ~1m: Dark Green
            distance <= 5f -> Color(0xFF008000) // 1-5m: Green
            distance <= 20f -> Color(0xFF32CD32) // 5-20m: Lime Green
            distance <= 100f -> Color(0xFF90EE90) // 20-100m: Light Green
            else -> Color(0xFFA9A9A9)
        },
        animationSpec = tween(durationMillis = 500)
    )

    LaunchedEffect(distance) {
        if (distance > 0f && distance <= 20f) { // Vibrate only if a valid distance is received
            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else -200f, // From top
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "offsetYAnimation"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.8f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "scaleAnimation"
    )

    if (isVisible) { // More direct control over visibility for enter/exit transitions
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f), // Ensure popup is on top
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = isVisible, // Control visibility for animations
                enter = slideInVertically(
                    initialOffsetY = { -it }, // Slide from top
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it }, // Slide to top
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f) // Slightly less than full width
                        .offset(y = animatedOffsetY.dp) // Use for dynamic offset if needed, but slideIn/Out handles initial
                        .scale(animatedScale) // Apply scale animation
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp), // Padding for content within card
                    shape = MaterialTheme.shapes.extraLarge, // More rounded corners
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp) // Padding inside the card
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.stopUwb() // Stop UWB before dismissing
                                onDismissRequest()
                            },
                            modifier = Modifier
                                .align(Alignment.Start) // Top Start
                                .size(32.dp) // Slightly larger touch target
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DirectionArrow(angle)
                            DistanceMeter(distance)
                        }

                        Spacer(Modifier.height(16.dp))

                        // Local UWB Info Display
                        Text(
                            text = "My Role: ${if(viewModel.isController) "Controller" else "Controlee"}",
                            color = Color.White, fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = "My Addr: $localAddress",
                            color = Color.White, fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = "My Ch: $localChannel / Preamble: $localPreamble",
                            color = Color.White, fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                value = addressInput,
                                onValueChange = { addressInput = it },
                                label = { Text("Peer Address", color = Color.White) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            )
                            OutlinedTextField(
                                value = channelInput, // This is Preamble for Controlee
                                onValueChange = { channelInput = it },
                                label = { Text("Peer Preamble", color = Color.White) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            )
                        }


                        Spacer(Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.startUwb(addressInput, channelInput)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.8f),
                                    contentColor = Color.DarkGray
                                ),
                                border = BorderStroke(1.dp, Color.White)
                            ) {
                                Text("연결 시작")
                            }
                            ControllerSwitch(viewModel)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun ControllerSwitch(viewModel: UwbLocationViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Controller Mode", color = Color.White, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = viewModel.isController,
            onCheckedChange = { isChecked ->
                viewModel.setControllerState(isChecked)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Green,
                checkedTrackColor = Color.Green.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.Gray
            )
        )
    }
}

@Composable
fun DirectionArrow(angle: Float) {
    val animatedAngle by animateFloatAsState(
        targetValue = angle, // Assuming angle is in degrees for rotation
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "angleAnimation"
    )
    val arrowPainter = painterResource(id = R.drawable.arrow2) // Ensure this drawable exists
    Icon(
        painter = arrowPainter,
        contentDescription = "Direction Arrow",
        modifier = Modifier
            .size(70.dp) // Adjusted size
            .rotate(animatedAngle),
        tint = Color.White
    )
}

@Composable
fun DistanceMeter(distance: Float) {
    Text(
        text = if (distance > 0f) String.format("%.1f m", distance) else "--.- m",
        fontSize = 28.sp, // Larger font for distance
        color = Color.White,
        style = MaterialTheme.typography.headlineMedium // Use a more prominent style
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFCCCCCC)
@Composable
fun UwbPrecisionLocationPopupPreview() {
    val context = LocalContext.current
    // This preview won't have real UWB functionality, so flows will show defaults.
    val mockUwbService = remember { uwbService(context) }
    val viewModel = remember { UwbLocationViewModel(mockUwbService) }

    // Simulate ViewModel state for preview
    LaunchedEffect(Unit) {
        // mockUwbService.setRole(true) // To populate flows for preview, if needed, but constructor call in VM does it
    }

    Box(Modifier.fillMaxSize()) { // Added a Box to better simulate screen context
        UwbPrecisionLocationPopup(
            isVisible = true,
            onDismissRequest = {},
            viewModel = viewModel
        )
    }
}