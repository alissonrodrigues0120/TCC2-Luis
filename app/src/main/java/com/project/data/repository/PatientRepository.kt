package com.project.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.data.model.Patient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class PatientRepository(private val userId: String) {

    private val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance())

    // ✅ Helper para validar o userId antes de usar
    private val isValidUserId: Boolean
        get() = userId.isNotBlank()

    // ✅ patientsCollection só é criado se userId for válido
    private val patientsCollection
        get() = if (isValidUserId) {
            db.collection("users").document(userId).collection("patients")
        } else {
            null
        }

    // ✅ Obter pacientes do usuário atual
    fun getPatients(): Flow<List<Patient>> = if (!isValidUserId) {
        // Se não tem userId válido, emite lista vazia imediatamente
        flowOf(emptyList())
    } else {
        callbackFlow {
            val query = patientsCollection!!
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                snapshot?.let {
                    trySend(
                        it.documents.mapNotNull { doc ->
                            Patient.fromSnapshot(doc)
                        }
                    )
                }
            }

            awaitClose { listener.remove() }
        }
    }

    // ✅ Adicionar novo paciente
    suspend fun addPatient(patient: Patient): String? {
        if (!isValidUserId) return null

        return try {
            val collection = patientsCollection ?: return null
            val documentRef = collection.document()
            val patientWithId = patient.copy(userId = userId, id = documentRef.id)
            documentRef.set(patientWithId.toMap())
            documentRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ✅ Atualizar paciente
    suspend fun updatePatient(patient: Patient): Boolean {
        if (!isValidUserId || patient.id.isBlank()) return false

        return try {
            val collection = patientsCollection ?: return false
            val documentRef = collection.document(patient.id)
            documentRef.update(patient.toMap())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ✅ Excluir paciente
    suspend fun deletePatient(patientId: String): Boolean {
        if (!isValidUserId || patientId.isBlank()) return false

        return try {
            val collection = patientsCollection ?: return false
            collection.document(patientId).delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ✅ Importar pacientes de CSV
    suspend fun importPatientsFromCsv(patients: List<Patient>): Boolean {
        if (!isValidUserId) return false

        return try {
            val collection = patientsCollection ?: return false
            val batch = db.batch()
            patients.forEach { patient ->
                val docRef = collection.document()
                val patientWithId = patient.copy(userId = userId, id = docRef.id)
                batch.set(docRef, patientWithId.toMap())
            }
            batch.commit()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    // ✅ Atualizar data de edição do paciente (gatilho de Genograma/Ecomapa)
    suspend fun updatePatientLastEditDate(patientId: String): Boolean {
        if (!isValidUserId || patientId.isBlank()) return false

        return try {
            val collection = patientsCollection ?: return false
            collection.document(patientId).update("remoteLastUpdate", System.currentTimeMillis())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
