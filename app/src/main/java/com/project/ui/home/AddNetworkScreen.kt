package com.project.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.ui.components.TooltipIconButton

import com.project.data.model.SupportNetwork

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNetworkScreen(
    patientId: String,
    ecomapaId: String,
    networkId: String?,
    viewModel: EcomapaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    
    val supportNetworks by viewModel.supportNetworks.collectAsState()
    val existingNetwork = remember(networkId, supportNetworks) {
        if (networkId != null) {
            supportNetworks.find { it.id == networkId }
        } else null
    }

    var nomeInstituicao by remember(existingNetwork) { mutableStateOf(existingNetwork?.name ?: "") }
    
    var tipoConexao by remember(existingNetwork) { mutableStateOf(existingNetwork?.connectionType ?: "") }
    val tiposConexao = listOf("Selecionar", "Forte", "Fraca", "Conflituosa", "Neutra", "Compensatória")
    
    var categoria by remember(existingNetwork) { mutableStateOf(existingNetwork?.category ?: "") }
    val categorias = listOf("Selecionar", "Família", "Trabalho", "Religioso", "Comunidade", "Saúde", "Outro")
    
    var categoriaOutro by remember(existingNetwork) { mutableStateOf(existingNetwork?.customCategory ?: "") }
    
    var frequenciaContato by remember(existingNetwork) { mutableStateOf(existingNetwork?.contactFrequency ?: "") }
    val frequencias = listOf("Selecionar", "Diário", "Semanal", "Mensal", "Raro")
    
    var direcionamento by remember(existingNetwork) { mutableStateOf(existingNetwork?.supportDirection ?: "") }
    val direcionamentos = listOf("Selecionar", "Recebe Apoio", "Oferece Apoio", "Mútuo")

    var descricao by remember(existingNetwork) { mutableStateOf(existingNetwork?.description ?: "") }
    
    // Checkboxes state
    val tiposApoio = listOf("Emocional", "Financeiro", "Cuidado Físico", "Social", "Institucional", "Outro")
    val selectedApoios = remember(existingNetwork) { 
        mutableStateMapOf<String, Boolean>().apply {
            tiposApoio.forEach { 
                put(it, existingNetwork?.supportTypes?.contains(it) == true) 
            } 
        } 
    }

    var geraEstresse by remember(existingNetwork) { mutableStateOf(existingNetwork?.generatesStress ?: false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Rede de Apoio", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    TooltipIconButton(tooltipText = "Voltar", onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = nomeInstituicao,
                onValueChange = { nomeInstituicao = it },
                label = { Text("Nome da Instituição") },
                placeholder = { Text("Digite o nome") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            AppDropdownMenu(
                label = "Tipo de Conexão",
                options = tiposConexao,
                selectedOption = tipoConexao.ifEmpty { "Selecionar" },
                onOptionSelected = { tipoConexao = it }
            )

            AppDropdownMenu(
                label = "Categoria",
                options = categorias,
                selectedOption = categoria.ifEmpty { "Selecionar" },
                onOptionSelected = { categoria = it }
            )

            if (categoria == "Outro") {
                OutlinedTextField(
                    value = categoriaOutro,
                    onValueChange = { categoriaOutro = it },
                    label = { Text("Se outro") },
                    placeholder = { Text("Citar categoria") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AppDropdownMenu(
                label = "Frequência de Contato",
                options = frequencias,
                selectedOption = frequenciaContato.ifEmpty { "Selecionar" },
                onOptionSelected = { frequenciaContato = it }
            )

            AppDropdownMenu(
                label = "Direcionamento do apoio",
                options = direcionamentos,
                selectedOption = direcionamento.ifEmpty { "Selecionar" },
                onOptionSelected = { direcionamento = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tipos de Apoio Box
            Surface(
                color = Color(0xFFFBF8FF),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tipo de Apoio:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    tiposApoio.forEach { apoio ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedApoios[apoio] = !(selectedApoios[apoio] ?: false) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(text = apoio, modifier = Modifier.weight(1f), fontSize = 14.sp)
                            Checkbox(
                                checked = selectedApoios[apoio] ?: false,
                                onCheckedChange = { selectedApoios[apoio] = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                label = { Text("Descrição") },
                placeholder = { Text("Descreva a relação.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Esse vínculo gera estresse ?", fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = geraEstresse,
                        onCheckedChange = { geraEstresse = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (geraEstresse) "Sim" else "Não", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { 
                    val network = SupportNetwork(
                        id = existingNetwork?.id ?: "",
                        patientId = patientId,
                        ecomapaId = ecomapaId,
                        name = nomeInstituicao,
                        connectionType = tipoConexao,
                        category = categoria,
                        customCategory = categoriaOutro,
                        contactFrequency = frequenciaContato,
                        supportDirection = direcionamento,
                        description = descricao,
                        supportTypes = selectedApoios.filterValues { it }.keys.toList(),
                        generatesStress = geraEstresse,
                        createdAt = existingNetwork?.createdAt ?: System.currentTimeMillis()
                    )
                    viewModel.addSupportNetwork(patientId, ecomapaId, network) {
                        Toast.makeText(context, "Rede de apoio salva com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                    onBack() // Retornar visualmente de imediato
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 8.dp)
            ) {
                Text("Salvar", fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDropdownMenu(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
