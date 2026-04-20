package com.project.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot

data class Genograma(
    @DocumentId
    val id: String = "",
    val patientId: String = "",
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "patientId" to patientId,
            "title" to title,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): Genograma {
            return Genograma(
                id = snapshot.id,
                patientId = snapshot.getString("patientId") ?: "",
                title = snapshot.getString("title") ?: "",
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = snapshot.getLong("updatedAt") ?: snapshot.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }
    }
}
