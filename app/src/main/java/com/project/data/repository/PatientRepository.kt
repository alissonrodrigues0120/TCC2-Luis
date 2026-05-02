package com.project.data.repository

import com.google.firebase.firestore.Query
import com.project.data.model.Patient
import com.project.data.remote.UserFirestore
import com.project.data.remote.observeDocuments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class PatientRepository(private val userId: String) {

    private val firestore = UserFirestore(userId)
    private val db = firestore.db

    private val isValidUserId: Boolean
        get() = firestore.hasUser

    private val patientsCollection
        get() = firestore.patientsCollection()

    fun getPatients(): Flow<List<Patient>> =
        patientsCollection
            ?.orderBy("createdAt", Query.Direction.DESCENDING)
            .observeDocuments { Patient.fromSnapshot(it) }

    suspend fun addPatient(patient: Patient): String? {
        if (!isValidUserId) return null

        return try {
            val collection = patientsCollection ?: return null
            val documentRef = collection.document()
            val patientWithId = patient.copy(userId = userId, id = documentRef.id)
            documentRef.set(patientWithId.toMap()).await()
            documentRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updatePatient(patient: Patient): Boolean {
        if (!isValidUserId || patient.id.isBlank()) return false

        return try {
            val collection = patientsCollection ?: return false
            val documentRef = collection.document(patient.id)
            documentRef.update(patient.toMap()).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deletePatient(patientId: String): Boolean {
        if (!isValidUserId || patientId.isBlank()) return false

        return try {
            val collection = patientsCollection ?: return false
            collection.document(patientId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

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
            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updatePatientLastEditDate(patientId: String): Boolean {
        if (!isValidUserId || patientId.isBlank()) return false

        return try {
            val collection = patientsCollection ?: return false
            collection.document(patientId).update("remoteLastUpdate", System.currentTimeMillis()).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
