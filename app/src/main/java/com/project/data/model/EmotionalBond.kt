package com.project.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot

data class EmotionalBond(
    @DocumentId
    val id: String = "",
    val genogramaId: String = "",
    val patientId: String = "",
    val membroAId: String = "",
    val membroBId: String = "",
    val tipo: String = "", // "muito_proximo" | "proximo" | "distante" | "conflituoso"...
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "genogramaId" to genogramaId,
        "patientId" to patientId,
        "membroAId" to membroAId,
        "membroBId" to membroBId,
        "tipo" to tipo,
        "createdAt" to createdAt
    )

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): EmotionalBond {
            return EmotionalBond(
                id = snapshot.id,
                genogramaId = snapshot.getString("genogramaId") ?: "",
                patientId = snapshot.getString("patientId") ?: "",
                membroAId = snapshot.getString("membroAId") ?: "",
                membroBId = snapshot.getString("membroBId") ?: "",
                tipo = snapshot.getString("tipo") ?: "",
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }
    }
}
