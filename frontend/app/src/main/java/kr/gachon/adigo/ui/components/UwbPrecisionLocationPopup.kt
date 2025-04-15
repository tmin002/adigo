package kr.gachon.adigo.ui.components

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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

@Composable
fun UwbPrecisionLocationPopup(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    viewModel: UwbLocationViewModel

) {


    val coroutineScope = rememberCoroutineScope()

    var address by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf("") }

    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    val distance by viewModel.distance.collectAsState()
    val angle by viewModel.angle.collectAsState()

    // 배경색을 거리별로 점진적으로 변경
    val backgroundColor by animateColorAsState(
        targetValue = when { // 거리 좁혀질수록 회색 -> 초록색으로 변경
            distance <= 5f -> Color(0xFF008000) // 1m 이하: 진한 초록색
            distance <= 20f -> Color(0xFF32CD32) // 20m 이하: 중간 초록색
            distance <= 100f -> Color(0xFF90EE90) // 100m 이하: 밝은 초록색
            else -> Color(0xFFA9A9A9) // 그 외: 회색
        }
    )

    // 진동 효과 추가
    LaunchedEffect(distance) {
        if (distance <= 20f) {
            vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    // 애니메이션 관련 변수
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else -100f, // 위에서 내려오는 애니메이션
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    // 애니메이션 관련 변수
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.5f, // 작게 시작해서 점점 커짐
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    Box(
        contentAlignment = Alignment.TopCenter // 화면 상단에 배치
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { -200 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -200 }) + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(1f) // 다이나믹 아일랜드 크기에서 시작
                    .height(170.dp)
                    .scale(animatedScale)
                    .offset(y = animatedOffsetY.dp)
                    .padding(30.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ){
                        DirectionArrow(angle) // 왼쪽 화살표

                        DistanceMeter(distance)

                        Column ()
                        {

                            Row(){

                                Column {

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.startUwb(address,channel)

                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Red
                                        ),
                                        border = BorderStroke(1.dp, Color.Red)
                                    ) {
                                        Text("확인")
                                    }

                                    ControllerSwitch(viewModel)



                                }

                                Column {

                                    TextField(
                                        value = address,
                                        onValueChange = { address = it }
                                    )

                                    TextField(
                                        value = channel,
                                        onValueChange = { channel = it }
                                    )

                                }
                            }

                        }



                    }



                    }



                }
            }
        }
    }



@Composable
fun ControllerSwitch(viewModel: UwbLocationViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            checked = viewModel.isController,
            onCheckedChange = { isChecked ->
                viewModel.setControllerState(isChecked)

            }
        )
    }
}



@Composable
fun DirectionArrow(angle: Float) {
    val animatedAngle by animateFloatAsState(
        targetValue = angle,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "angleAnimation"
    )
    val arrowPainter = painterResource(id = R.drawable.arrow2)
    Icon(
        painter = arrowPainter,
        contentDescription = "Direction Arrow",
        modifier = Modifier
            .size(100.dp)
            .rotate(animatedAngle),
        tint = Color.White
    )
}

@Composable
fun DistanceMeter(distance: Float) {
    Text(
        text = String.format("%.1f m", distance),
        fontSize = 18.sp,
        color = Color.White
    )
}

@Preview(showBackground = true)
@Composable
fun UwbPrecisionLocationPopupPreview() {
    val context = LocalContext.current
    val uwbService = remember { uwbService(context) }
    val viewModel = remember { UwbLocationViewModel(uwbService) }

    UwbPrecisionLocationPopup(
        isVisible = true,
        onDismissRequest = {},
        viewModel = viewModel
    )
}

