package com.project.data.model

import com.google.firebase.firestore.DocumentId

data class SupportNetwork(
    @DocumentId
    val id: String = "",
    val ecomapaId: String = "",
    val patientId: String = "",
    val name: String = "",
    val connectionType: String = "",
    val category: String = "",
    val customCategory: String = "",
    val contactFrequency: String = "",
    val supportDirection: String = "",
    val description: String = "",
    val supportTypes: List<String> = emptyList(),
    val generatesStress: Boolean = false,
    val posX: Float? = null,
    val posY: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "ecomapaId" to ecomapaId,
            "patientId" to patientId,
            "name" to name,
            "connectionType" to connectionType,
            "category" to category,
            "customCategory" to customCategory,
            "contactFrequency" to contactFrequency,
            "supportDirection" to supportDirection,
            "description" to description,
            "supportTypes" to supportTypes,
            "generatesStress" to generatesStress,
            "createdAt" to createdAt
        )
        if (posX != null) map["posX"] = posX
        if (posY != null) map["posY"] = posY
        return map
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromSnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot): SupportNetwork {
            return SupportNetwork(
                id = snapshot.id,
                ecomapaId = snapshot.getString("ecomapaId") ?: "",
                patientId = snapshot.getString("patientId") ?: "",
                name = snapshot.getString("name") ?: "",
                connectionType = snapshot.getString("connectionType") ?: "",
                category = snapshot.getString("category") ?: "",
                customCategory = snapshot.getString("customCategory") ?: "",
                contactFrequency = snapshot.getString("contactFrequency") ?: "",
                supportDirection = snapshot.getString("supportDirection") ?: "",
                description = snapshot.getString("description") ?: "",
                supportTypes = (snapshot.get("supportTypes") as? List<String>) ?: emptyList(),
                generatesStress = snapshot.getBoolean("generatesStress") ?: false,
                posX = snapshot.getDouble("posX")?.toFloat(),
                posY = snapshot.getDouble("posY")?.toFloat(),
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }
    }
}
