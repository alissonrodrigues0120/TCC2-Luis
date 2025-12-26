package com.project.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ResetPasswordScreen(
    onBackToLogin: () -> Unit,
    viewModel: ResetPasswordViewModel = viewModel()
) {
    var email by rememberSaveable { mutableStateOf("") }

    val state by viewModel.state.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Recuperar senha",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.clearError()
                },
                label = { Text("E-mail") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.resetPassword(email)
                },
                enabled = state !is ResetPasswordState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar link de recuperação")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBackToLogin) {
                Text(
                    text = "Voltar para a tela de login",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            when (state) {
                is ResetPasswordState.Loading -> {
                    CircularProgressIndicator()
                }

                is ResetPasswordState.Success -> {
                    Text(
                        text = "E-mail de recuperação enviado!",
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onBackToLogin) {
                        Text("Voltar para o login")
                    }
                }

                is ResetPasswordState.Error -> {
                    Text(
                        text = (state as ResetPasswordState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {}
            }
        }
    }
}
