package com.project.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.project.data.model.EmotionalBond
import com.project.data.model.Ecomapa
import com.project.data.model.FamilyMember
import com.project.data.model.GenogramFiliation
import com.project.data.model.GenogramUnion
import com.project.data.model.Patient
import com.project.data.model.SupportNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.UUID

class DataSyncManager(private val context: Context) {

    private fun toJsonWithId(data: Map<String, Any>, id: String): JSONObject {
        return JSONObject(data).apply {
            put("id", id)
        }
    }

    private fun <T> toJsonArrayWithIds(
        items: List<T>,
        toMap: (T) -> Map<String, Any>,
        getId: (T) -> String
    ): JSONArray = JSONArray().apply {
        items.forEach { item ->
            put(toJsonWithId(toMap(item), getId(item)))
        }
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        val values = optJSONArray(key) ?: return emptyList()
        return List(values.length()) { index -> values.optString(index) }
    }

    private fun JSONObject.optFloatOrNull(key: String): Float? =
        if (has(key)) getDouble(key).toFloat() else null

    private fun JSONObject.optFloatOrZero(key: String): Float =
        optFloatOrNull(key) ?: 0f

    private fun newDocumentId(): String = UUID.randomUUID().toString()

    private fun mappedId(
        originalId: String,
        idMap: Map<String, String>,
        importedIds: List<String>
    ): String? = idMap[originalId] ?: importedIds.singleOrNull()

    private fun String.remapId(idMap: Map<String, String>): String =
        idMap[this] ?: this

    suspend fun exportPatientData(
        patient: Patient,
        ecomapas: List<Ecomapa>,
        supportNetworks: List<SupportNetwork>,
        genogramasData: GenogramaRepository.GenogramaExportData? = null
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val rootJson = JSONObject()
            
            // 1. Paciente
            rootJson.put("patient", JSONObject(patient.toMap()))
            
            // 2. Ecomapas
            rootJson.put("ecomapas", toJsonArrayWithIds(ecomapas, { it.toMap() }, { it.id }))

            // 3. Redes de Apoio
            rootJson.put("supportNetworks", toJsonArrayWithIds(supportNetworks, { it.toMap() }, { it.id }))

            // 4. Genogramas
            val genogramasObj = JSONObject()
            if (genogramasData != null) {
                genogramasObj.put(
                    "genogramas",
                    toJsonArrayWithIds(genogramasData.genogramas, { it.toMap() }, { it.id })
                )
                genogramasObj.put(
                    "members",
                    toJsonArrayWithIds(genogramasData.members, { it.toMap() }, { it.id })
                )
                genogramasObj.put(
                    "unions",
                    toJsonArrayWithIds(genogramasData.unions, { it.toMap() }, { it.id })
                )
                genogramasObj.put(
                    "filiations",
                    toJsonArrayWithIds(genogramasData.filiations, { it.toMap() }, { it.id })
                )
                genogramasObj.put(
                    "bonds",
                    toJsonArrayWithIds(genogramasData.bonds, { it.toMap() }, { it.id })
                )
            }
            rootJson.put("genogramasData", genogramasObj)

            // Salvar no Cache
            val fileName = "paciente_${patient.name.replace(" ", "_")}.tcc2"
            val file = File(context.cacheDir, fileName)
            
            FileOutputStream(file).use { output ->
                output.write(rootJson.toString(4).toByteArray())
            }

            // Retornar Uri Seguro pelo FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportPatientData(patient: Patient, userId: String): Uri? = withContext(Dispatchers.IO) {
        if (userId.isBlank() || patient.id.isBlank()) {
            return@withContext null
        }

        val ecomapaRepo = EcomapaRepository(userId)
        val genogramaRepo = GenogramaRepository(userId)
        val (ecomapas, supportNetworks) = ecomapaRepo.exportEcomapasData(patient.id)
        val genogramasData = genogramaRepo.exportGenogramasData(patient.id)

        exportPatientData(patient, ecomapas, supportNetworks, genogramasData)
    }

    fun shareExportedFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar Perfil do Paciente"))
    }

    suspend fun importPatientData(uri: Uri, userId: String, onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val jsonString = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            val rootJson = JSONObject(jsonString)

            val patientJson = rootJson.getJSONObject("patient")
            val ecomapasArray = rootJson.getJSONArray("ecomapas")
            val networksArray = rootJson.getJSONArray("supportNetworks")

            // Reconstruct and insert Patient
            val patientRepo = PatientRepository(userId)
            val ecomapaRepo = EcomapaRepository(userId)

            val originalPatient = Patient(
                name = patientJson.optString("name", "Importado"),
                age = patientJson.optInt("age", 0),
                gender = patientJson.optString("gender", "Outro"),
                condition = patientJson.optString("condition", "Em tratamento"),
                observations = patientJson.optString("observations", "Importado externamente."),
                birthDate = patientJson.optString("birthDate", ""),
                photoBase64 = patientJson.optString("photoBase64", ""),
                createdAt = patientJson.optLong("createdAt", System.currentTimeMillis()),
                remoteLastUpdate = patientJson.optLong("remoteLastUpdate", System.currentTimeMillis())
            )

            onProgress("Importando paciente...")
            val newPatientId = patientRepo.addPatient(originalPatient) ?: return@withContext false

            onProgress("Restaurando Ecomapas...")
            val ecomapaIdMap = mutableMapOf<String, String>() // Map old ID to new ID
            val importedEcomapaIds = mutableListOf<String>()
            for (i in 0 until ecomapasArray.length()) {
                val eJson = ecomapasArray.getJSONObject(i)
                val oldEcomapaId = eJson.optString("id", "")
                val title = eJson.optString("title", "")
                
                val newEcomapaId = ecomapaRepo.createEcomapa(newPatientId, title)
                if (newEcomapaId != null) {
                    importedEcomapaIds.add(newEcomapaId)
                    if (oldEcomapaId.isNotEmpty()) {
                        ecomapaIdMap[oldEcomapaId] = newEcomapaId
                    }
                }
            }

            onProgress("Conectando Redes de Apoio...")
            for (i in 0 until networksArray.length()) {
                val nJson = networksArray.getJSONObject(i)
                val oldEcomapaId = nJson.optString("ecomapaId", "")
                val mappedEcomapaId = mappedId(oldEcomapaId, ecomapaIdMap, importedEcomapaIds)

                if (mappedEcomapaId != null) {
                    val network = SupportNetwork(
                        name = nJson.optString("name", ""),
                        connectionType = nJson.optString("connectionType", ""),
                        category = nJson.optString("category", ""),
                        customCategory = nJson.optString("customCategory", ""),
                        contactFrequency = nJson.optString("contactFrequency", ""),
                        supportDirection = nJson.optString("supportDirection", ""),
                        description = nJson.optString("description", ""),
                        supportTypes = nJson.optStringList("supportTypes"),
                        generatesStress = nJson.optBoolean("generatesStress", false),
                        posX = nJson.optFloatOrNull("posX"),
                        posY = nJson.optFloatOrNull("posY"),
                        ecomapaId = mappedEcomapaId,
                        patientId = newPatientId,
                        createdAt = nJson.optLong("createdAt", System.currentTimeMillis())
                    )
                    ecomapaRepo.addSupportNetwork(newPatientId, mappedEcomapaId, network)
                }
            }

            onProgress("Restaurando Genogramas...")
            val genogramaRepo = GenogramaRepository(userId)
            val genogramasDataObj = rootJson.optJSONObject("genogramasData")
            
            if (genogramasDataObj != null && genogramasDataObj.has("genogramas")) {
                val gArray = genogramasDataObj.getJSONArray("genogramas")
                val mArray = genogramasDataObj.optJSONArray("members")
                val uArray = genogramasDataObj.optJSONArray("unions")
                val fArray = genogramasDataObj.optJSONArray("filiations")
                val bArray = genogramasDataObj.optJSONArray("bonds")

                val genogramaIdMap = mutableMapOf<String, String>()
                val memberIdMap = mutableMapOf<String, String>()
                val unionIdMap = mutableMapOf<String, String>()
                val importedGenogramaIds = mutableListOf<String>()

                // 1. Genogramas
                for (i in 0 until gArray.length()) {
                    val gJson = gArray.getJSONObject(i)
                    val oldGId = gJson.optString("id", "")
                    val title = gJson.optString("title", "Genograma Importado")
                    val newGId = genogramaRepo.createGenograma(newPatientId, title)
                    if (newGId != null) {
                        importedGenogramaIds.add(newGId)
                        if (oldGId.isNotEmpty()) {
                            genogramaIdMap[oldGId] = newGId
                        }
                    }
                }

                // 2. Members
                if (mArray != null) {
                    for (i in 0 until mArray.length()) {
                        val mJson = mArray.getJSONObject(i)
                        val oldGId = mJson.optString("genogramaId", "")
                        val mappedGId = mappedId(oldGId, genogramaIdMap, importedGenogramaIds)
                        if (mappedGId != null) {
                            val oldMId = mJson.optString("id", "")
                            val newMId = newDocumentId()
                            if (oldMId.isNotEmpty()) memberIdMap[oldMId] = newMId

                            val observacoes = buildString {
                                val observacoesAtuais = mJson.optString("observacoes", "").trim()
                                val escolaridadeLegada = mJson.optString("escolaridade", "").trim()

                                if (observacoesAtuais.isNotEmpty()) {
                                    append(observacoesAtuais)
                                }
                                if (escolaridadeLegada.isNotEmpty()) {
                                    if (isNotEmpty()) append("\n")
                                    append("Escolaridade: ")
                                    append(escolaridadeLegada)
                                }
                            }

                            val member = FamilyMember(
                                id = newMId,
                                genogramaId = mappedGId,
                                patientId = newPatientId,
                                nome = mJson.optString("nome", ""),
                                sexo = mJson.optString("sexo", "Outro"),
                                nascimento = mJson.optString("nascimento", mJson.optString("dataNascimento", "")),
                                vivo = mJson.optBoolean("vivo", true),
                                falecimento = mJson.optString("falecimento", mJson.optString("dataFalecimento", "")),
                                causaMorte = mJson.optString("causaMorte", ""),
                                ocupacao = mJson.optString("ocupacao", ""),
                                condicoesSaude = mJson.optStringList("condicoesSaude"),
                                observacoes = observacoes,
                                isEgo = mJson.optBoolean("isEgo", false),
                                geracao = mJson.optInt("geracao", 0),
                                offsetX = mJson.optFloatOrZero("offsetX"),
                                offsetY = mJson.optFloatOrZero("offsetY"),
                                createdAt = mJson.optLong("createdAt", System.currentTimeMillis())
                            )
                            genogramaRepo.saveMember(newPatientId, mappedGId, member)
                        }
                    }
                }

                // 3. Unions
                if (uArray != null) {
                    for (i in 0 until uArray.length()) {
                        val uJson = uArray.getJSONObject(i)
                        val oldGId = uJson.optString("genogramaId", "")
                        val mappedGId = mappedId(oldGId, genogramaIdMap, importedGenogramaIds)
                        if (mappedGId != null) {
                            val oldUId = uJson.optString("id", "")
                            val newUId = newDocumentId()
                            if (oldUId.isNotEmpty()) unionIdMap[oldUId] = newUId

                            val union = GenogramUnion(
                                id = newUId,
                                genogramaId = mappedGId,
                                patientId = newPatientId,
                                membroA = uJson.optString("membroA").remapId(memberIdMap),
                                membroB = uJson.optString("membroB").remapId(memberIdMap),
                                tipo = uJson.optString("tipo", uJson.optString("tipoUniao", "Casamento")),
                                status = uJson.optString("status", "Ativo"),
                                dataInicio = uJson.optString("dataInicio", ""),
                                dataFim = uJson.optString("dataFim", ""),
                                createdAt = uJson.optLong("createdAt", System.currentTimeMillis())
                            )
                            genogramaRepo.saveUnion(newPatientId, mappedGId, union)
                        }
                    }
                }

                // 4. Filiations
                if (fArray != null) {
                    for (i in 0 until fArray.length()) {
                        val fJson = fArray.getJSONObject(i)
                        val oldGId = fJson.optString("genogramaId", "")
                        val mappedGId = mappedId(oldGId, genogramaIdMap, importedGenogramaIds)
                        if (mappedGId != null) {
                            val gemelar = when {
                                fJson.has("gemelar") -> fJson.optString("gemelar", "nenhum")
                                !fJson.optBoolean("isGemeo", false) -> "nenhum"
                                fJson.optBoolean("isGemeosIdenticos", false) -> "identico"
                                else -> "fraterno"
                            }

                            val filiation = GenogramFiliation(
                                id = newDocumentId(),
                                genogramaId = mappedGId,
                                patientId = newPatientId,
                                uniaoOrigemId = fJson.optString("uniaoOrigemId").remapId(unionIdMap),
                                filhoId = fJson.optString("filhoId").remapId(memberIdMap),
                                paiId = fJson.optString("paiId").remapId(memberIdMap),
                                maeId = fJson.optString("maeId").remapId(memberIdMap),
                                tipo = fJson.optString("tipo", fJson.optString("tipoFilhacao", "Biológico")),
                                gemelar = gemelar,
                                parGemelarId = fJson.optString("parGemelarId").remapId(memberIdMap),
                                createdAt = fJson.optLong("createdAt", System.currentTimeMillis())
                            )
                            genogramaRepo.saveFiliation(newPatientId, mappedGId, filiation)
                        }
                    }
                }

                // 5. Emotional Bonds
                if (bArray != null) {
                    for (i in 0 until bArray.length()) {
                        val bJson = bArray.getJSONObject(i)
                        val oldGId = bJson.optString("genogramaId", "")
                        val mappedGId = mappedId(oldGId, genogramaIdMap, importedGenogramaIds)
                        if (mappedGId != null) {
                            val tipo = when {
                                bJson.has("tipo") -> bJson.optString("tipo", "")
                                bJson.has("tipoVinculo") -> bJson.optString("tipoVinculo", "")
                                bJson.optBoolean("isConflict", false) -> "Conflituoso"
                                else -> "Muito Próximo"
                            }

                            val bond = EmotionalBond(
                                id = newDocumentId(),
                                genogramaId = mappedGId,
                                patientId = newPatientId,
                                membroAId = bJson.optString("membroAId").remapId(memberIdMap),
                                membroBId = bJson.optString("membroBId").remapId(memberIdMap),
                                tipo = tipo,
                                isConflict = bJson.optBoolean("isConflict", tipo == "Conflituoso"),
                                details = bJson.optString("details", ""),
                                createdAt = bJson.optLong("createdAt", System.currentTimeMillis())
                            )
                            genogramaRepo.saveEmotionalBond(newPatientId, mappedGId, bond)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
