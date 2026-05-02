package com.project.data.repository

import com.google.firebase.firestore.Query
import com.project.data.model.Ecomapa
import com.project.data.model.SupportNetwork
import com.project.data.remote.UserFirestore
import com.project.data.remote.getCacheFirst
import com.project.data.remote.observeDocuments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class EcomapaRepository(private val userId: String) {

    private val firestore = UserFirestore(userId)

    private val isValidUserId: Boolean get() = firestore.hasUser

    private fun getEcomapasCollection(patientId: String) =
        firestore.patientSubcollection(patientId, ECOMAPAS_COLLECTION)

    private fun getSupportNetworksCollection(patientId: String, ecomapaId: String) =
        getEcomapasCollection(patientId)?.document(ecomapaId)?.collection(SUPPORT_NETWORKS_COLLECTION)



    // --- ECOMAPA METHODS ---

    fun getEcomapas(patientId: String): Flow<List<Ecomapa>> =
        getEcomapasCollection(patientId)
            ?.orderBy("createdAt", Query.Direction.DESCENDING)
            .observeDocuments { Ecomapa.fromSnapshot(it) }

    private suspend fun touchPatientUpdate(patientId: String) {
        firestore.touchPatientUpdate(patientId)
    }

    suspend fun renameEcomapa(patientId: String, ecomapaId: String, newTitle: String) {
        if (!isValidUserId) return
        try {
            getEcomapasCollection(patientId)?.document(ecomapaId)?.update("title", newTitle)?.await()
            touchPatientUpdate(patientId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createEcomapa(patientId: String, title: String = ""): String? {
        if (!isValidUserId) return null
        return try {
            val collection = getEcomapasCollection(patientId) ?: return null
            val docRef = collection.document()
            val ecomapa = Ecomapa(id = docRef.id, patientId = patientId, title = title)
            docRef.set(ecomapa.toMap()).await()
            touchPatientUpdate(patientId)
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
            touchPatientUpdate(patientId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- SUPPORT NETWORK METHODS ---

    fun getSupportNetworks(patientId: String, ecomapaId: String): Flow<List<SupportNetwork>> =
        getSupportNetworksCollection(patientId, ecomapaId)
            ?.orderBy("createdAt", Query.Direction.ASCENDING)
            .observeDocuments { SupportNetwork.fromSnapshot(it) }

    suspend fun addSupportNetwork(patientId: String, ecomapaId: String, network: SupportNetwork): String? {
        if (!isValidUserId) return null
        return try {
            val collection = getSupportNetworksCollection(patientId, ecomapaId) ?: return null
            val docRef = if (network.id.isEmpty()) collection.document() else collection.document(network.id)
            val newNetwork = network.copy(id = docRef.id, ecomapaId = ecomapaId, patientId = patientId)
            docRef.set(newNetwork.toMap()).await()
            touchPatientUpdate(patientId)
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
            touchPatientUpdate(patientId)
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
            val ecomapasSnap = collection.getCacheFirst()
            for (doc in ecomapasSnap.documents) {
                val ecomapa = Ecomapa.fromSnapshot(doc)
                ecomapasSet.add(ecomapa)
                
                val networksSnap = collection
                    .document(ecomapa.id)
                    .collection(SUPPORT_NETWORKS_COLLECTION)
                    .getCacheFirst()
                supportNetworksSet.addAll(networksSnap.documents.mapNotNull { SupportNetwork.fromSnapshot(it) })
            }
            Pair(ecomapasSet, supportNetworksSet)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(emptyList(), emptyList())
        }
    }
    suspend fun duplicateEcomapa(patientId: String, originalEcomapaId: String, newTitle: String): String? {
        if (!isValidUserId) return null
        return try {
            val collection = getEcomapasCollection(patientId) ?: return null
            val docRef = collection.document()
            val newId = docRef.id
            
            val newEcomapa = Ecomapa(
                id = newId, 
                patientId = patientId, 
                title = newTitle
            )
            docRef.set(newEcomapa.toMap()).await()
            
            val networksSnap = collection
                .document(originalEcomapaId)
                .collection(SUPPORT_NETWORKS_COLLECTION)
                .get()
                .await()
            
            networksSnap.documents.forEach { doc ->
                val network = SupportNetwork.fromSnapshot(doc)
                addSupportNetwork(patientId, newId, network.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    ecomapaId = newId
                ))
            }
            newId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private companion object {
        const val ECOMAPAS_COLLECTION = "ecomapas"
        const val SUPPORT_NETWORKS_COLLECTION = "supportNetworks"
    }
}
