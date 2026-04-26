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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.progetto.ui.components.HeartButton
import com.example.progetto.ui.components.HeartTextField
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.ui.viewmodels.AuthViewModel
import com.example.progetto.ui.viewmodels.LoginUiState

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    // State variables match the database exactly
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val uiState = viewModel.uiState

    // Handle registration success
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onRegisterSuccess()
            viewModel.resetState()
        }
    }

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
                .padding(top = 20.dp, bottom = 40.dp),
            color = MaterialTheme.colorScheme.primary
        )

        // 2. Campi di Input
        HeartTextField(value = username, onValueChange = { username = it }, label = "Username")
        HeartTextField(value = email, onValueChange = { email = it }, label = "Email")
        HeartTextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true)
        HeartTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", isPassword = true)

        val displayError = localError ?: if (uiState is LoginUiState.Error) uiState.message else null
        
        if (displayError != null) {
            Text(
                text = displayError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Bottone Sign Up
        HeartButton(
            text = if (uiState is LoginUiState.Loading) "Registering..." else "Sign up",
            onClick = {
                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    localError = "Please fill all fields"
                } else if (password != confirmPassword) {
                    localError = "Passwords do not match"
                } else {
                    localError = null
                    viewModel.register(username, email, password, "android_device_1")
                }
            },
            enabled = uiState !is LoginUiState.Loading
        )

        TextButton(onClick = {
            viewModel.resetState()
            onNavigateBack()
        }) {
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