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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextFieldDefaults // Ensure this is imported for OutlinedTextField colors
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun UwbPrecisionLocationPopup(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    viewModel: UwbLocationViewModel
) {
    val coroutineScope = rememberCoroutineScope()

    var peerAddressInput by remember { mutableStateOf(UwbLocationViewModel.DEFAULT_PEER_ADDRESS.toString()) }
    var configChannelInput by remember { mutableStateOf(UwbLocationViewModel.DEFAULT_CONFIG_CHANNEL.toString()) }
    var configPreambleInput by remember { mutableStateOf(UwbLocationViewModel.DEFAULT_CONFIG_PREAMBLE.toString()) }

    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    val distance by viewModel.distance.collectAsState()
    val angle by viewModel.angle.collectAsState()

    val localAddress by viewModel.localUwbAddress.collectAsState()
    val localChannel by viewModel.localUwbChannel.collectAsState()
    val localPreamble by viewModel.localUwbPreambleIndex.collectAsState()

    // Correctly access the isController state (it's already a State object)
    val isControllerRole = viewModel.isController

    val backgroundColor by animateColorAsState(
        targetValue = when {
            distance <= 1f && distance > 0f -> Color(0xFF006400)
            distance <= 5f && distance > 0f -> Color(0xFF008000)
            distance <= 20f && distance > 0f -> Color(0xFF32CD32)
            distance <= 100f && distance > 0f -> Color(0xFF90EE90)
            else -> Color(0xFFA9A9A9)
        },
        animationSpec = tween(durationMillis = 500), label = "bgColorAnim"
    )

    LaunchedEffect(distance) {
        if (distance > 0f && distance <= 20f) {
            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    // AnimatedVisibility handles the enter/exit for the whole popup
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Box( // This Box is now inside AnimatedVisibility to ensure it's part of the animation
            modifier = Modifier
                .fillMaxSize() // Fill the space made available by AnimatedVisibility
                .zIndex(10f),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            viewModel.stopUwb()
                            onDismissRequest()
                        },
                        modifier = Modifier.align(Alignment.Start).size(32.dp)
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

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "My Role: ${if (isControllerRole) "Controller" else "Controlee"}", // Use the corrected state
                        color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "My Addr: $localAddress",
                        color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "My Ch: $localChannel / Preamble: $localPreamble",
                        color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val peerChannelLabel = if (isControllerRole) "Peer Channel" else "My Listen Ch" // Use the corrected state
                    val peerPreambleLabel = if (isControllerRole) "Peer Preamble" else "My Listen Preamble" // Use the corrected state

                    OutlinedTextField(
                        value = peerAddressInput,
                        onValueChange = { peerAddressInput = it },
                        label = { Text("Peer Address", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    Row(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = configChannelInput,
                            onValueChange = { configChannelInput = it },
                            label = { Text(peerChannelLabel, color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = configPreambleInput,
                            onValueChange = { configPreambleInput = it },
                            label = { Text(peerPreambleLabel, color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
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
                                    viewModel.startUwb(peerAddressInput, configChannelInput, configPreambleInput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.9f),
                                contentColor = Color.DarkGray
                            ),
                            border = BorderStroke(1.dp, Color.White)
                        ) {
                            Text("연결 시작")
                        }
                        ControllerSwitch(viewModel) // Pass the whole viewModel
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}



@Composable
fun ControllerSwitch(viewModel: UwbLocationViewModel) { // Pass viewModel
    // Directly use viewModel.isController for the checked state
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Controller", color = Color.White, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = viewModel.isController, // Corrected: Direct use
            onCheckedChange = { viewModel.setControllerState(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

// Removed the problematic UwbLocationViewModel.isControllerState() extension function

@Composable
fun DirectionArrow(angle: Float) {
    val animatedAngle by animateFloatAsState(
        targetValue = angle,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "angleAnimation"
    )
    val arrowPainter = painterResource(id = R.drawable.arrow2)
    Icon(
        painter = arrowPainter,
        contentDescription = "Direction Arrow",
        modifier = Modifier
            .size(70.dp)
            .rotate(animatedAngle),
        tint = Color.White
    )
}

@Composable
fun DistanceMeter(distance: Float) {
    Text(
        text = if (distance != 0f) String.format("%.1f m", distance) else "--.- m", // Check if not exactly 0
        fontSize = 28.sp,
        color = Color.White,
        style = MaterialTheme.typography.headlineMedium
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFCCCCCC)
@Composable
fun UwbPrecisionLocationPopupPreview() {
    val context = LocalContext.current
    val mockUwbService = remember { uwbService(context) }
    val viewModel = remember { UwbLocationViewModel(mockUwbService) }

    Box(Modifier.fillMaxSize()) {
        UwbPrecisionLocationPopup(
            isVisible = true,
            onDismissRequest = {},
            viewModel = viewModel
        )
    }
}