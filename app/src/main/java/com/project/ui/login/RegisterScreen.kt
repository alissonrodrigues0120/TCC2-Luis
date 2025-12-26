package com.project.ui.login

import android.R.color
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val state by viewModel.state.collectAsState()

    // Observa sucesso
    LaunchedEffect(state) {
        if (state is RegisterState.Success) {
            onRegisterSuccess()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ){Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Cadastro",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                viewModel.register(
                    name = name,
                    email = email,
                    password = password
                )
            },
            enabled = state !is RegisterState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cadastrar")
        }

        TextButton(onClick = onBackToLogin) {
            Text(
                text = "Já tenho conta",
                color = MaterialTheme.colorScheme.primary
            )
        }

        when (state) {
            is RegisterState.Loading -> {
                CircularProgressIndicator()
            }

            is RegisterState.Error -> {
                Text(
                    text = (state as RegisterState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> Unit
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar para login")
        }

    }

    }
}
