package com.project.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private var failedAttempts = 0

    fun login(email: String, senha: String) {
        if (failedAttempts >= 3) {
            _loginState.value = LoginState.TooManyFailures("Muitas tentativas falhas. Redefina sua senha ou tente novamente mais tarde.")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            try {
                val result = auth.signInWithEmailAndPassword(email.trim(), senha.trim()).await()

                if (result.user != null) {
                    failedAttempts = 0
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

                if (e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException || e.message?.contains("wrong password") == true) {
                    failedAttempts++
                }

                if (failedAttempts >= 3) {
                    _loginState.value = LoginState.TooManyFailures("Aviso: Falhas consecutivas de login detectadas. Confirme sua senha.")
                } else {
                    _loginState.value = LoginState.Error(errorMessage)
                }
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    fun checkCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val userId: String) : LoginState()
    data class Error(val message: String) : LoginState()
    data class TooManyFailures(val message: String) : LoginState()
}
