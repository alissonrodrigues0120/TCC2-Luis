package com.project.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot

data class FamilyMember(
    @DocumentId
    val id: String = "",
    val genogramaId: String = "",
    val patientId: String = "",
    val nome: String = "",
    val nascimento: String = "",
    val sexo: String = "", // "M" | "F" | "NB"
    val vivo: Boolean = true,
    val falecimento: String = "", 
    val causaMorte: String = "", 
    val geracao: Int = 0, // -2 | -1 | 0 | 1 | 2
    val ocupacao: String = "",
    val condicoesSaude: List<String> = emptyList(),
    val observacoes: String = "",
    val isEgo: Boolean = false, // True se este nó for o Paciente principal
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "genogramaId" to genogramaId,
        "patientId" to patientId,
        "nome" to nome,
        "nascimento" to nascimento,
        "sexo" to sexo,
        "vivo" to vivo,
        "falecimento" to falecimento,
        "causaMorte" to causaMorte,
        "geracao" to geracao,
        "ocupacao" to ocupacao,
        "condicoesSaude" to condicoesSaude,
        "observacoes" to observacoes,
        "isEgo" to isEgo,
        "createdAt" to createdAt
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromSnapshot(snapshot: DocumentSnapshot): FamilyMember {
            return FamilyMember(
                id = snapshot.id,
                genogramaId = snapshot.getString("genogramaId") ?: "",
                patientId = snapshot.getString("patientId") ?: "",
                nome = snapshot.getString("nome") ?: "",
                nascimento = snapshot.getString("nascimento") ?: "",
                sexo = snapshot.getString("sexo") ?: "",
                vivo = snapshot.getBoolean("vivo") ?: true,
                falecimento = snapshot.getString("falecimento") ?: "",
                causaMorte = snapshot.getString("causaMorte") ?: "",
                geracao = snapshot.getLong("geracao")?.toInt() ?: 0,
                ocupacao = snapshot.getString("ocupacao") ?: "",
                condicoesSaude = (snapshot.get("condicoesSaude") as? List<String>) ?: emptyList(),
                observacoes = snapshot.getString("observacoes") ?: "",
                isEgo = snapshot.getBoolean("isEgo") ?: false,
                createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }
    }
}
