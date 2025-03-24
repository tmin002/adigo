package kr.gachon.adigo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kr.gachon.adigo.ui.theme.AdigoTheme

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdigoTheme {
                // 추후 회원가입 UI 구현 예정
            }
        }
    }
}
