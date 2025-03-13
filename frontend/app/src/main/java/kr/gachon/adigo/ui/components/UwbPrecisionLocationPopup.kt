package kr.gachon.adigo.ui.components

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import kr.gachon.adigo.R
import kr.gachon.adigo.ui.viewmodel.UwbLocationViewModel

@Composable
fun UwbPrecisionLocationPopup(
    onDismissRequest: () -> Unit,
    //viewModel: UwbLocationViewModel
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    //val distance by viewModel.distance.collectAsState()
    //val angle by viewModel.angle.collectAsState()
    val distance =0;
    var angle =0.01f;

    // 배경색을 거리별로 점진적으로 변경
    val backgroundColor by animateColorAsState(
        targetValue = when {
            distance <= 1f -> Color(0xFF4CAF50)
            distance <= 10f -> Color(0xFFB2FF59)
            distance <= 20f -> Color(0xFFD7FFB8)
            else -> Color.White
        }
    )

    // 진동 효과 추가
    LaunchedEffect(distance) {
        if (distance <= 1.0f) {
            vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    // 애니메이션 관련 변수
    val transition = rememberInfiniteTransition()
    val animatedScale by animateFloatAsState(
        targetValue = if (distance <= 1f) 1.2f else 1.0f,
        animationSpec = tween(durationMillis = 300)
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.5f) // 다이나믹 아일랜드 크기에서 시작
                .fillMaxHeight(0.1f)
                .offset(y = animatedOffsetY.dp)
                .scale(animatedScale)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DirectionArrow(angle)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = String.format("%.1f m", distance),
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun DirectionArrow(angle: Float) {
    val arrowPainter = painterResource(id = R.drawable.arrow2)
    Icon(
        painter = arrowPainter,
        contentDescription = "Direction Arrow",
        modifier = Modifier
            .size(100.dp)
            .rotate(angle),
        tint = Color.White
    )
}
