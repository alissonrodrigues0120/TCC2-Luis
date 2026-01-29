package com.project.data.model

import com.google.firebase.firestore.DocumentId

data class Patient(
    @DocumentId
    val id: String = "",
    val name: String,
    val age: Int,
    val gender: String,
    val condition: String = "Em tratamento",
    val observations: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = "",
    val remoteLastUpdate: Long = System.currentTimeMillis()
) {
    // Converter para Map para o Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "age" to age,
            "gender" to gender,
            "condition" to condition,
            "observations" to observations,
            "createdAt" to createdAt,
            "userId" to userId,
            "remoteLastUpdate" to remoteLastUpdate
        )
    }

    // Converter de DocumentSnapshot
    companion object {
        fun fromSnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot): Patient {
            return Patient(
                id = snapshot.id,
                name = snapshot.getString("name") ?: "",
                age = snapshot.getLong("age")?.toInt() ?: 0,
                gender = snapshot.getString("gender") ?: "Outro",
                condition = snapshot.getString("condition") ?: "Em tratamento",
                observations = snapshot.getString("observations") ?: "",
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis(),
                userId = snapshot.getString("userId") ?: "",
                remoteLastUpdate = snapshot.getLong("remoteLastUpdate") ?: System.currentTimeMillis()
            )
        }
    }
}