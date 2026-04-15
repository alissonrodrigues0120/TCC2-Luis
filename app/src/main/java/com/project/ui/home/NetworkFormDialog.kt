package com.project.ui.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.project.data.model.SupportNetwork

@Composable
fun NetworkOptionsDialog(
    network: SupportNetwork,
    onDismiss: () -> Unit,
    onDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Opções: ${network.name}", fontWeight = FontWeight.Bold) },
        text = {
            Text("O que deseja fazer com esta rede de apoio?")
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDetails) {
                    Text("Detalhes", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onEdit) {
                    Text("Editar", color = Color(0xFF512DA8), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Excluir", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkFormDialog(
    patientId: String,
    ecomapaId: String,
    existingNetwork: SupportNetwork?,
    onDismiss: () -> Unit,
    onSave: (SupportNetwork) -> Unit
) {
    val context = LocalContext.current.applicationContext
    
    var nomeInstituicao by remember { mutableStateOf(existingNetwork?.name ?: "") }
    
    var tipoConexao by remember { mutableStateOf(existingNetwork?.connectionType ?: "") }
    val tiposConexao = listOf("Selecionar", "Forte", "Fraca", "Conflituosa", "Neutra", "Compensatória")
    
    var categoria by remember { mutableStateOf(existingNetwork?.category ?: "") }
    val categorias = listOf("Selecionar", "Família", "Trabalho", "Religioso", "Comunidade", "Saúde", "Outro")
    
    var categoriaOutro by remember { mutableStateOf(existingNetwork?.customCategory ?: "") }
    
    var frequenciaContato by remember { mutableStateOf(existingNetwork?.contactFrequency ?: "") }
    val frequencias = listOf("Selecionar", "Diário", "Semanal", "Mensal", "Raro")
    
    var direcionamento by remember { mutableStateOf(existingNetwork?.supportDirection ?: "") }
    val direcionamentos = listOf("Selecionar", "Recebe Apoio", "Oferece Apoio", "Mútuo")

    var descricao by remember { mutableStateOf(existingNetwork?.description ?: "") }
    
    val tiposApoio = listOf("Emocional", "Financeiro", "Cuidado Físico", "Social", "Institucional", "Outro")
    val selectedApoios = remember { 
        mutableStateMapOf<String, Boolean>().apply {
            tiposApoio.forEach { 
                put(it, existingNetwork?.supportTypes?.contains(it) == true) 
            } 
        } 
    }

    var geraEstresse by remember { mutableStateOf(existingNetwork?.generatesStress ?: false) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp), // Leaves some space at top to show it's a floating dialog
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingNetwork == null) "Nova Rede de Apoio" else "Editar Rede",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Text("X", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
                
                HorizontalDivider()

                // Form Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7E57C2))
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
                                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF7E57C2))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (geraEstresse) "Sim" else "Não", fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (nomeInstituicao.isBlank() || tipoConexao == "Selecionar" || categoria == "Selecionar") {
                                Toast.makeText(context, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
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
                            onSave(network)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        Text("Salvar Alteraçōes", fontSize = 16.sp, color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
