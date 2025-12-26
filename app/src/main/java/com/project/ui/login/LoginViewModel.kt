package com.project.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.data.model.User
import com.project.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, senha: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            val user = User(
                email = email,
                senha = senha,
            )

            repository.saveUser(user)

        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    object Error : LoginState()
}
