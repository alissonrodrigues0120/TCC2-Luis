package com.project.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot

data class GenogramFiliation(
    @DocumentId
    val id: String = "",
    val genogramaId: String = "",
    val patientId: String = "",
    val filhoId: String = "", // ID do FamilyMember Filho
    val uniaoOrigemId: String = "", // Pode ser vazio se filiação solo
    val paiId: String = "", 
    val maeId: String = "",
    val tipo: String = "", // "biologico" | "adotivo" | "criacao"
    val gemelar: String = "", // "nenhum" | "identico" | "fraterno"
    val parGemelarId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "genogramaId" to genogramaId,
        "patientId" to patientId,
        "filhoId" to filhoId,
        "uniaoOrigemId" to uniaoOrigemId,
        "paiId" to paiId,
        "maeId" to maeId,
        "tipo" to tipo,
        "gemelar" to gemelar,
        "parGemelarId" to parGemelarId,
        "createdAt" to createdAt
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): GenogramFiliation {
            return GenogramFiliation(
                id = snapshot.id,
                genogramaId = snapshot.getString("genogramaId") ?: "",
                patientId = snapshot.getString("patientId") ?: "",
                filhoId = snapshot.getString("filhoId") ?: "",
                uniaoOrigemId = snapshot.getString("uniaoOrigemId") ?: "",
                paiId = snapshot.getString("paiId") ?: "",
                maeId = snapshot.getString("maeId") ?: "",
                tipo = snapshot.getString("tipo") ?: "",
                gemelar = snapshot.getString("gemelar") ?: "",
                parGemelarId = snapshot.getString("parGemelarId") ?: "",
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }
    }
}
