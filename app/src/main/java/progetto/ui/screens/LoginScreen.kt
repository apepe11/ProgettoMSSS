package progetto.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.R
import com.example.progetto.ui.components.HeartButton
import com.example.progetto.ui.components.HeartTextField
import com.example.progetto.ui.theme.HeartMusicTheme

@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Titolo
        Text(
            text = "HeartMusic",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 40.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Logo (più piccolo rispetto alla Welcome)
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 3. Campi di Input
        HeartTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email or Username"
        )

        HeartTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 4. Bottone Sign In
        HeartButton(
            text = "Sign in",
            onClick = onLoginSuccess
        )

        Spacer(modifier = Modifier.weight(1f))

        // 5. Forgot Password
        Text(
            text = "Forgot your password?",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clickable { onNavigateToForgotPassword() }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    HeartMusicTheme {
        LoginScreen()
    }
}