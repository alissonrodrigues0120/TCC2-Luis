package com.project.ui.home


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.data.repository.PatientRepository
import com.project.data.model.Patient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class HomeScreenPatients(
    val patients: List<Patient> = listOf(),
    val isRefreshing: Boolean = false
)


class HomeViewModel(
    private val repository: PatientRepository
) : ViewModel() {



    private val _isRefreshing = MutableStateFlow(false)

    val screenState: StateFlow<HomeScreenPatients> =
        repository.getPatients()
            .combine(_isRefreshing) { patients, refreshing ->
                HomeScreenPatients(
                    patients = patients,
                    isRefreshing = refreshing
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                HomeScreenPatients()
            )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // UX only – Firestore já atualiza automaticamente
            kotlinx.coroutines.delay(600)
            _isRefreshing.value = false
        }
    }

    fun addPatient(patient: Patient, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val id = repository.addPatient(patient)
            if (id != null) {
                onSuccess()
            }
        }
    }

    fun updatePatient(patient: com.project.data.model.Patient, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.updatePatient(patient)
            if (success) {
                onSuccess()
            }
        }
    }

    fun deletePatient(patientId: String) {
        viewModelScope.launch {
            repository.deletePatient(patientId)
        }
    }

    fun getPatientById(patientId: String): Patient? {
        return screenState.value.patients.find { it.id == patientId }
    }
}