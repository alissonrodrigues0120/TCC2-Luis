
package com.project.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _state = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val state: StateFlow<RegisterState> = _state

    fun register(
        name: String,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _state.value = RegisterState.Loading

            try {
                repository.register(
                    email = email,
                    password = password,
                    name = name
                )

                _state.value = RegisterState.Success

            } catch (e: Exception) {
                _state.value = RegisterState.Error(
                    e.message ?: "Erro ao cadastrar usuário"
                )
            }
        }
    }

    fun clearError() {
        if (_state.value is RegisterState.Error) {
            _state.value = RegisterState.Idle
        }
    }
}
