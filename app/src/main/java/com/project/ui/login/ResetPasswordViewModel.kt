package com.project.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import com.project.ui.login.ResetPasswordState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ResetPasswordViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _state =
        MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)
    val state: StateFlow<ResetPasswordState> = _state

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _state.value = ResetPasswordState.Loading

            try {
                repository.resetPassword(email)
                _state.value = ResetPasswordState.Success
            } catch (e: Exception) {
                _state.value = ResetPasswordState.Error(
                    e.message ?: "Erro ao enviar email de recuperação"
                )
            }
        }
    }

    fun clearError() {
        if (_state.value is ResetPasswordState.Error) {
            _state.value = ResetPasswordState.Idle
        }
    }
}


