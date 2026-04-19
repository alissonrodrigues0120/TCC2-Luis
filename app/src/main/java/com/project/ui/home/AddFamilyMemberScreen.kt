package com.project.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.data.model.FamilyMember
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFamilyMemberScreen(
    patientId: String,
    genogramaId: String,
    memberId: String? = null,
    viewModel: GenogramaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    
    // Existing Member for Edit Mode
    val existingMember = remember(memberId, state.members) {
        state.members.find { it.id == memberId }
    }

    var isEgo by remember { mutableStateOf(existingMember?.isEgo ?: false) }
    var name by remember { mutableStateOf(existingMember?.nome ?: "") }
    var birthData by remember { mutableStateOf(existingMember?.nascimento ?: "") }
    var sexo by remember { mutableStateOf(existingMember?.sexo ?: "M") }
    var vivo by remember { mutableStateOf(existingMember?.vivo ?: true) }
    var deathData by remember { mutableStateOf(existingMember?.falecimento ?: "") }
    var deathCause by remember { mutableStateOf(existingMember?.causaMorte ?: "") }
    var geracao by remember { mutableStateOf((existingMember?.geracao ?: 0).toFloat()) }
    var ocupacao by remember { mutableStateOf(existingMember?.ocupacao ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (memberId == null) "Novo Familiar" else "Editar Familiar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Ego Checkbox
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(
                    checked = isEgo,
                    onCheckedChange = { /* Bloqueado pois já é auto-povoado */ },
                    enabled = false // Bloqueia edicao
                )
                Text(if(isEgo) "Paciente Foco (Ego Centralizado)" else "Membro Familiar Típico", color = if(isEgo) Color(0xFF512DA8) else Color.Gray)
            }
            
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { newValue -> 
                    // Regra importada: Filtrar números e caracteres especiais
                    name = newValue.filter { it.isLetter() || it.isWhitespace() }
                },
                label = { Text("Nome do Familiar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Sexo Options
            Text("Sexo Biológico / Gênero Clínico", fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("M" to "Masculino", "F" to "Feminino", "NB" to "Não-Binário").forEach { (code, label) ->
                    FilterChip(
                        selected = sexo == code,
                        onClick = { sexo = code },
                        label = { Text(label) }
                    )
                }
            }

            // Generation Slider
            Text("Geração em relação ao Ego: ${geracao.toInt()}", fontSize = 14.sp)
            Slider(
                value = geracao,
                onValueChange = { geracao = it },
                valueRange = -2f..2f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )
            val generationDesc = when(geracao.toInt()) {
                -2 -> "Bisavós (-2)"
                -1 -> "Avós / Tios-avós (-1)"
                0 -> "Pais / Tios (0)"
                1 -> "Ego / Irmãos / Primos (1)"
                2 -> "Filhos / Sobrinhos (2)"
                else -> ""
            }
            Text("Classe: $generationDesc", fontSize = 12.sp, color = Color.Gray)

            OutlinedTextField(
                value = birthData,
                onValueChange = { birthData = it },
                label = { Text("Ano de Nascimento (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = ocupacao,
                onValueChange = { ocupacao = it },
                label = { Text("Ocupação Principal (Opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Vivo / Morto
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Switch(
                    checked = vivo,
                    onCheckedChange = { vivo = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (vivo) "Membro Vivo" else "Membro Falecido")
            }

            AnimatedVisibility(visible = !vivo) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = deathData,
                        onValueChange = { deathData = it },
                        label = { Text("Ano do Falecimento") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = deathCause,
                        onValueChange = { deathCause = it },
                        label = { Text("Causa da Morte (Opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val memberToSave = FamilyMember(
                        id = memberId ?: UUID.randomUUID().toString(),
                        genogramaId = genogramaId,
                        patientId = patientId,
                        nome = name,
                        nascimento = birthData,
                        sexo = sexo,
                        vivo = vivo,
                        falecimento = if(vivo) "" else deathData,
                        causaMorte = if(vivo) "" else deathCause,
                        geracao = geracao.toInt(),
                        ocupacao = ocupacao,
                        isEgo = isEgo,
                        createdAt = existingMember?.createdAt ?: System.currentTimeMillis()
                    )
                    viewModel.saveMember(patientId, genogramaId, memberToSave)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF512DA8))
            ) {
                Text(if (memberId == null) "Cadastrar Familiar" else "Salvar Alterações")
            }
        }
    }
}
