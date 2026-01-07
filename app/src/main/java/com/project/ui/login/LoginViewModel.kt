package com.project.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, senha: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            try {
                // Realiza autenticação no Firebase
                val result = auth.signInWithEmailAndPassword(email.trim(), senha.trim()).await()

                if (result.user != null) {
                    _loginState.value = LoginState.Success(result.user!!.uid)
                } else {
                    _loginState.value = LoginState.Error("Falha na autenticação")
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("no user record") == true ->
                        "Usuário não cadastrado"
                    e.message?.contains("wrong password") == true ->
                        "Senha incorreta"
                    e.message?.contains("network") == true ->
                        "Sem conexão com a internet"
                    e.message?.contains("invalid email") == true ->
                        "E-mail inválido"
                    else ->
                        "Erro ao fazer login: ${e.message}"
                }
                _loginState.value = LoginState.Error(errorMessage)
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    // Verifica se já tem usuário autenticado
    fun checkCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val userId: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

// Extensão para Firebase Tasks (adicione em um arquivo separado se preferir)
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result, null)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Operation failed"))
            }
        }
    }
}