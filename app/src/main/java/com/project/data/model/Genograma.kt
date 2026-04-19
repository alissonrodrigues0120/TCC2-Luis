package com.project.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot

data class Genograma(
    @DocumentId
    val id: String = "",
    val patientId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "patientId" to patientId,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): Genograma {
            return Genograma(
                id = snapshot.id,
                patientId = snapshot.getString("patientId") ?: "",
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }
    }
}
