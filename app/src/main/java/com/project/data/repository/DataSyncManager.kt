package com.project.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.project.data.model.Ecomapa
import com.project.data.model.Patient
import com.project.data.model.SupportNetwork
import com.project.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class DataSyncManager(private val context: Context) {

    private fun toJsonWithId(data: Map<String, Any>, id: String): JSONObject {
        return JSONObject(data).apply {
            put("id", id)
        }
    }

    suspend fun exportPatientData(
        patient: Patient,
        ecomapas: List<Ecomapa>,
        supportNetworks: List<SupportNetwork>,
        genogramasData: com.project.data.repository.GenogramaRepository.GenogramaExportData? = null
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val rootJson = JSONObject()
            
            // 1. Paciente
            rootJson.put("patient", JSONObject(patient.toMap()))
            
            // 2. Ecomapas
            val ecomapasArray = JSONArray()
            ecomapas.forEach { e -> ecomapasArray.put(toJsonWithId(e.toMap(), e.id)) }
            rootJson.put("ecomapas", ecomapasArray)

            // 3. Redes de Apoio
            val networksArray = JSONArray()
            supportNetworks.forEach { n -> networksArray.put(toJsonWithId(n.toMap(), n.id)) }
            rootJson.put("supportNetworks", networksArray)

            // 4. Genogramas
            val genogramasObj = JSONObject()
            if (genogramasData != null) {
                val gArray = JSONArray()
                genogramasData.genogramas.forEach { g -> gArray.put(toJsonWithId(g.toMap(), g.id)) }
                genogramasObj.put("genogramas", gArray)

                val mArray = JSONArray()
                genogramasData.members.forEach { m -> mArray.put(toJsonWithId(m.toMap(), m.id)) }
                genogramasObj.put("members", mArray)

                val uArray = JSONArray()
                genogramasData.unions.forEach { u -> uArray.put(toJsonWithId(u.toMap(), u.id)) }
                genogramasObj.put("unions", uArray)

                val fArray = JSONArray()
                genogramasData.filiations.forEach { f -> fArray.put(toJsonWithId(f.toMap(), f.id)) }
                genogramasObj.put("filiations", fArray)

                val bArray = JSONArray()
                genogramasData.bonds.forEach { b -> bArray.put(toJsonWithId(b.toMap(), b.id)) }
                genogramasObj.put("bonds", bArray)
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
            val patientRepo = com.project.data.repository.PatientRepository(userId)
            val ecomapaRepo = com.project.data.repository.EcomapaRepository(userId)

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
                val mappedEcomapaId = ecomapaIdMap[oldEcomapaId] ?: importedEcomapaIds.singleOrNull()

                if (mappedEcomapaId != null) {
                    val supportTypesJson = nJson.optJSONArray("supportTypes")
                    val supportTypes = mutableListOf<String>()
                    if (supportTypesJson != null) {
                        for (j in 0 until supportTypesJson.length()) {
                            supportTypes.add(supportTypesJson.getString(j))
                        }
                    }

                    val network = SupportNetwork(
                        name = nJson.optString("name", ""),
                        connectionType = nJson.optString("connectionType", ""),
                        category = nJson.optString("category", ""),
                        customCategory = nJson.optString("customCategory", ""),
                        contactFrequency = nJson.optString("contactFrequency", ""),
                        supportDirection = nJson.optString("supportDirection", ""),
                        description = nJson.optString("description", ""),
                        supportTypes = supportTypes,
                        generatesStress = nJson.optBoolean("generatesStress", false),
                        posX = if (nJson.has("posX")) nJson.getDouble("posX").toFloat() else null,
                        posY = if (nJson.has("posY")) nJson.getDouble("posY").toFloat() else null,
                        ecomapaId = mappedEcomapaId,
                        patientId = newPatientId,
                        createdAt = nJson.optLong("createdAt", System.currentTimeMillis())
                    )
                    ecomapaRepo.addSupportNetwork(newPatientId, mappedEcomapaId, network)
                }
            }

            onProgress("Restaurando Genogramas...")
            val genogramaRepo = com.project.data.repository.GenogramaRepository(userId)
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
                        val mappedGId = genogramaIdMap[oldGId] ?: importedGenogramaIds.singleOrNull()
                        if (mappedGId != null) {
                            val oldMId = mJson.optString("id", "")
                            val newMId = java.util.UUID.randomUUID().toString()
                            if (oldMId.isNotEmpty()) memberIdMap[oldMId] = newMId

                            val condicoesSaude = mutableListOf<String>()
                            val condicoesSaudeJson = mJson.optJSONArray("condicoesSaude")
                            if (condicoesSaudeJson != null) {
                                for (j in 0 until condicoesSaudeJson.length()) {
                                    condicoesSaude.add(condicoesSaudeJson.optString(j))
                                }
                            }

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
                                condicoesSaude = condicoesSaude,
                                observacoes = observacoes,
                                isEgo = mJson.optBoolean("isEgo", false),
                                geracao = mJson.optInt("geracao", 0),
                                offsetX = if (mJson.has("offsetX")) mJson.getDouble("offsetX").toFloat() else 0f,
                                offsetY = if (mJson.has("offsetY")) mJson.getDouble("offsetY").toFloat() else 0f,
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
                        val mappedGId = genogramaIdMap[oldGId] ?: importedGenogramaIds.singleOrNull()
                        if (mappedGId != null) {
                            val oldUId = uJson.optString("id", "")
                            val newUId = java.util.UUID.randomUUID().toString()
                            if (oldUId.isNotEmpty()) unionIdMap[oldUId] = newUId

                            val union = GenogramUnion(
                                id = newUId,
                                genogramaId = mappedGId,
                                patientId = newPatientId,
                                membroA = memberIdMap[uJson.optString("membroA")] ?: uJson.optString("membroA"),
                                membroB = memberIdMap[uJson.optString("membroB")] ?: uJson.optString("membroB"),
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
                        val mappedGId = genogramaIdMap[oldGId] ?: importedGenogramaIds.singleOrNull()
                        if (mappedGId != null) {
                            val gemelar = when {
                                fJson.has("gemelar") -> fJson.optString("gemelar", "nenhum")
                                !fJson.optBoolean("isGemeo", false) -> "nenhum"
                                fJson.optBoolean("isGemeosIdenticos", false) -> "identico"
                                else -> "fraterno"
                            }

                            val filiation = GenogramFiliation(
                                id = java.util.UUID.randomUUID().toString(),
                                genogramaId = mappedGId,
                                patientId = newPatientId,
                                uniaoOrigemId = unionIdMap[fJson.optString("uniaoOrigemId")] ?: fJson.optString("uniaoOrigemId"),
                                filhoId = memberIdMap[fJson.optString("filhoId")] ?: fJson.optString("filhoId"),
                                paiId = memberIdMap[fJson.optString("paiId")] ?: fJson.optString("paiId"),
                                maeId = memberIdMap[fJson.optString("maeId")] ?: fJson.optString("maeId"),
                                tipo = fJson.optString("tipo", fJson.optString("tipoFilhacao", "Biológico")),
                                gemelar = gemelar,
                                parGemelarId = memberIdMap[fJson.optString("parGemelarId")] ?: fJson.optString("parGemelarId"),
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
                        val mappedGId = genogramaIdMap[oldGId] ?: importedGenogramaIds.singleOrNull()
                        if (mappedGId != null) {
                            val tipo = when {
                                bJson.has("tipo") -> bJson.optString("tipo", "")
                                bJson.has("tipoVinculo") -> bJson.optString("tipoVinculo", "")
                                bJson.optBoolean("isConflict", false) -> "Conflituoso"
                                else -> "Muito Próximo"
                            }

                            val bond = EmotionalBond(
                                id = java.util.UUID.randomUUID().toString(),
                                genogramaId = mappedGId,
                                patientId = newPatientId,
                                membroAId = memberIdMap[bJson.optString("membroAId")] ?: bJson.optString("membroAId"),
                                membroBId = memberIdMap[bJson.optString("membroBId")] ?: bJson.optString("membroBId"),
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
