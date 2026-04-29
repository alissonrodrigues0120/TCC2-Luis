package com.project.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot

data class GenogramFiliation(
    @DocumentId
    val id: String = "",
    val genogramaId: String = "",
    val patientId: String = "",
    val filhoId: String = "", // ID do FamilyMember Filho
    val uniaoOrigemId: String = "", // Pode ser vazio se filiacao solo
    val paiId: String = "",
    val maeId: String = "",
    val tipoFilhacao: String = "",
    val isGemeo: Boolean = false,
    val isGemeosIdenticos: Boolean = false,
    val tipo: String = if (tipoFilhacao.isNotBlank()) tipoFilhacao else "", // "biologico" | "adotivo" | "criacao"
    val gemelar: String = when {
        isGemeosIdenticos -> "identico"
        isGemeo -> "fraterno"
        else -> "nenhum"
    }, // "nenhum" | "identico" | "fraterno"
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
            val isGemeoLegado = snapshot.getBoolean("isGemeo") ?: false
            val isGemeosIdenticosLegado = snapshot.getBoolean("isGemeosIdenticos") ?: false
            val gemelarLegado = when {
                isGemeosIdenticosLegado -> "identico"
                isGemeoLegado -> "fraterno"
                else -> "nenhum"
            }

            return GenogramFiliation(
                id = snapshot.id,
                genogramaId = snapshot.getString("genogramaId") ?: "",
                patientId = snapshot.getString("patientId") ?: "",
                filhoId = snapshot.getString("filhoId") ?: "",
                uniaoOrigemId = snapshot.getString("uniaoOrigemId") ?: "",
                paiId = snapshot.getString("paiId") ?: "",
                maeId = snapshot.getString("maeId") ?: "",
                tipo = snapshot.getString("tipo") ?: snapshot.getString("tipoFilhacao") ?: "",
                gemelar = snapshot.getString("gemelar") ?: gemelarLegado,
                parGemelarId = snapshot.getString("parGemelarId") ?: "",
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }
    }
}
