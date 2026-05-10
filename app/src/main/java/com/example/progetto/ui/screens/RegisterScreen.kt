package com.example.progetto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // 1. ADDED THIS IMPORT
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    // 2. CHANGED 'remember' TO 'rememberSaveable'
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

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
            text = stringResource(R.string.register_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 20.dp, bottom = 40.dp),
            color = MaterialTheme.colorScheme.primary
        )

        // 2. Campi di Input
        HeartTextField(value = username, onValueChange = { username = it }, label = stringResource(R.string.register_username_label))
        HeartTextField(value = email, onValueChange = { email = it }, label = stringResource(R.string.register_email_label))
        HeartTextField(value = password, onValueChange = { password = it }, label = stringResource(R.string.register_password_label), isPassword = true)
        HeartTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = stringResource(R.string.register_confirm_password_label), isPassword = true)

        val displayError = localError ?: if (uiState is LoginUiState.Error) uiState.message else null

        if (displayError != null) {
            Text(
                text = displayError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { 
                        liveRegion = LiveRegionMode.Polite 
                    }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Bottone Sign Up
        HeartButton(
            text = if (uiState is LoginUiState.Loading) stringResource(R.string.register_registering) else stringResource(R.string.register_sign_up),
            onClick = {
                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    localError = context.getString(R.string.register_error_fields)
                } else if (password != confirmPassword) {
                    localError = context.getString(R.string.register_error_passwords_mismatch)
                } else {
                    localError = null
                    viewModel.register(username, email, password, "android_device_1")
                }
            },
            enabled = uiState !is LoginUiState.Loading
        )

        val loginLabel = stringResource(R.string.register_go_to_login_description)
        TextButton(
            onClick = {
                viewModel.resetState()
                onNavigateBack()
            },
            modifier = Modifier.semantics {
                role = Role.Button
                onClick(label = loginLabel) { 
                    viewModel.resetState()
                    onNavigateBack()
                    true 
                }
            }
        ) {
            Text(stringResource(R.string.register_already_have_account), color = MaterialTheme.colorScheme.primary)
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