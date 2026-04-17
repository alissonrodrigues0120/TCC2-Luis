package com.project.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.data.model.Ecomapa
import com.project.data.model.SupportNetwork
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class EcomapaRepository(private val userId: String) {

    private val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance())

    private val isValidUserId: Boolean get() = userId.isNotBlank()

    // Base Collection for a Patient's Ecomapas
    private fun getEcomapasCollection(patientId: String) =
        if (isValidUserId) db.collection("users").document(userId).collection("patients").document(patientId).collection("ecomapas") else null

    // Base Collection for an Ecomapa's Support Networks
    private fun getSupportNetworksCollection(patientId: String, ecomapaId: String) =
        getEcomapasCollection(patientId)?.document(ecomapaId)?.collection("supportNetworks")

    // --- ECOMAPA METHODS ---

    fun getEcomapas(patientId: String): Flow<List<Ecomapa>> = if (!isValidUserId) flowOf(emptyList()) else {
        callbackFlow {
            val query = getEcomapasCollection(patientId)!!.orderBy("createdAt", Query.Direction.DESCENDING)
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> Ecomapa.fromSnapshot(doc) })
                }
            }
            awaitClose { listener.remove() }
        }
    }

    suspend fun createEcomapa(patientId: String): String? {
        if (!isValidUserId) return null
        return try {
            val collection = getEcomapasCollection(patientId) ?: return null
            val docRef = collection.document()
            val ecomapa = Ecomapa(id = docRef.id, patientId = patientId)
            docRef.set(ecomapa.toMap())
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteEcomapa(patientId: String, ecomapaId: String): Boolean {
        if (!isValidUserId) return false
        return try {
            val collection = getEcomapasCollection(patientId) ?: return false
            collection.document(ecomapaId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- SUPPORT NETWORK METHODS ---

    fun getSupportNetworks(patientId: String, ecomapaId: String): Flow<List<SupportNetwork>> = if (!isValidUserId) flowOf(emptyList()) else {
        callbackFlow {
            val collection = getSupportNetworksCollection(patientId, ecomapaId)
            if (collection == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val query = collection.orderBy("createdAt", Query.Direction.ASCENDING)
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.documents.mapNotNull { doc -> SupportNetwork.fromSnapshot(doc) })
                }
            }
            awaitClose { listener.remove() }
        }
    }

    suspend fun addSupportNetwork(patientId: String, ecomapaId: String, network: SupportNetwork): String? {
        if (!isValidUserId) return null
        return try {
            val collection = getSupportNetworksCollection(patientId, ecomapaId) ?: return null
            val docRef = if (network.id.isEmpty()) collection.document() else collection.document(network.id)
            val newNetwork = network.copy(id = docRef.id, ecomapaId = ecomapaId, patientId = patientId)
            docRef.set(newNetwork.toMap()).await()
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteSupportNetwork(patientId: String, ecomapaId: String, networkId: String): Boolean {
        if (!isValidUserId) return false
        return try {
            val collection = getSupportNetworksCollection(patientId, ecomapaId) ?: return false
            collection.document(networkId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportEcomapasData(patientId: String): Pair<List<Ecomapa>, List<SupportNetwork>> {
        if (!isValidUserId) return Pair(emptyList(), emptyList())
        return try {
            val ecomapasSet = mutableListOf<Ecomapa>()
            val supportNetworksSet = mutableListOf<SupportNetwork>()
            
            val collection = getEcomapasCollection(patientId) ?: return Pair(emptyList(), emptyList())
            val ecomapasSnap = collection.get().await()
            for (doc in ecomapasSnap.documents) {
                val ecomapa = Ecomapa.fromSnapshot(doc)
                ecomapasSet.add(ecomapa)
                
                val networksSnap = collection.document(ecomapa.id).collection("supportNetworks").get().await()
                supportNetworksSet.addAll(networksSnap.documents.mapNotNull { SupportNetwork.fromSnapshot(it) })
            }
            Pair(ecomapasSet, supportNetworksSet)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(emptyList(), emptyList())
        }
    }
}
