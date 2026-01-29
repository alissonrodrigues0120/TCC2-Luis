package com.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.data.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID



class HomeViewModel(repository: PatientRepository) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // CORREÇÃO 1: Especifique o tipo EXPLICITAMENTE
    private val _patients = MutableStateFlow<List<Patient>>(emptyList())

    // CORREÇÃO 2: Exponha como StateFlow com tipo específico
    val patients: StateFlow<List<Patient>> = _patients

    fun loadPatients() {
        viewModelScope.launch {
            try {
                if (userId.isEmpty()) return@launch

                val query = db.collection("users").document(userId).collection("patients")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

                val documents = query.get().await().documents
                val loadedPatients = documents.map { document ->
                    Patient(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        age = document.getLong("age")?.toInt() ?: 0,
                        gender = document.getString("gender") ?: "Outro",
                        condition = document.getString("condition") ?: "Em tratamento"
                    )
                }
                // CORREÇÃO 3: Atualize com o tipo correto
                _patients.value = loadedPatients
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addPatient(patient: Patient, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                if (userId.isEmpty()) return@launch

                val patientId = UUID.randomUUID().toString()
                val newPatient = Patient(
                    id = patientId,
                    name = patient.name,
                    age = patient.age,
                    gender = patient.gender,
                    condition = patient.condition
                )

                db.collection("users").document(userId).collection("patients").document(patientId).set(
                    mapOf(
                        "name" to newPatient.name,
                        "age" to newPatient.age,
                        "gender" to newPatient.gender,
                        "condition" to newPatient.condition,
                        "userId" to userId,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()

                loadPatients()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePatient(patientId: String) {
        viewModelScope.launch {
            try {
                if (userId.isEmpty()) return@launch

                db.collection("patients").document(patientId).delete().await()
                loadPatients()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}