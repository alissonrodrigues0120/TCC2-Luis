package com.project.data.repository

import com.google.firebase.firestore.Query
import com.project.data.model.EmotionalBond
import com.project.data.model.FamilyMember
import com.project.data.model.GenogramFiliation
import com.project.data.model.GenogramUnion
import com.project.data.model.Genograma
import com.project.data.remote.UserFirestore
import com.project.data.remote.getCacheFirst
import com.project.data.remote.observeDocuments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class GenogramaRepository(private val userId: String) {

    private val firestore = UserFirestore(userId)

    private val isValidUserId: Boolean get() = firestore.hasUser

    private fun getGenogramasCollection(patientId: String) =
        firestore.patientSubcollection(patientId, GENOGRAMAS_COLLECTION)

    private fun getMembersCollection(patientId: String, genogramaId: String) =
        getGenogramasCollection(patientId)?.document(genogramaId)?.collection(MEMBERS_COLLECTION)

    private fun getUnionsCollection(patientId: String, genogramaId: String) =
        getGenogramasCollection(patientId)?.document(genogramaId)?.collection(UNIONS_COLLECTION)
        
    private fun getFiliationsCollection(patientId: String, genogramaId: String) =
        getGenogramasCollection(patientId)?.document(genogramaId)?.collection(FILIATIONS_COLLECTION)

    private fun getEmotionalBondsCollection(patientId: String, genogramaId: String) =
        getGenogramasCollection(patientId)?.document(genogramaId)?.collection(EMOTIONAL_BONDS_COLLECTION)

    private suspend fun touchPatientUpdate(patientId: String) {
        firestore.touchPatientUpdate(patientId)
    }


    // --- GENOGRAMA METHODS ---

    fun getGenogramas(patientId: String): Flow<List<Genograma>> =
        getGenogramasCollection(patientId)
            ?.orderBy("createdAt", Query.Direction.DESCENDING)
            .observeDocuments { Genograma.fromSnapshot(it) }

    suspend fun renameGenograma(patientId: String, genogramaId: String, newTitle: String) {
        if (!isValidUserId) return
        try {
            getGenogramasCollection(patientId)?.document(genogramaId)?.update("title", newTitle)?.await()
            touchPatientUpdate(patientId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createGenograma(patientId: String, title: String = ""): String? {
        if (!isValidUserId) return null
        return try {
            val collection = getGenogramasCollection(patientId) ?: return null
            val docRef = collection.document()
            val genograma = Genograma(id = docRef.id, patientId = patientId, title = title)
            docRef.set(genograma.toMap()).await()
            touchPatientUpdate(patientId)
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
            touchPatientUpdate(patientId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    // --- FAMILY MEMBERS ---

    fun getMembers(patientId: String, genogramaId: String): Flow<List<FamilyMember>> =
        getMembersCollection(patientId, genogramaId)
            ?.orderBy("createdAt")
            .observeDocuments { FamilyMember.fromSnapshot(it) }

    suspend fun saveMember(patientId: String, genogramaId: String, member: FamilyMember): String? {
        if (!isValidUserId) return null
        return try {
            val coll = getMembersCollection(patientId, genogramaId) ?: return null
            val docRef = if (member.id.isEmpty()) coll.document() else coll.document(member.id)
            docRef.set(member.copy(id = docRef.id, genogramaId = genogramaId, patientId = patientId).toMap()).await()
            touchPatientUpdate(patientId)
            docRef.id
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    suspend fun deleteMember(patientId: String, genogramaId: String, memberId: String) {
        getMembersCollection(patientId, genogramaId)?.document(memberId)?.delete()?.await()
        touchPatientUpdate(patientId)
    }


    // --- UNIONS ---

    fun getUnions(patientId: String, genogramaId: String): Flow<List<GenogramUnion>> =
        getUnionsCollection(patientId, genogramaId)
            ?.orderBy("createdAt")
            .observeDocuments { GenogramUnion.fromSnapshot(it) }

    suspend fun saveUnion(patientId: String, genogramaId: String, union: GenogramUnion) {
        val coll = getUnionsCollection(patientId, genogramaId) ?: return
        val docRef = if (union.id.isEmpty()) coll.document() else coll.document(union.id)
        docRef.set(union.copy(id = docRef.id, genogramaId = genogramaId, patientId = patientId).toMap()).await()
        touchPatientUpdate(patientId)
    }
    
    suspend fun deleteUnion(patientId: String, genogramaId: String, unionId: String) {
        getUnionsCollection(patientId, genogramaId)?.document(unionId)?.delete()?.await()
        touchPatientUpdate(patientId)
    }


    // --- FILIATIONS ---

    fun getFiliations(patientId: String, genogramaId: String): Flow<List<GenogramFiliation>> =
        getFiliationsCollection(patientId, genogramaId)
            ?.orderBy("createdAt")
            .observeDocuments { GenogramFiliation.fromSnapshot(it) }

    suspend fun saveFiliation(patientId: String, genogramaId: String, filiation: GenogramFiliation) {
        val coll = getFiliationsCollection(patientId, genogramaId) ?: return
        val docRef = if (filiation.id.isEmpty()) coll.document() else coll.document(filiation.id)
        docRef.set(filiation.copy(id = docRef.id, genogramaId = genogramaId, patientId = patientId).toMap()).await()
        touchPatientUpdate(patientId)
    }

    suspend fun deleteFiliation(patientId: String, genogramaId: String, filiationId: String) {
        getFiliationsCollection(patientId, genogramaId)?.document(filiationId)?.delete()?.await()
        touchPatientUpdate(patientId)
    }


    // --- EMOTIONAL BONDS ---

    fun getEmotionalBonds(patientId: String, genogramaId: String): Flow<List<EmotionalBond>> =
        getEmotionalBondsCollection(patientId, genogramaId)
            ?.orderBy("createdAt")
            .observeDocuments { EmotionalBond.fromSnapshot(it) }

    suspend fun saveEmotionalBond(patientId: String, genogramaId: String, bond: EmotionalBond) {
        val coll = getEmotionalBondsCollection(patientId, genogramaId) ?: return
        val docRef = if (bond.id.isEmpty()) coll.document() else coll.document(bond.id)
        docRef.set(bond.copy(id = docRef.id, genogramaId = genogramaId, patientId = patientId).toMap()).await()
        touchPatientUpdate(patientId)
    }

    suspend fun deleteEmotionalBond(patientId: String, genogramaId: String, bondId: String) {
        getEmotionalBondsCollection(patientId, genogramaId)?.document(bondId)?.delete()?.await()
        touchPatientUpdate(patientId)
    }

    suspend fun duplicateGenograma(patientId: String, originalGenogramaId: String, newTitle: String): String? {
        if (!isValidUserId) return null
        return try {
            val collection = getGenogramasCollection(patientId) ?: return null
            val docRef = collection.document()
            val newId = docRef.id
            val genograma = Genograma(id = newId, patientId = patientId, title = newTitle)
            docRef.set(genograma.toMap()).await()
            touchPatientUpdate(patientId)
            
            val oldMembersSnap = getMembersCollection(patientId, originalGenogramaId)?.get()?.await()
            val oldUnionsSnap = getUnionsCollection(patientId, originalGenogramaId)?.get()?.await()
            val oldFiliationsSnap = getFiliationsCollection(patientId, originalGenogramaId)?.get()?.await()
            val oldBondsSnap = getEmotionalBondsCollection(patientId, originalGenogramaId)?.get()?.await()
            
            val memberIdMap = mutableMapOf<String, String>()
            val unionIdMap = mutableMapOf<String, String>()
            
            oldMembersSnap?.documents?.forEach { doc ->
                val m = FamilyMember.fromSnapshot(doc)
                val mNewId = java.util.UUID.randomUUID().toString()
                memberIdMap[m.id] = mNewId
                saveMember(patientId, newId, m.copy(id = mNewId, genogramaId = newId))
            }
            
            oldUnionsSnap?.documents?.forEach { doc ->
                val u = GenogramUnion.fromSnapshot(doc)
                val uNewId = java.util.UUID.randomUUID().toString()
                unionIdMap[u.id] = uNewId
                saveUnion(patientId, newId, u.copy(
                    id = uNewId, 
                    genogramaId = newId,
                    membroA = memberIdMap[u.membroA] ?: u.membroA,
                    membroB = memberIdMap[u.membroB] ?: u.membroB
                ))
            }
            
            oldFiliationsSnap?.documents?.forEach { doc ->
                val f = GenogramFiliation.fromSnapshot(doc)
                saveFiliation(patientId, newId, f.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    genogramaId = newId,
                    uniaoOrigemId = unionIdMap[f.uniaoOrigemId] ?: f.uniaoOrigemId,
                    filhoId = memberIdMap[f.filhoId] ?: f.filhoId,
                    paiId = memberIdMap[f.paiId] ?: f.paiId,
                    maeId = memberIdMap[f.maeId] ?: f.maeId,
                    parGemelarId = memberIdMap[f.parGemelarId] ?: f.parGemelarId
                ))
            }
            
            oldBondsSnap?.documents?.forEach { doc ->
                val b = EmotionalBond.fromSnapshot(doc)
                saveEmotionalBond(patientId, newId, b.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    genogramaId = newId,
                    membroAId = memberIdMap[b.membroAId] ?: b.membroAId,
                    membroBId = memberIdMap[b.membroBId] ?: b.membroBId
                ))
            }

            newId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class GenogramaExportData(
        val genogramas: List<Genograma>,
        val members: List<FamilyMember>,
        val unions: List<GenogramUnion>,
        val filiations: List<GenogramFiliation>,
        val bonds: List<EmotionalBond>
    )

    suspend fun exportGenogramasData(patientId: String): GenogramaExportData {
        if (!isValidUserId) return GenogramaExportData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        return try {
            val genogramasList = mutableListOf<Genograma>()
            val membersList = mutableListOf<FamilyMember>()
            val unionsList = mutableListOf<GenogramUnion>()
            val filiationsList = mutableListOf<GenogramFiliation>()
            val bondsList = mutableListOf<EmotionalBond>()

            val collection = getGenogramasCollection(patientId) ?: return GenogramaExportData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            val genogramasSnap = collection.getCacheFirst()

            for (doc in genogramasSnap.documents) {
                val genograma = Genograma.fromSnapshot(doc)
                genogramasList.add(genograma)

                val membersSnap = getMembersCollection(patientId, genograma.id)?.getCacheFirst()
                membersSnap?.documents?.forEach { membersList.add(FamilyMember.fromSnapshot(it)) }

                val unionsSnap = getUnionsCollection(patientId, genograma.id)?.getCacheFirst()
                unionsSnap?.documents?.forEach { unionsList.add(GenogramUnion.fromSnapshot(it)) }

                val filiationsSnap = getFiliationsCollection(patientId, genograma.id)?.getCacheFirst()
                filiationsSnap?.documents?.forEach { filiationsList.add(GenogramFiliation.fromSnapshot(it)) }

                val bondsSnap = getEmotionalBondsCollection(patientId, genograma.id)?.getCacheFirst()
                bondsSnap?.documents?.forEach { bondsList.add(EmotionalBond.fromSnapshot(it)) }
            }

            GenogramaExportData(genogramasList, membersList, unionsList, filiationsList, bondsList)
        } catch (e: Exception) {
            e.printStackTrace()
            GenogramaExportData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }
    }

    private companion object {
        const val GENOGRAMAS_COLLECTION = "genogramas"
        const val MEMBERS_COLLECTION = "members"
        const val UNIONS_COLLECTION = "unions"
        const val FILIATIONS_COLLECTION = "filiations"
        const val EMOTIONAL_BONDS_COLLECTION = "emotional_bonds"
    }
}
