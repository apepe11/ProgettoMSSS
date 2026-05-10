package com.example.progetto.ui.screens

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.R
import com.example.progetto.ui.components.HeartButton
import com.example.progetto.ui.components.HeartTextField
import com.example.progetto.ui.theme.HeartMusicTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.progetto.ui.viewmodels.AuthViewModel
import com.example.progetto.ui.viewmodels.LoginUiState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.LiveRegionMode

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.platform.LocalContext

@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    viewModel:AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val uiState = viewModel.uiState

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Titolo
        Text(
            text = stringResource(R.string.login_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 40.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Logo (più piccolo rispetto alla Welcome)
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = stringResource(R.string.login_logo_description),
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
            label = stringResource(R.string.login_email_username_label)
        )

        HeartTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.login_password_label),
            isPassword = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        val displayError = localError ?: if (uiState is LoginUiState.Error) uiState.message else null
        
        if (displayError != null) {
            Text(
                text = displayError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { 
                        // Tell TalkBack to announce the error immediately
                        liveRegion = LiveRegionMode.Polite 
                    }
            )
        }

        // 4. Bottone Sign In
        HeartButton(
            text = if (uiState is LoginUiState.Loading) stringResource(R.string.login_signing_in) else stringResource(R.string.login_sign_in),
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    localError = context.getString(R.string.login_error_empty)
                } else {
                    localError = null
                    viewModel.login(email, password)
                }
            },
            enabled = uiState !is LoginUiState.Loading // Disabilita il click durante il caricamento
        )

        Spacer(modifier = Modifier.weight(1f))

        // 5. Opzioni extra
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val resetPasswordLabel = stringResource(R.string.login_reset_password_description)
            Text(
                text = stringResource(R.string.login_forgot_password),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = resetPasswordLabel
                    ) { onNavigateToForgotPassword() }
            )
            
            val createAccountLabel = stringResource(R.string.login_create_account_description)
            Row(modifier = Modifier.semantics(mergeDescendants = true) { }) {
                Text(
                    text = stringResource(R.string.login_no_account),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.welcome_sign_up),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = createAccountLabel
                    ) { onNavigateToRegister() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    HeartMusicTheme {
        LoginScreen()
    }
}
