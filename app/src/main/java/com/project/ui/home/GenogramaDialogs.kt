package com.project.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.data.model.EmotionalBond
import com.project.data.model.FamilyMember
import com.project.data.model.GenogramFiliation
import com.project.data.model.GenogramUnion
import java.util.UUID
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.project.ui.components.TooltipIconButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


@Composable
fun SharedUnionDialog(
    members: List<FamilyMember>,
    initialUnion: GenogramUnion?,
    prefilledMemberA: String? = null,
    onDismiss: () -> Unit,
    onSave: (GenogramUnion) -> Unit,
    genogramaId: String,
    patientId: String
) {
    var selectedA by remember { mutableStateOf<String?>(initialUnion?.membroA ?: prefilledMemberA) }
    var selectedB by remember { mutableStateOf<String?>(initialUnion?.membroB) }
    var unionType by remember { mutableStateOf(initialUnion?.tipo ?: "Casamento") }
    var unionStatus by remember { mutableStateOf(initialUnion?.status ?: "Ativo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialUnion != null) "Editar União" else "Registrar União") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Cônjuge A:", fontSize = 12.sp)
                var expandedA by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedA = true }, modifier = Modifier.fillMaxWidth()) { 
                        Text(members.find { it.id == selectedA }?.nome ?: "Selecionar Membro...") 
                    }
                    DropdownMenu(expanded = expandedA, onDismissRequest = { expandedA = false }) { 
                        members.filter { it.id != selectedB }.forEach { m -> 
                            DropdownMenuItem(text = { Text(m.nome) }, onClick = { selectedA = m.id; expandedA = false }) 
                        } 
                    }
                }
                
                Text("Cônjuge B:", fontSize = 12.sp)
                var expandedB by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedB = true }, modifier = Modifier.fillMaxWidth()) { 
                        Text(members.find { it.id == selectedB }?.nome ?: "Selecionar Parceiro...") 
                    }
                    DropdownMenu(expanded = expandedB, onDismissRequest = { expandedB = false }) { 
                        members.filter { it.id != selectedA }.forEach { m -> 
                            DropdownMenuItem(text = { Text(m.nome) }, onClick = { selectedB = m.id; expandedB = false }) 
                        } 
                    }
                }
                
                Text("Tipo de Vínculo:", fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Column { 
                    listOf("Casamento", "União Estável", "Namoro").forEach { type -> 
                        Row(verticalAlignment = Alignment.CenterVertically) { 
                            RadioButton(selected = unionType == type, onClick = { unionType = type })
                            Text(type, fontSize = 12.sp) 
                        } 
                    } 
                }
                
                Text("Status Atual:", fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Column { 
                    listOf("Ativo", "Separado", "Divorciado").forEach { status -> 
                        Row(verticalAlignment = Alignment.CenterVertically) { 
                            RadioButton(selected = unionStatus == status, onClick = { unionStatus = status })
                            Text(status, fontSize = 12.sp) 
                        } 
                    } 
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedA != null && selectedB != null && selectedA != selectedB) {
                    val newUnion = GenogramUnion(
                        id = initialUnion?.id ?: UUID.randomUUID().toString(),
                        genogramaId = genogramaId, 
                        patientId = patientId, 
                        tipo = unionType, 
                        status = unionStatus, 
                        membroA = selectedA!!, 
                        membroB = selectedB!!
                    )
                    onSave(newUnion)
                }
            }) { Text(if (initialUnion != null) "Atualizar" else "Salvar União") }
        }, 
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun SharedFiliationDialog(
    members: List<FamilyMember>,
    initialFiliation: GenogramFiliation?,
    prefilledParent: String? = null,
    onDismiss: () -> Unit,
    onSave: (GenogramFiliation) -> Unit,
    genogramaId: String,
    patientId: String
) {
    var selectedFilho by remember { mutableStateOf<String?>(initialFiliation?.filhoId) }
    var selectedPai by remember { mutableStateOf<String?>(initialFiliation?.paiId?.ifEmpty { prefilledParent }) }
    var selectedMae by remember { mutableStateOf<String?>(initialFiliation?.maeId?.ifEmpty { prefilledParent }) }
    var filiationType by remember { mutableStateOf(initialFiliation?.tipo ?: "Biológico") }
    var isHomoaffective by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text(if (initialFiliation != null) "Editar Parentesco" else "Registrar Parentesco/Filiação") }, 
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = isHomoaffective, onCheckedChange = { isHomoaffective = it })
                    Text("Configuração Homoafetiva", fontSize = 12.sp)
                }

                Text("Filho(a):", fontSize = 12.sp)
                var expandedFilho by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedFilho = true }, modifier = Modifier.fillMaxWidth()) { 
                        Text(members.find { it.id == selectedFilho }?.nome ?: "Selecionar Criança/Adulto...") 
                    }
                    DropdownMenu(expanded = expandedFilho, onDismissRequest = { expandedFilho = false }) { 
                        members.filter { it.id != selectedPai && it.id != selectedMae }.forEach { m -> 
                            DropdownMenuItem(text = { Text(m.nome) }, onClick = { selectedFilho = m.id; expandedFilho = false }) 
                        } 
                    }
                }
                
                val gen1Label = if (isHomoaffective) "Responsável A (Opcional):" else "Genitor A (Opcional):"
                Text(gen1Label, fontSize = 12.sp)
                var expandedPai by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedPai = true }, modifier = Modifier.fillMaxWidth()) { 
                        Text(members.find { it.id == selectedPai }?.nome ?: "Nenhum / Desconhecido") 
                    }
                    DropdownMenu(expanded = expandedPai, onDismissRequest = { expandedPai = false }) {
                        DropdownMenuItem(text = { Text("Nenhum / Desconhecido") }, onClick = { selectedPai = null; expandedPai = false })
                        members.filter { it.id != selectedFilho && it.id != selectedMae }.forEach { m -> 
                            DropdownMenuItem(text = { Text(m.nome) }, onClick = { selectedPai = m.id; expandedPai = false }) 
                        }
                    }
                }
                
                val gen2Label = if (isHomoaffective) "Responsável B (Opcional):" else "Genitor B (Opcional):"
                Text(gen2Label, fontSize = 12.sp)
                var expandedMae by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedMae = true }, modifier = Modifier.fillMaxWidth()) { 
                        Text(members.find { it.id == selectedMae }?.nome ?: "Nenhum / Desconhecido") 
                    }
                    DropdownMenu(expanded = expandedMae, onDismissRequest = { expandedMae = false }) {
                        DropdownMenuItem(text = { Text("Nenhum / Desconhecido") }, onClick = { selectedMae = null; expandedMae = false })
                        members.filter { it.id != selectedFilho && it.id != selectedPai }.forEach { m -> 
                            DropdownMenuItem(text = { Text(m.nome) }, onClick = { selectedMae = m.id; expandedMae = false }) 
                        }
                    }
                }
                
                Text("Natureza do Vinculo:", fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Column { 
                    listOf("Biológico", "Adotivo").forEach { type -> 
                        Row(verticalAlignment = Alignment.CenterVertically) { 
                            RadioButton(selected = filiationType == type, onClick = { filiationType = type })
                            Text(type, fontSize = 12.sp) 
                        } 
                    } 
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedFilho != null) {
                    val newFiliation = GenogramFiliation(
                        id = initialFiliation?.id ?: UUID.randomUUID().toString(),
                        genogramaId = genogramaId, 
                        patientId = patientId, 
                        filhoId = selectedFilho!!, 
                        paiId = selectedPai ?: "", 
                        maeId = selectedMae ?: "", 
                        tipo = filiationType, 
                        gemelar = "nenhum"
                    )
                    onSave(newFiliation)
                }
            }) { Text(if (initialFiliation != null) "Atualizar" else "Salvar Parentesco") }
        }, 
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun SharedEmotionalDialog(
    members: List<FamilyMember>,
    initialBond: EmotionalBond?,
    prefilledMemberA: String? = null,
    onDismiss: () -> Unit,
    onSave: (EmotionalBond) -> Unit,
    genogramaId: String,
    patientId: String
) {
    var selectedA by remember { mutableStateOf<String?>(initialBond?.membroAId ?: prefilledMemberA) }
    var selectedB by remember { mutableStateOf<String?>(initialBond?.membroBId) }
    var bondType by remember { mutableStateOf(initialBond?.tipo ?: "Próximo") }

    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text(if (initialBond != null) "Editar Traço Emocional" else "Traço Emocional e Psicossocial") }, 
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Membro A:", fontSize = 12.sp)
                var expandedA by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedA = true }, modifier = Modifier.fillMaxWidth()) { 
                        Text(members.find { it.id == selectedA }?.nome ?: "Selecionar Membro...") 
                    }
                    DropdownMenu(expanded = expandedA, onDismissRequest = { expandedA = false }) { 
                        members.filter { it.id != selectedB }.forEach { m -> 
                            DropdownMenuItem(text = { Text(m.nome) }, onClick = { selectedA = m.id; expandedA = false }) 
                        } 
                    }
                }
                
                Text("Membro B:", fontSize = 12.sp)
                var expandedB by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedB = true }, modifier = Modifier.fillMaxWidth()) { 
                        Text(members.find { it.id == selectedB }?.nome ?: "Selecionar Membro...") 
                    }
                    DropdownMenu(expanded = expandedB, onDismissRequest = { expandedB = false }) { 
                        members.filter { it.id != selectedA }.forEach { m -> 
                            DropdownMenuItem(text = { Text(m.nome) }, onClick = { selectedB = m.id; expandedB = false }) 
                        } 
                    }
                }
                
                Text("Escopo do Relacionamento:", fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                val bondedTypes = listOf("Muito Próximo", "Próximo", "Distante", "Conflituoso", "Rompido")
                bondedTypes.forEach { type -> 
                    Row(verticalAlignment = Alignment.CenterVertically) { 
                        RadioButton(selected = bondType == type, onClick = { bondType = type })
                        Text(type, fontSize = 12.sp) 
                    } 
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedA != null && selectedB != null && selectedA != selectedB) {
                    val newBond = EmotionalBond(
                        id = initialBond?.id ?: UUID.randomUUID().toString(),
                        genogramaId = genogramaId, 
                        patientId = patientId, 
                        tipo = bondType, 
                        membroAId = selectedA!!, 
                        membroBId = selectedB!!
                    )
                    onSave(newBond)
                }
            }) { Text(if(initialBond != null) "Atualizar" else "Gravar Laço") }
        }, 
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun FamilyMemberItem(
    title: String, 
    subtitle: String,
    isEgo: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showEdit: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = if(isEgo) androidx.compose.ui.graphics.Color(0xFFE8EAF6) else androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Person, 
                    contentDescription = null, 
                    modifier = Modifier.size(36.dp), 
                    tint = if(isEgo) androidx.compose.ui.graphics.Color(0xFF3F51B5) else androidx.compose.ui.graphics.Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if(isEgo) "$title (Paciente Cêntrico)" else title, 
                fontSize = 16.sp, 
                color = MaterialTheme.colorScheme.onSurface, 
                fontWeight = if(isEgo) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
            )
            Text(
                text = subtitle, 
                fontSize = 12.sp, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Row {
            if (showEdit) {
                TooltipIconButton(tooltipText = "Editar", onClick = onEdit) { 
                    Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Editar", tint = androidx.compose.ui.graphics.Color.Black) 
                }
            }
            TooltipIconButton(tooltipText = "Excluir", onClick = onDelete) { 
                Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Excluir", tint = androidx.compose.ui.graphics.Color.Black) 
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddMemberDialog(
    onDismiss: () -> Unit,
    onSave: (FamilyMember) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("M") }
    var vivo by remember { mutableStateOf(true) }
    var nascimento by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criação Rápida de Membro") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Membro") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Column {
                    Text("Gênero", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = sexo == "M", onClick = { sexo = "M" })
                        Text("M")
                        Spacer(modifier = Modifier.width(4.dp))
                        RadioButton(selected = sexo == "F", onClick = { sexo = "F" })
                        Text("F")
                        Spacer(modifier = Modifier.width(4.dp))
                        RadioButton(selected = sexo == "Outro", onClick = { sexo = "Outro" })
                        Text("Outros")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vivo, onCheckedChange = { vivo = it })
                    Text("Membro está vivo")
                }

                OutlinedTextField(
                    value = nascimento,
                    onValueChange = { nascimento = it },
                    label = { Text("Ano de Nasc. (Ex: 1990)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nome.isNotBlank()) {
                        val novoMembro = FamilyMember(
                            nome = nome,
                            sexo = sexo,
                            vivo = vivo,
                            nascimento = nascimento
                        )
                        onSave(novoMembro)
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
