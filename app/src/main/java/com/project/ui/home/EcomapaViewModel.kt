package com.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.ui.components.TooltipIconButton

import com.project.data.model.Ecomapa
import com.project.data.model.SupportNetwork
import com.project.data.repository.EcomapaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EcomapaViewModel(private val repository: EcomapaRepository) : ViewModel() {

    // Ecomapas de um paciente específico
    private val _ecomapas = MutableStateFlow<List<Ecomapa>>(emptyList())
    val ecomapas: StateFlow<List<Ecomapa>> = _ecomapas.asStateFlow()

    // Redes de apoio de um ecomapa específico
    private val _supportNetworks = MutableStateFlow<List<SupportNetwork>>(emptyList())
    val supportNetworks: StateFlow<List<SupportNetwork>> = _supportNetworks.asStateFlow()

    fun loadEcomapas(patientId: String) {
        viewModelScope.launch {
            repository.getEcomapas(patientId).collect {
                _ecomapas.value = it
            }
        }
    }

    fun createEcomapa(patientId: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newId = repository.createEcomapa(patientId, "Ecomapa Clínico")
            if (newId != null) {
                onCreated(newId)
            }
        }
    }

    fun deleteEcomapa(patientId: String, ecomapaId: String) {
        viewModelScope.launch {
            repository.deleteEcomapa(patientId, ecomapaId)
        }
    }

    fun loadSupportNetworks(patientId: String, ecomapaId: String) {
        viewModelScope.launch {
            repository.getSupportNetworks(patientId, ecomapaId).collect {
                _supportNetworks.value = it
            }
        }
    }

    fun addSupportNetwork(patientId: String, ecomapaId: String, network: SupportNetwork, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val id = repository.addSupportNetwork(patientId, ecomapaId, network)
            if (id != null) {
                onSuccess()
            }
        }
    }

    fun updateNetworkPosition(patientId: String, ecomapaId: String, networkId: String, posX: Float, posY: Float) {
        viewModelScope.launch {
            val network = _supportNetworks.value.find { it.id == networkId } ?: return@launch
            repository.addSupportNetwork(patientId, ecomapaId, network.copy(posX = posX, posY = posY))
        }
    }

    fun deleteSupportNetwork(patientId: String, ecomapaId: String, networkId: String) {
        viewModelScope.launch {
            repository.deleteSupportNetwork(patientId, ecomapaId, networkId)
        }
    }

    suspend fun exportEcomapasData(patientId: String): Pair<List<Ecomapa>, List<SupportNetwork>> {
        return repository.exportEcomapasData(patientId)
    }

    fun duplicateEcomapa(patientId: String, originalEcomapaId: String, currentTitle: String) {
        viewModelScope.launch {
            val title = if (currentTitle.isBlank()) "Ecomapa Clínico" else currentTitle
            repository.duplicateEcomapa(patientId, originalEcomapaId, "$title Cópia")
        }
    }

    fun renameEcomapa(patientId: String, ecomapaId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameEcomapa(patientId, ecomapaId, newTitle)
        }
    }
}
