package com.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.ui.components.TooltipIconButton

import com.project.data.model.EmotionalBond
import com.project.data.model.FamilyMember
import com.project.data.model.GenogramFiliation
import com.project.data.model.GenogramUnion
import com.project.data.model.Genograma
import com.project.data.model.Patient
import com.project.data.repository.GenogramaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class GenogramaState(
    val genogramas: List<Genograma> = emptyList(),
    val members: List<FamilyMember> = emptyList(),
    val unions: List<GenogramUnion> = emptyList(),
    val filiations: List<GenogramFiliation> = emptyList(),
    val emotionalBonds: List<EmotionalBond> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class GenogramaViewModel(private val repository: GenogramaRepository) : ViewModel() {

    private val _state = MutableStateFlow(GenogramaState())
    val state: StateFlow<GenogramaState> = _state

    // --- GENOGRAMAS BAISCO ---

    fun loadGenogramas(patientId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.getGenogramas(patientId).collect { list ->
                _state.value = _state.value.copy(genogramas = list, isLoading = false)
            }
        }
    }

    fun createGenograma(patient: Patient, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val idx = repository.createGenograma(patient.id, "Genograma Clínico")
            if (idx != null) {
                // Auto-povoar o Ego Offline First
                val egoSexo = when(patient.gender.lowercase()) {
                    "masculino", "homem" -> "M"
                    "feminino", "mulher" -> "F"
                    else -> "Outro"
                }
                
                val egoMember = FamilyMember(
                    id = "",
                    genogramaId = idx,
                    patientId = patient.id,
                    nome = patient.name,
                    isEgo = true,
                    sexo = egoSexo,
                    geracao = 0,
                    vivo = true
                )
                repository.saveMember(patient.id, idx, egoMember)
                
                onCreated(idx)
            } else {
                _state.value = _state.value.copy(error = "Falha ao criar Genograma")
            }
        }
    }

    fun deleteGenograma(patientId: String, genogramaId: String) {
        viewModelScope.launch {
            repository.deleteGenograma(patientId, genogramaId)
        }
    }

    // --- CARREGAMENTO DE REDES DO GENOGRAMA ---

    fun loadGenogramaNodesAndEdges(patientId: String, genogramaId: String) {
        viewModelScope.launch {
            launch {
                repository.getMembers(patientId, genogramaId).collect { lst ->
                    _state.value = _state.value.copy(members = lst)
                }
            }
            launch {
                repository.getUnions(patientId, genogramaId).collect { lst ->
                    _state.value = _state.value.copy(unions = lst)
                }
            }
            launch {
                repository.getFiliations(patientId, genogramaId).collect { lst ->
                    _state.value = _state.value.copy(filiations = lst)
                }
            }
            launch {
                repository.getEmotionalBonds(patientId, genogramaId).collect { lst ->
                    _state.value = _state.value.copy(emotionalBonds = lst)
                }
            }
        }
    }

    // --- ADD / DELETE NODES & EDGES ---

    fun saveMember(patientId: String, genogramaId: String, member: FamilyMember, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveMember(patientId, genogramaId, member)
            if (id != null) onSuccess(id)
        }
    }

    fun updateMemberPosition(patientId: String, genogramaId: String, memberId: String, offsetX: Float, offsetY: Float) {
        viewModelScope.launch {
            val member = state.value.members.find { it.id == memberId } ?: return@launch
            repository.saveMember(patientId, genogramaId, member.copy(offsetX = offsetX, offsetY = offsetY))
        }
    }
    fun deleteMember(patientId: String, genogramaId: String, memberId: String) {
        viewModelScope.launch {
            // Cascade delete: Remover uniões, filiações e laços emocionais atrelados
            state.value.unions.filter { it.membroA == memberId || it.membroB == memberId }.forEach {
                repository.deleteUnion(patientId, genogramaId, it.id)
            }
            state.value.filiations.filter { it.paiId == memberId || it.maeId == memberId || it.filhoId == memberId }.forEach {
                repository.deleteFiliation(patientId, genogramaId, it.id)
            }
            state.value.emotionalBonds.filter { it.membroAId == memberId || it.membroBId == memberId }.forEach {
                repository.deleteEmotionalBond(patientId, genogramaId, it.id)
            }
            // Deletar o membro em si
            repository.deleteMember(patientId, genogramaId, memberId) 
        }
    }

    fun saveUnion(patientId: String, genogramaId: String, union: GenogramUnion) {
        viewModelScope.launch { repository.saveUnion(patientId, genogramaId, union) }
    }
    fun deleteUnion(patientId: String, genogramaId: String, unionId: String) {
        viewModelScope.launch { repository.deleteUnion(patientId, genogramaId, unionId) }
    }

    fun saveFiliation(patientId: String, genogramaId: String, filiation: GenogramFiliation) {
        viewModelScope.launch { repository.saveFiliation(patientId, genogramaId, filiation) }
    }
    fun deleteFiliation(patientId: String, genogramaId: String, filiationId: String) {
        viewModelScope.launch { repository.deleteFiliation(patientId, genogramaId, filiationId) }
    }

    fun saveEmotionalBond(patientId: String, genogramaId: String, bond: EmotionalBond) {
        viewModelScope.launch { repository.saveEmotionalBond(patientId, genogramaId, bond) }
    }
    fun deleteEmotionalBond(patientId: String, genogramaId: String, bondId: String) {
        viewModelScope.launch { repository.deleteEmotionalBond(patientId, genogramaId, bondId) }
    }

    fun duplicateGenograma(patientId: String, originalGenogramaId: String, currentTitle: String) {
        viewModelScope.launch {
            val title = if (currentTitle.isBlank()) "Genograma Clínico" else currentTitle
            repository.duplicateGenograma(patientId, originalGenogramaId, "$title Cópia")
        }
    }

    fun renameGenograma(patientId: String, genogramaId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameGenograma(patientId, genogramaId, newTitle)
        }
    }
}

class GenogramaViewModelFactory(private val repository: GenogramaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GenogramaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GenogramaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
