package com.project.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.data.model.EmotionalBond
import com.project.data.model.FamilyMember
import com.project.data.model.GenogramFiliation
import com.project.data.model.GenogramUnion
import com.project.data.model.Genograma
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class GenogramaRepository(private val userId: String) {

    private val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance())

    private val isValidUserId: Boolean get() = userId.isNotBlank()

    // --- Base Collection Navigation ---

    private fun getGenogramasCollection(patientId: String) =
        if (isValidUserId) db.collection("users").document(userId).collection("patients").document(patientId).collection("genogramas") else null

    private fun getMembersCollection(patientId: String, genogramaId: String) =
        getGenogramasCollection(patientId)?.document(genogramaId)?.collection("members")

    private fun getUnionsCollection(patientId: String, genogramaId: String) =
        getGenogramasCollection(patientId)?.document(genogramaId)?.collection("unions")
        
    private fun getFiliationsCollection(patientId: String, genogramaId: String) =
        getGenogramasCollection(patientId)?.document(genogramaId)?.collection("filiations")

    private fun getEmotionalBondsCollection(patientId: String, genogramaId: String) =
        getGenogramasCollection(patientId)?.document(genogramaId)?.collection("emotional_bonds")


    // --- GENOGRAMA METHODS ---

    fun getGenogramas(patientId: String): Flow<List<Genograma>> = if (!isValidUserId) flowOf(emptyList()) else {
        callbackFlow {
            val query = getGenogramasCollection(patientId)!!.orderBy("createdAt", Query.Direction.DESCENDING)
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> Genograma.fromSnapshot(doc) })
                }
            }
            awaitClose { listener.remove() }
        }
    }

    suspend fun createGenograma(patientId: String): String? {
        if (!isValidUserId) return null
        return try {
            val collection = getGenogramasCollection(patientId) ?: return null
            val docRef = collection.document()
            val genograma = Genograma(id = docRef.id, patientId = patientId)
            docRef.set(genograma.toMap()) // Offline-first sem await
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteGenograma(patientId: String, genogramaId: String): Boolean {
        if (!isValidUserId) return false
        return try {
            getGenogramasCollection(patientId)?.document(genogramaId)?.delete()?.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    // --- FAMILY MEMBERS ---

    fun getMembers(patientId: String, genogramaId: String): Flow<List<FamilyMember>> = if (!isValidUserId) flowOf(emptyList()) else {
        callbackFlow {
            val coll = getMembersCollection(patientId, genogramaId) ?: return@callbackFlow
            val listener = coll.orderBy("createdAt").addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                snap?.let { trySend(it.documents.mapNotNull { doc -> FamilyMember.fromSnapshot(doc) }) }
            }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveMember(patientId: String, genogramaId: String, member: FamilyMember): String? {
        if (!isValidUserId) return null
        return try {
            val coll = getMembersCollection(patientId, genogramaId) ?: return null
            val docRef = if (member.id.isEmpty()) coll.document() else coll.document(member.id)
            docRef.set(member.copy(id = docRef.id, genogramaId = genogramaId, patientId = patientId).toMap())
            docRef.id
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    suspend fun deleteMember(patientId: String, genogramaId: String, memberId: String) {
        getMembersCollection(patientId, genogramaId)?.document(memberId)?.delete()?.await()
    }


    // --- UNIONS ---

    fun getUnions(patientId: String, genogramaId: String): Flow<List<GenogramUnion>> = if (!isValidUserId) flowOf(emptyList()) else {
        callbackFlow {
            val coll = getUnionsCollection(patientId, genogramaId) ?: return@callbackFlow
            val listener = coll.orderBy("createdAt").addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                snap?.let { trySend(it.documents.mapNotNull { doc -> GenogramUnion.fromSnapshot(doc) }) }
            }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveUnion(patientId: String, genogramaId: String, union: GenogramUnion) {
        val coll = getUnionsCollection(patientId, genogramaId) ?: return
        val docRef = if (union.id.isEmpty()) coll.document() else coll.document(union.id)
        docRef.set(union.copy(id = docRef.id, genogramaId = genogramaId, patientId = patientId).toMap())
    }
    
    suspend fun deleteUnion(patientId: String, genogramaId: String, unionId: String) {
        getUnionsCollection(patientId, genogramaId)?.document(unionId)?.delete()?.await()
    }


    // --- FILIATIONS ---

    fun getFiliations(patientId: String, genogramaId: String): Flow<List<GenogramFiliation>> = if (!isValidUserId) flowOf(emptyList()) else {
        callbackFlow {
            val coll = getFiliationsCollection(patientId, genogramaId) ?: return@callbackFlow
            val listener = coll.orderBy("createdAt").addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                snap?.let { trySend(it.documents.mapNotNull { doc -> GenogramFiliation.fromSnapshot(doc) }) }
            }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveFiliation(patientId: String, genogramaId: String, filiation: GenogramFiliation) {
        val coll = getFiliationsCollection(patientId, genogramaId) ?: return
        val docRef = if (filiation.id.isEmpty()) coll.document() else coll.document(filiation.id)
        docRef.set(filiation.copy(id = docRef.id, genogramaId = genogramaId, patientId = patientId).toMap())
    }

    suspend fun deleteFiliation(patientId: String, genogramaId: String, filiationId: String) {
        getFiliationsCollection(patientId, genogramaId)?.document(filiationId)?.delete()?.await()
    }


    // --- EMOTIONAL BONDS ---

    fun getEmotionalBonds(patientId: String, genogramaId: String): Flow<List<EmotionalBond>> = if (!isValidUserId) flowOf(emptyList()) else {
        callbackFlow {
            val coll = getEmotionalBondsCollection(patientId, genogramaId) ?: return@callbackFlow
            val listener = coll.orderBy("createdAt").addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                snap?.let { trySend(it.documents.mapNotNull { doc -> EmotionalBond.fromSnapshot(doc) }) }
            }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveEmotionalBond(patientId: String, genogramaId: String, bond: EmotionalBond) {
        val coll = getEmotionalBondsCollection(patientId, genogramaId) ?: return
        val docRef = if (bond.id.isEmpty()) coll.document() else coll.document(bond.id)
        docRef.set(bond.copy(id = docRef.id, genogramaId = genogramaId, patientId = patientId).toMap())
    }

    suspend fun deleteEmotionalBond(patientId: String, genogramaId: String, bondId: String) {
        getEmotionalBondsCollection(patientId, genogramaId)?.document(bondId)?.delete()?.await()
    }
}
