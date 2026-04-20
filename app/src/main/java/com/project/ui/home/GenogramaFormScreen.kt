package com.project.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.ui.components.TooltipIconButton

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GenogramaFormScreen(
    patientId: String,
    genogramaId: String,
    viewModel: GenogramaViewModel,
    onBack: () -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (String) -> Unit,
    onOpenView: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    
    // Deletion states
    var memberToDelete by remember { mutableStateOf<com.project.data.model.FamilyMember?>(null) }
    var unionToDelete by remember { mutableStateOf<com.project.data.model.GenogramUnion?>(null) }
    var filiationToDelete by remember { mutableStateOf<com.project.data.model.GenogramFiliation?>(null) }
    var bondToDelete by remember { mutableStateOf<com.project.data.model.EmotionalBond?>(null) }
    
    // Creation/Edition Modals states
    var showUnionDialog by remember { mutableStateOf(false) }
    var showFiliationDialog by remember { mutableStateOf(false) }
    var showEmotionalDialog by remember { mutableStateOf(false) }
    
    // IDs for editing (if null, translates to create new)
    var editingUnionId by remember { mutableStateOf<String?>(null) }
    var editingFiliationId by remember { mutableStateOf<String?>(null) }
    var editingBondId by remember { mutableStateOf<String?>(null) }

    val state by viewModel.state.collectAsState()
    LaunchedEffect(genogramaId) {
        viewModel.loadGenogramaNodesAndEdges(patientId, genogramaId)
    }

    val coroutineScope = rememberCoroutineScope()
    val tabTitles = listOf("Membros", "Parentescos", "Uniões", "Afinidades")
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Formulário - Genograma", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    TooltipIconButton(tooltipText = "Voltar", onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isMenuExpanded,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(expandFrom = Alignment.Bottom),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Button(onClick = { isMenuExpanded = false; onAddMember() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))) {
                            Text("Adicionar Familiar", color = Color.White)
                        }
                        Button(onClick = { isMenuExpanded = false; editingUnionId = null; showUnionDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))) {
                            Text("Adicionar União Marital", color = Color.White)
                        }
                        Button(onClick = { isMenuExpanded = false; editingFiliationId = null; showFiliationDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))) {
                            Text("Adicionar Filiação (Pai/Mãe)", color = Color.White)
                        }
                        Button(onClick = { isMenuExpanded = false; editingBondId = null; showEmotionalDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2577E))) {
                            Text("Adicionar Laço Emocional", color = Color.White)
                        }
                        Button(onClick = { isMenuExpanded = false; onOpenView() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))) {
                            Text("Renderizar Genograma", color = Color.White)
                        }
                        Button(onClick = { isMenuExpanded = false; onBack() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))) {
                            Text("Salvar / Voltar", color = Color.White)
                        }
                    }
                }
                FloatingActionButton(onClick = { isMenuExpanded = !isMenuExpanded }, containerColor = Color(0xFF512DA8), contentColor = Color.White) {
                    Icon(imageVector = if (isMenuExpanded) Icons.Default.Close else Icons.Default.Add, contentDescription = "Menu")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title, fontSize = 13.sp, fontWeight = if(pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (page) {
                        0 -> {
                            Text(text = "Membros Familiares Cadastrados", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (state.members.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                    Text("Nenhum membro familiar cadastrado.", fontSize = 14.sp, color = Color.Gray)
                                }
                            } else {
                                state.members.forEach { member ->
                                    val generationLvl = when(member.geracao) {
                                        -2 -> "Bisavós"; -1 -> "Avós"; 0 -> "Pais/Tios"; 1 -> "Irmãos/Ego"; 2 -> "Filhos"
                                        else -> "Geração ${member.geracao}"
                                    }
                                    val subtitle = "Geração: $generationLvl | Sexo: ${member.sexo}"
                                    FamilyMemberItem(
                                        title = member.nome.ifBlank { "Desconhecido" }, subtitle = subtitle, isEgo = member.isEgo,
                                        onEdit = { onEditMember(member.id) }, onDelete = { memberToDelete = member }, showEdit = true
                                    )
                                }
                            }
                        }
                        1 -> {
                            Text(text = "Filiações Familiares (Parentescos)", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (state.filiations.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                    Text("Sem vínculos biológicos/adotivos cadastrados.", fontSize = 14.sp, color = Color.Gray)
                                }
                            } else {
                                state.filiations.forEach { filiation ->
                                    val nomeFilho = state.members.find { it.id == filiation.filhoId }?.nome ?: "Desconhecido"
                                    val nomePai = state.members.find { it.id == filiation.paiId }?.nome ?: "?"
                                    val nomeMae = state.members.find { it.id == filiation.maeId }?.nome ?: "?"
                                    FamilyMemberItem(
                                        title = "Filho(a): $nomeFilho", subtitle = "Descende de: $nomePai e $nomeMae | Tipo: ${filiation.tipo}",
                                        isEgo = false,
                                        onEdit = { editingFiliationId = filiation.id; showFiliationDialog = true }, 
                                        onDelete = { filiationToDelete = filiation }, showEdit = true
                                    )
                                }
                            }
                        }
                        2 -> {
                            Text(text = "Casamentos e Uniões Vinculadas", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (state.unions.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                    Text("Sem uniões cadastradas.", fontSize = 14.sp, color = Color.Gray)
                                }
                            } else {
                                state.unions.forEach { union ->
                                    val nomeA = state.members.find { it.id == union.membroA }?.nome ?: "Desconhecido"
                                    val nomeB = state.members.find { it.id == union.membroB }?.nome ?: "Desconhecido"
                                    FamilyMemberItem(
                                        title = "$nomeA ↔ $nomeB", subtitle = "Tipo: ${union.tipo} | Status: ${union.status}",
                                        isEgo = false,
                                        onEdit = { editingUnionId = union.id; showUnionDialog = true }, 
                                        onDelete = { unionToDelete = union }, showEdit = true
                                    )
                                }
                            }
                        }
                        3 -> {
                            Text(text = "Laços Emocionais (Afinidades)", fontSize = 16.sp, fontWeight = FontWeight.Normal)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (state.emotionalBonds.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                    Text("Sem vínculos emocionais sistêmicos.", fontSize = 14.sp, color = Color.Gray)
                                }
                            } else {
                                state.emotionalBonds.forEach { bond ->
                                    val nomeA = state.members.find { it.id == bond.membroAId }?.nome ?: "Desconhecido"
                                    val nomeB = state.members.find { it.id == bond.membroBId }?.nome ?: "Desconhecido"
                                    FamilyMemberItem(
                                        title = "$nomeA ↔ $nomeB", subtitle = "Aspecto: ${bond.tipo}",
                                        isEgo = false,
                                        onEdit = { editingBondId = bond.id; showEmotionalDialog = true }, 
                                        onDelete = { bondToDelete = bond }, showEdit = true
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // MODALS DE CONFIRM DE EXCLUSAO
            if (memberToDelete != null) {
                AlertDialog(onDismissRequest = { memberToDelete = null }, title = { Text("Excluir Familiar") }, text = { Text("Deseja remover '${memberToDelete!!.nome}'?") },
                    confirmButton = { Button(onClick = { viewModel.deleteMember(patientId, genogramaId, memberToDelete!!.id); memberToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) { Text("Excluir") } },
                    dismissButton = { Button(onClick = { memberToDelete = null }) { Text("Cancelar") } }
                )
            }
            if (unionToDelete != null) {
                AlertDialog(onDismissRequest = { unionToDelete = null }, title = { Text("Excluir União") }, text = { Text("Deseja deletar este casamento/união?") },
                    confirmButton = { Button(onClick = { viewModel.deleteUnion(patientId, genogramaId, unionToDelete!!.id); unionToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) { Text("Excluir") } },
                    dismissButton = { Button(onClick = { unionToDelete = null }) { Text("Cancelar") } }
                )
            }
            if (filiationToDelete != null) {
                AlertDialog(onDismissRequest = { filiationToDelete = null }, title = { Text("Excluir Filiação") }, text = { Text("Deseja deletar o registro de parentesco biológico?") },
                    confirmButton = { Button(onClick = { viewModel.deleteFiliation(patientId, genogramaId, filiationToDelete!!.id); filiationToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) { Text("Excluir") } },
                    dismissButton = { Button(onClick = { filiationToDelete = null }) { Text("Cancelar") } }
                )
            }
            if (bondToDelete != null) {
                AlertDialog(onDismissRequest = { bondToDelete = null }, title = { Text("Excluir Vínculo") }, text = { Text("Deseja deletar a afinidade psicológica?") },
                    confirmButton = { Button(onClick = { viewModel.deleteEmotionalBond(patientId, genogramaId, bondToDelete!!.id); bondToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) { Text("Excluir") } },
                    dismissButton = { Button(onClick = { bondToDelete = null }) { Text("Cancelar") } }
                )
            }

            // MODALS DE CRIACAO / EDICAO
            if (showUnionDialog) {
                SharedUnionDialog(
                    members = state.members,
                    initialUnion = if(editingUnionId != null) state.unions.find { it.id == editingUnionId } else null,
                    prefilledMemberA = null,
                    onDismiss = { showUnionDialog = false; editingUnionId = null },
                    onSave = { newUnion -> 
                        viewModel.saveUnion(patientId, genogramaId, newUnion)
                        showUnionDialog = false
                        editingUnionId = null
                    },
                    genogramaId = genogramaId,
                    patientId = patientId
                )
            }

            if (showFiliationDialog) {
                SharedFiliationDialog(
                    members = state.members,
                    initialFiliation = if(editingFiliationId != null) state.filiations.find { it.id == editingFiliationId } else null,
                    onDismiss = { showFiliationDialog = false; editingFiliationId = null },
                    onSave = { newFiliation -> 
                        viewModel.saveFiliation(patientId, genogramaId, newFiliation)
                        showFiliationDialog = false
                        editingFiliationId = null
                    },
                    genogramaId = genogramaId,
                    patientId = patientId
                )
            }

            if (showEmotionalDialog) {
                SharedEmotionalDialog(
                    members = state.members,
                    initialBond = if(editingBondId != null) state.emotionalBonds.find { it.id == editingBondId } else null,
                    onDismiss = { showEmotionalDialog = false; editingBondId = null },
                    onSave = { newBond -> 
                        viewModel.saveEmotionalBond(patientId, genogramaId, newBond)
                        showEmotionalDialog = false
                        editingBondId = null
                    },
                    genogramaId = genogramaId,
                    patientId = patientId
                )
            }
        }
    }
}
