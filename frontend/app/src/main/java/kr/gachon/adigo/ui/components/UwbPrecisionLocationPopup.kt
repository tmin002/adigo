package kr.gachon.adigo.ui.components

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import kr.gachon.adigo.R
import kr.gachon.adigo.ui.viewmodel.UwbLocationViewModel

@Composable
fun UwbPrecisionLocationPopup(
    onDismissRequest: () -> Unit,
    viewModel: UwbLocationViewModel =UwbLocationViewModel()
) {
    val distance by viewModel.distance.collectAsState()
    val angle by viewModel.angle.collectAsState()

    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    val backgroundColor by animateColorAsState(
        targetValue = when {
            distance <= 1f -> Color(0xFF4CAF50)
            distance <= 10f -> Color(0xFFB2FF59)
            distance <= 20f -> Color(0xFFD7FFB8)
            else -> Color.White
        }
    )

    LaunchedEffect(distance) {
        if (distance <= 1.0f) {
            vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DirectionArrow(angle)

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = String.format("%.1f m", distance),
                    fontSize = 30.sp,
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
            .size(200.dp)
            .rotate(angle),
        tint = Color.White
    )
}


