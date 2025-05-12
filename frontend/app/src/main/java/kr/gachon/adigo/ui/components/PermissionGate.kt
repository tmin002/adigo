package kr.gachon.adigo.ui.components

/* Android / Framework */
import android.Manifest
import android.content.Context
import android.content.Intent
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
    content: @Composable () -> Unit          // 권한 OK 후 보여줄 UI
) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(false) }

    /* ── 런처 ── */
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            // 배터리 최적화 예외까지 신청한 뒤 granted = true
            requestIgnoreBatteryOptim(ctx) { granted = true }
        } else {
            Toast.makeText(
                ctx,
                "권한 거부 시 위치 공유 기능이 제한됩니다.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /* ── 최초 실행 때 권한 요청 ── */
    LaunchedEffect(Unit) {
        val perms = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                add(Manifest.permission.UWB_RANGING)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.POST_NOTIFICATIONS)
        }
        launcher.launch(perms.toTypedArray())
    }

    /* ── 권한 승인되면 실제 UI 그리기 ── */
    if (granted) content()
}

/* 배터리 최적화 무시 요청 → 완료되면 onDone() 호출 */
private fun requestIgnoreBatteryOptim(ctx: Context, onDone: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${ctx.packageName}"))
            ctx.startActivity(intent)
        }
    }
    onDone()   // 바로 true 로 두고, 사용자가 화면 복귀 시점에도 다시 granted=true 유지
}