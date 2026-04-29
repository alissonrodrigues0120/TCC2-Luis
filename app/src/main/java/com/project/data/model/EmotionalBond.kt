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
    val tipoVinculo: String = "",
    val isConflict: Boolean = false,
    val details: String = "",
    val tipo: String = when {
        tipoVinculo.isNotBlank() -> tipoVinculo
        isConflict -> "Conflituoso"
        else -> ""
    }, // "muito_proximo" | "proximo" | "distante" | "conflituoso"...
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "genogramaId" to genogramaId,
            "patientId" to patientId,
            "membroAId" to membroAId,
            "membroBId" to membroBId,
            "tipo" to tipo,
            "createdAt" to createdAt
        )

        if (details.isNotBlank()) {
            map["details"] = details
        }
        if (isConflict || tipo == "Conflituoso") {
            map["isConflict"] = true
        }

        return map
    }

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): EmotionalBond {
            val isConflict = snapshot.getBoolean("isConflict") ?: false
            val tipo = snapshot.getString("tipo")
                ?: snapshot.getString("tipoVinculo")
                ?: if (isConflict) "Conflituoso" else ""

            return EmotionalBond(
                id = snapshot.id,
                genogramaId = snapshot.getString("genogramaId") ?: "",
                patientId = snapshot.getString("patientId") ?: "",
                membroAId = snapshot.getString("membroAId") ?: "",
                membroBId = snapshot.getString("membroBId") ?: "",
                tipo = tipo,
                isConflict = isConflict,
                details = snapshot.getString("details") ?: "",
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }
    }
}
