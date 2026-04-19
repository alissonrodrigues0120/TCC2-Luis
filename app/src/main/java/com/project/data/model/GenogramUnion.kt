package com.project.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot

data class GenogramUnion(
    @DocumentId
    val id: String = "",
    val genogramaId: String = "",
    val patientId: String = "",
    val tipo: String = "", // "casamento" | "uniao_estavel" | "namoro"
    val status: String = "", // "ativo" | "separado" | "divorciado"
    val dataInicio: String = "",
    val dataFim: String = "",
    val membroA: String = "", // ID do FamilyMember A
    val membroB: String = "", // ID do FamilyMember B
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "genogramaId" to genogramaId,
        "patientId" to patientId,
        "tipo" to tipo,
        "status" to status,
        "dataInicio" to dataInicio,
        "dataFim" to dataFim,
        "membroA" to membroA,
        "membroB" to membroB,
        "createdAt" to createdAt
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): GenogramUnion {
            return GenogramUnion(
                id = snapshot.id,
                genogramaId = snapshot.getString("genogramaId") ?: "",
                patientId = snapshot.getString("patientId") ?: "",
                tipo = snapshot.getString("tipo") ?: "",
                status = snapshot.getString("status") ?: "",
                dataInicio = snapshot.getString("dataInicio") ?: "",
                dataFim = snapshot.getString("dataFim") ?: "",
                membroA = snapshot.getString("membroA") ?: "",
                membroB = snapshot.getString("membroB") ?: "",
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }
    }
}
