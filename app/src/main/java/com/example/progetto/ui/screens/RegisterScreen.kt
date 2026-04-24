package com.example.progetto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.ui.components.HeartButton
import com.example.progetto.ui.components.HeartTextField
import com.example.progetto.ui.theme.HeartMusicTheme

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {}
) {
    // State variables match the database exactly
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Titolo
        Text(
            text = "Subscribe...",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 20.dp, bottom = 40.dp), // Increased padding since photo is gone
            color = MaterialTheme.colorScheme.primary
        )

        // 2. Campi di Input
        HeartTextField(value = username, onValueChange = { username = it }, label = "Username")
        HeartTextField(value = email, onValueChange = { email = it }, label = "Email")
        HeartTextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true)

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Bottone Sign Up
        HeartButton(
            text = "Sign up",
            onClick = onRegisterSuccess
        )

        TextButton(onClick = onNavigateBack) {
            Text("Already have an account? Sign in", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    HeartMusicTheme {
        RegisterScreen()
    }
}