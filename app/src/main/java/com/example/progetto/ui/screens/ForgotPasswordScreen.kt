package com.example.progetto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progetto.ui.components.HeartButton
import com.example.progetto.ui.components.HeartTextField

import androidx.compose.ui.res.stringResource
import com.example.progetto.R

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.progetto.ui.viewmodels.AuthViewModel
import com.example.progetto.ui.viewmodels.ForgotPasswordUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    val uiState = viewModel.forgotPasswordState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.forgot_password_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.playlist_back_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.forgot_password_recover_header),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.forgot_password_instructions),
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            HeartTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.register_email_label)
            )

            if (uiState is ForgotPasswordUiState.Error) {
                Text(
                    text = uiState.message.asString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState is ForgotPasswordUiState.Success) {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            HeartButton(
                text = if (uiState is ForgotPasswordUiState.Loading) stringResource(R.string.login_signing_in) else stringResource(R.string.forgot_password_send_link),
                onClick = { 
                    if (email.isNotBlank()) {
                        viewModel.forgotPassword(email)
                    }
                },
                enabled = uiState !is ForgotPasswordUiState.Loading
            )
        }
    }
}
