package com.project.data.model

import com.google.firebase.firestore.DocumentId

data class Ecomapa(
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
        fun fromSnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot): Ecomapa {
            return Ecomapa(
                id = snapshot.id,
                patientId = snapshot.getString("patientId") ?: "",
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }
    }
}
