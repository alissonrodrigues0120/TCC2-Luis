package com.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.ui.components.TooltipIconButton

import com.project.data.repository.EcomapaRepository

class EcomapaViewModelFactory(private val repository: EcomapaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EcomapaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EcomapaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
