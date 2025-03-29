import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lock // Changed from Timer in the previous user code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight // Import FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay

// --- Composable for the Verification Code Input Field ---
// (VerificationCodeInputField and VerificationCodeDigitInput composables remain the same as before)
@Composable
fun VerificationCodeInputField(
    modifier: Modifier = Modifier,
    codeLength: Int, // Removed default value here to enforce setting it
    onCodeComplete: (String) -> Unit
) {
    // ... (Implementation is identical to the previous version)
    // 1. State to hold the input value for each digit field
    val codeDigits = remember { List(codeLength) { mutableStateOf("") } }

    // 2. State to hold the complete code string (derived from individual digits)
    val completeCode by remember {
        derivedStateOf { codeDigits.joinToString("") { it.value } }
    }

    // 3. Focus Requesters to manage focus between fields
    val focusRequesters = remember { List(codeLength) { FocusRequester() } }

    // Automatically request focus on the first field when the composable appears
    LaunchedEffect(Unit) {
        delay(100) // Small delay to ensure UI is ready
        // Check if focusRequesters is not empty before accessing index 0
        if (focusRequesters.isNotEmpty()) {
            focusRequesters[0].requestFocus()
        }
    }

    // Notify the caller when the code is complete
    LaunchedEffect(completeCode) {
        if (completeCode.length == codeLength) {
            onCodeComplete(completeCode)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween // Or Arrangement.spacedBy(8.dp)
    ) {
        repeat(codeLength) { index ->
            VerificationCodeDigitInput( // Renamed composable
                digitValue = codeDigits[index].value,
                focusRequester = focusRequesters[index],
                onValueChange = { newValue ->
                    // Allow only single digit or empty string
                    val filteredValue = newValue.filter { it.isDigit() }.take(1)

                    // Update the current digit's state
                    codeDigits[index].value = filteredValue

                    if (filteredValue.isNotEmpty()) {
                        // Move focus to the next field if available
                        if (index < codeLength - 1) {
                            focusRequesters[index + 1].requestFocus()
                        } else {
                            // Optional: Hide keyboard if it's the last digit
                        }
                    } else { // Handle backspace on empty field
                        // Move focus to the previous field if available
                        if (index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun VerificationCodeDigitInput( // Renamed composable
    digitValue: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit
) {
    // ... (Implementation is identical to the previous version)
    Box(
        modifier = Modifier
            .width(45.dp) // Adjust width as needed
            .height(50.dp), // Adjust height as needed
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = digitValue,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground // Use appropriate theme color
            ),
            decorationBox = { innerTextField ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.weight(1f), // Pushes the divider down
                        contentAlignment = Alignment.Center
                    ) {
                        innerTextField() // Display the actual text field content (the digit)
                    }
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = if (digitValue.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray // Change color based on input
                    )
                }
            }
        )
    }
}


// --- VerificationCodeScreen with 4-digit input ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationCodeScreen(
    onBackPress: () -> Unit,
    onResendClick: () -> Unit,
    onSubmitCode: (String) -> Unit
) {
    // --- State ---
    val codeLength = 4 // *** Set code length to 4 ***
    var enteredCode by remember { mutableStateOf("") }

    // Timer state (example) - reset if needed for shorter time?
    var timeLeft by remember { mutableStateOf(5 * 60) } // Keeping 5 minutes for now
    val timerText = remember(timeLeft) { "%02d:%02d".format(timeLeft / 60, timeLeft % 60) }
    LaunchedEffect(key1 = timeLeft) { if (timeLeft > 0) { delay(1000L); timeLeft-- } }

    BackHandler { onBackPress() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* No title */ },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로 가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title - Updated text
            Text(
                text = "보내드린 인증번호를\n입력해주세요", // Removed "6자리"
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold, // Use imported FontWeight
                lineHeight = 30.sp
            )

            // Timer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = "남은 시간", tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(timerText, color = Color.Gray, fontSize = 14.sp)
            }

            // --- Verification Code Input Field ---
            VerificationCodeInputField(
                codeLength = codeLength, // *** Pass codeLength (4) ***
                onCodeComplete = { code ->
                    println("인증 코드 입력됨: $code")
                    enteredCode = code
                    // Optionally trigger submission automatically for 4 digits
                    // onSubmitCode(code)
                },
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Resend Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onResendClick)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("인증문자 재발송", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Submit button
            Button(
                onClick = { if (enteredCode.length == codeLength) onSubmitCode(enteredCode) }, // *** Check against codeLength (4) ***
                enabled = enteredCode.length == codeLength, // *** Enable when length is codeLength (4) ***
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("인증 확인")
            }
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun VerificationCodeInputField4DigitPreview() { // Updated Preview name
    MaterialTheme {
        var code by remember { mutableStateOf("") }
        // Explicitly set codeLength in preview if default is removed
        VerificationCodeInputField(codeLength = 4, onCodeComplete = { code = it })
    }
}

@Preview(showBackground = true, device = "id:pixel_4")
@Composable
fun VerificationCodeScreen4DigitPreview() { // Updated Preview name
    MaterialTheme {
        VerificationCodeScreen(
            onBackPress = {},
            onResendClick = {},
            onSubmitCode = { println("미리보기 제출 (4자리): $it") }
        )
    }
}