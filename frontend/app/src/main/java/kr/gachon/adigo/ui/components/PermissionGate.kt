package kr.gachon.adigo.ui.components

/* Android / Framework */
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

/* AndroidX / Compose */
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun PermissionGate(
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(false) }

    // 요청할 권한 리스트
    var permissionQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentRequest by remember { mutableStateOf<String?>(null) }

    // 권한 요청 런처
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 다음 권한으로 이동
            if (permissionQueue.isNotEmpty()) {
                val next = permissionQueue.first()
                permissionQueue = permissionQueue.drop(1)
                currentRequest = next
            } else {
                granted = true
                currentRequest = null
            }
        } else {
            Toast.makeText(ctx, "필수 권한이 거부되어 앱 기능이 제한됩니다.", Toast.LENGTH_LONG).show()
            currentRequest = null
        }
    }

    // 최초 실행: 권한 목록 필터링 및 첫 요청 시작
    LaunchedEffect(Unit) {
        val requiredPermissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                add(Manifest.permission.UWB_RANGING)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val denied = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
        }

        if (denied.isEmpty()) {
            granted = true
        } else {
            permissionQueue = denied.drop(1)
            currentRequest = denied.first()
        }
    }

    // 요청 트리거: currentRequest 변경되면 launch 실행
    LaunchedEffect(currentRequest) {
        currentRequest?.let { launcher.launch(it) }
    }

    if (granted) content()
}