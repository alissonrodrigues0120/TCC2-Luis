package com.project.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.project.data.model.Patient
import kotlinx.coroutines.tasks.await

class PatientRepository(private val userId: String) {
    private val db = FirebaseFirestore.getInstance()
    private val patientsCollection = db.collection("patients")

    // Obter pacientes do usuário atual
    suspend fun getPatients(): List<Patient> {
        return try {
            val query = patientsCollection.whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

            val documents = query.get().await().documents
            documents.map { Patient.fromSnapshot(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Adicionar novo paciente
    suspend fun addPatient(patient: Patient): String? {
        return try {
            val newPatient = patient.copy(userId = userId)
            val documentRef = patientsCollection.document()
            documentRef.set(newPatient.toMap()).await()
            documentRef.id
        } catch (e: Exception) {
            null
        }
    }

    // Atualizar paciente
    suspend fun updatePatient(patient: Patient): Boolean {
        return try {
            val documentRef = patientsCollection.document(patient.id)
            documentRef.update(patient.toMap()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Excluir paciente
    suspend fun deletePatient(patientId: String): Boolean {
        return try {
            patientsCollection.document(patientId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Importar pacientes de CSV (exemplo simplificado)
    suspend fun importPatientsFromCsv(patients: List<Patient>): Boolean {
        return try {
            val batch = db.batch()
            patients.forEach { patient ->
                val docRef = patientsCollection.document()
                batch.set(docRef, patient.copy(userId = userId, id = docRef.id).toMap())
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}