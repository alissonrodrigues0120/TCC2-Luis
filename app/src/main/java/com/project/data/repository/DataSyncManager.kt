package com.project.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.project.data.model.Ecomapa
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

class DataSyncManager(private val context: Context) {

    suspend fun exportPatientData(
        patient: Patient,
        ecomapas: List<Ecomapa>,
        supportNetworks: List<SupportNetwork>
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val rootJson = JSONObject()
            
            // 1. Paciente
            rootJson.put("patient", JSONObject(patient.toMap()))
            
            // 2. Ecomapas
            val ecomapasArray = JSONArray()
            ecomapas.forEach { e -> ecomapasArray.put(JSONObject(e.toMap())) }
            rootJson.put("ecomapas", ecomapasArray)

            // 3. Redes de Apoio
            val networksArray = JSONArray()
            supportNetworks.forEach { n -> networksArray.put(JSONObject(n.toMap())) }
            rootJson.put("supportNetworks", networksArray)

            // 4. Genogramas (Esqueleto Vazio para futuras implementacoes)
            rootJson.put("genogramas", JSONArray())

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
                createdAt = System.currentTimeMillis()
            )

            onProgress("Importando paciente...")
            val newPatientId = patientRepo.addPatient(originalPatient) ?: return@withContext false

            onProgress("Restaurando Ecomapas...")
            val ecomapaIdMap = mutableMapOf<String, String>() // Map old ID to new ID
            for (i in 0 until ecomapasArray.length()) {
                val eJson = ecomapasArray.getJSONObject(i)
                val oldEcomapaId = eJson.optString("id", "")
                
                val newEcomapaId = ecomapaRepo.createEcomapa(newPatientId)
                if (newEcomapaId != null && oldEcomapaId.isNotEmpty()) {
                    ecomapaIdMap[oldEcomapaId] = newEcomapaId
                }
            }

            onProgress("Conectando Redes de Apoio...")
            for (i in 0 until networksArray.length()) {
                val nJson = networksArray.getJSONObject(i)
                val oldEcomapaId = nJson.optString("ecomapaId", "")
                val mappedEcomapaId = ecomapaIdMap[oldEcomapaId]

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
                        ecomapaId = mappedEcomapaId,
                        patientId = newPatientId,
                        createdAt = System.currentTimeMillis()
                    )
                    ecomapaRepo.addSupportNetwork(newPatientId, mappedEcomapaId, network)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
