package com.project.ui.login

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

fun getPasswordStrength(password: String): Triple<String, Color, Float> {
    if (password.isEmpty()) return Triple("", Color.Transparent, 0f)
    
    val length = password.length
    val hasLower = password.any { it.isLowerCase() }
    val hasUpper = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    
    return when {
        length >= 8 && hasLower && hasUpper && hasDigit && hasSpecial -> Triple("Forte", Color(0xFF4CAF50), 1f) // Green
        length >= 6 && hasLower && hasDigit -> Triple("Média", Color(0xFFFF9800), 0.66f) // Orange
        else -> Triple("Fraca", Color(0xFFF44336), 0.33f) // Red
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var confirmEmail by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val state by viewModel.state.collectAsState()

    val emailsMatch = email == confirmEmail || confirmEmail.isEmpty()
    val passwordsMatch = password == confirmPassword || confirmPassword.isEmpty()
    
    val passwordStrength = getPasswordStrength(password)
    
    val isFormValid = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() &&
                      email == confirmEmail && password == confirmPassword &&
                      passwordStrength.first != "Fraca" // Prevent weak passwords

    LaunchedEffect(state) {
        if (state is RegisterState.Success) {
            onRegisterSuccess()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Cadastro",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.headlineMedium
            )
            
            Text(
                text = "Crie sua conta preenchendo os dados abaixo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            OutlinedTextField(
                value = name,
                onValueChange = { newValue -> 
                    name = newValue.filter { it.isLetter() || it.isWhitespace() }
                },
                label = { Text("Nome Completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = confirmEmail,
                onValueChange = { confirmEmail = it },
                label = { Text("Confirmar E-mail") },
                isError = !emailsMatch,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            AnimatedVisibility(visible = !emailsMatch) {
                Text("Os e-mails não coincidem.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "Ocultar" else "Mostrar")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Password Strength Indicator
            AnimatedVisibility(visible = password.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Força da senha:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = passwordStrength.first,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = passwordStrength.second
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { passwordStrength.third },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color.LightGray, RoundedCornerShape(4.dp)),
                        color = passwordStrength.second,
                        trackColor = Color.Transparent,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (passwordStrength.first == "Fraca") {
                        Text("Mínimo de 6 caracteres combinando letras e números.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            }

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar Senha") },
                isError = !passwordsMatch,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            AnimatedVisibility(visible = !passwordsMatch) {
                Text("As senhas não coincidem.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        viewModel.register(
                            name = name,
                            email = email,
                            password = password
                        )
                    }
                },
                enabled = state !is RegisterState.Loading && isFormValid,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Tragar dados e Cadastrar", fontSize = 16.sp)
            }

            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Já tenho conta, acessar",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            when (state) {
                is RegisterState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                is RegisterState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (state as RegisterState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }

                else -> Unit
            }
        }
    }
}
