package com.project.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.data.model.Patient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PatientRepository(private val userId: String) {

    private val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance())

    private val patientsCollection = db.collection("users").document(userId).collection("patients")


    // Obter pacientes do usuário atual
    fun getPatients(): Flow<List<Patient>> = callbackFlow {
        val query = patientsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            snapshot?.let {
                trySend(
                    it.documents.map { doc ->
                        Patient.fromSnapshot(doc)
                    }
                )
            }
        }

        awaitClose { listener.remove() }
    }



    // Adicionar novo paciente
    suspend fun addPatient(patient: Patient): String? {
        return try {
            val newPatient = patient
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

