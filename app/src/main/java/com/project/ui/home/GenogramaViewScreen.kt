package com.project.ui.home

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.data.model.FamilyMember
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenogramaViewScreen(
    patientId: String,
    genogramaId: String,
    viewModel: GenogramaViewModel,
    onBack: () -> Unit,
    onEditMember: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(genogramaId) {
        viewModel.loadGenogramaNodesAndEdges(patientId, genogramaId)
    }
    
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    
    var selectedMember by remember { mutableStateOf<FamilyMember?>(null) }
    
    // States para o SimulEdit
    var showUnionDialog by remember { mutableStateOf(false) }
    var editingUnionId by remember { mutableStateOf<String?>(null) }
    var showFiliationDialog by remember { mutableStateOf(false) }
    var editingFiliationId by remember { mutableStateOf<String?>(null) }
    var showEmotionalDialog by remember { mutableStateOf(false) }
    var editingBondId by remember { mutableStateOf<String?>(null) }
    
    // Parametros Fisicos
    val verticalSpacing = 280f
    val horizontalSpacing = 220f
    val nodePositions = remember { mutableStateMapOf<String, Offset>() }
    val shapeSize = 80f
    val halfShape = shapeSize / 2f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visualizador", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val bitmap = generateGenogramaBitmap(
                            members = state.members,
                            unions = state.unions,
                            filiations = state.filiations,
                            emotionalBonds = state.emotionalBonds,
                            nodePositions = nodePositions,
                            shapeSize = shapeSize
                        )
                        saveGenogramaToGallery(context, bitmap, "Paciente_$patientId")
                    }) {
                        Text("Baixar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF512DA8), modifier = Modifier.padding(end = 16.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFAFAFA))
        ) {
            val density = LocalDensity.current
            val canvasCenter = remember(maxWidth, maxHeight) {
                with(density) { Offset(maxWidth.toPx() / 2f, maxHeight.toPx() / 2f) }
            }

            // Motor de Matriz Baseado no Ego Geografico (Graph Traversal Algorithm)
            val localMembers = state.members
            val localFiliations = state.filiations

            LaunchedEffect(localMembers, localFiliations, canvasCenter) {
                nodePositions.clear()
                
                val ego = localMembers.find { it.isEgo }
                if (ego == null) {
                    // Fail-safe caso não exista ego (Nao deveria ocorrer)
                    localMembers.forEachIndexed { i, m -> nodePositions[m.id] = Offset(canvasCenter.x + (i*100f), canvasCenter.y) }
                    return@LaunchedEffect
                }

                val processed = mutableSetOf<String>()
                val logicalGrid = mutableMapOf<String, Pair<Float, Float>>()
                
                logicalGrid[ego.id] = Pair(0f, 0f)
                processed.add(ego.id)

                fun getBirthYear(m: FamilyMember): Int {
                    val y = m.nascimento.takeLast(4).filter { it.isDigit() }
                    return y.toIntOrNull() ?: 9999
                }

                // SUBIDA (Antepassados - Fase A)
                val queueUp = ArrayDeque<String>()
                queueUp.add(ego.id)

                while(queueUp.isNotEmpty()) {
                    val childId = queueUp.removeFirst()
                    val childPos = logicalGrid[childId] ?: continue
                    
                    val filiation = localFiliations.find { it.filhoId == childId }
                    if (filiation != null) {
                        if (filiation.paiId.isNotEmpty() && !processed.contains(filiation.paiId)) {
                            // Pais de quem ta no centro (Ego) ganham spread maior, avos ganham spread menor pra caber.
                            val spread = se(childPos.second == 0f, 1.2f, 0.6f)
                            logicalGrid[filiation.paiId] = Pair(childPos.first - spread, childPos.second - 1f)
                            processed.add(filiation.paiId)
                            queueUp.add(filiation.paiId)
                        }
                        if (filiation.maeId.isNotEmpty() && !processed.contains(filiation.maeId)) {
                            val spread = se(childPos.second == 0f, 1.2f, 0.6f)
                            logicalGrid[filiation.maeId] = Pair(childPos.first + spread, childPos.second - 1f)
                            processed.add(filiation.maeId)
                            queueUp.add(filiation.maeId)
                        }
                    }
                }

                // MEIO (Irmaos do Ego - Fase B)
                val egoFiliation = localFiliations.find { it.filhoId == ego.id }
                if (egoFiliation != null && (egoFiliation.paiId.isNotEmpty() || egoFiliation.maeId.isNotEmpty())) {
                    val siblingsIds = localFiliations.filter { 
                        it.filhoId != ego.id && 
                        (it.paiId == egoFiliation.paiId || it.maeId == egoFiliation.maeId) 
                    }.map { it.filhoId }
                    
                    val sortedSiblings = localMembers.filter { it.id in siblingsIds }.sortedBy { getBirthYear(it) }
                    val older = sortedSiblings.filter { getBirthYear(it) <= getBirthYear(ego) }
                    val younger = sortedSiblings.filter { getBirthYear(it) > getBirthYear(ego) }
                    
                    older.forEachIndexed { i, sib ->
                        logicalGrid[sib.id] = Pair(-1.2f * (older.size - i), 0f)
                        processed.add(sib.id)
                    }
                    younger.forEachIndexed { i, sib ->
                        logicalGrid[sib.id] = Pair(1.2f * (i + 1), 0f)
                        processed.add(sib.id)
                    }
                }

                // DESCIDA (Filhos do Ego - Fase C)
                val egoDescendants = localFiliations.filter { it.paiId == ego.id || it.maeId == ego.id }
                val sortedChildren = localMembers.filter { m -> egoDescendants.any { it.filhoId == m.id } }.sortedBy { getBirthYear(it) }
                val chStartX = -((sortedChildren.size - 1) * 1.0f / 2f)
                sortedChildren.forEachIndexed { i, child ->
                    if (!processed.contains(child.id)) {
                        logicalGrid[child.id] = Pair(chStartX + (i * 1.2f), 1f)
                        processed.add(child.id)
                        // Recursividade simples para os netos
                        val grandChildrenDesc = localFiliations.filter { it.paiId == child.id || it.maeId == child.id }
                        val sortedGrand = localMembers.filter { m -> grandChildrenDesc.any { it.filhoId == m.id } }.sortedBy { getBirthYear(it) }
                        val gStartX = (chStartX + (i * 1.2f)) - ((sortedGrand.size - 1) * 0.8f / 2f)
                        sortedGrand.forEachIndexed { gi, gchild ->
                             if(!processed.contains(gchild.id)) {
                                 logicalGrid[gchild.id] = Pair(gStartX + (gi * 0.8f), 2f)
                                 processed.add(gchild.id)
                             }
                        }
                    }
                }

                // SOBRAS (Orfãos Livres - Fase D)
                val orphans = localMembers.filter { !processed.contains(it.id) }
                orphans.forEachIndexed { i, orphan ->
                    logicalGrid[orphan.id] = Pair(-4f + (i * 1.5f), -3f) // Canto Esquerdo Superior
                }

                // Mapeamento Cartesian (Tradução Grade -> Display Float)
                logicalGrid.forEach { (id, gridPos) ->
                    val x = canvasCenter.x + (gridPos.first * horizontalSpacing)
                    val y = canvasCenter.y + (gridPos.second * verticalSpacing)
                    nodePositions[id] = Offset(x, y)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                            scale = (scale * gestureZoom).coerceIn(0.2f, 5f)
                            pan += gesturePan
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = pan.x, translationY = pan.y)
                ) {
                    // 1. Linhas de Conexão no Fundo
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Linhas Maritais (Casamentos/Uniões)
                        state.unions.forEach { union ->
                            val posA = nodePositions[union.membroA]
                            val posB = nodePositions[union.membroB]
                            if (posA != null && posB != null) {
                                val corVnculo = if(union.tipo == "Casamento") Color.Black else Color.DarkGray
                                val style = if(union.tipo == "União Estável") androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)) else androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                                
                                drawLine(
                                    color = corVnculo,
                                    start = posA,
                                    end = posB,
                                    strokeWidth = 4f,
                                    pathEffect = if(union.tipo == "União Estável" || union.tipo == "Namoro") androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f) else null
                                )
                                
                                // Rupturas Clinicas (Separado / Divorciado)
                                if (union.status == "Separado" || union.status == "Divorciado") {
                                    val midX = (posA.x + posB.x) / 2f
                                    val midY = (posA.y + posB.y) / 2f
                                    // Separado (1 barra)
                                    drawLine(color = Color.Red, start = Offset(midX - 10f, midY - 20f), end = Offset(midX + 10f, midY + 20f), strokeWidth = 5f)
                                    // Divorciado (2 barras)
                                    if (union.status == "Divorciado") {
                                        drawLine(color = Color.Red, start = Offset(midX - 25f, midY - 20f), end = Offset(midX - 5f, midY + 20f), strokeWidth = 5f)
                                    }
                                }
                            }
                        }
                        
                        // Linhas Biológicas (Filiações)
                        state.filiations.forEach { fil ->
                            val childPos = nodePositions[fil.filhoId]
                            if (childPos != null) {
                                val pA = nodePositions[fil.paiId]
                                val pB = nodePositions[fil.maeId]
                                val dashFx = if(fil.tipo == "Adotivo") androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f) else null
                                val clr = if(fil.tipo == "Adotivo") Color.Blue else Color.Black
                                
                                if (pA != null && pB != null) {
                                    val midX = (pA.x + pB.x) / 2f
                                    val midY = (pA.y + pB.y) / 2f
                                    val dropY = childPos.y - 120f
                                    
                                    drawLine(color = clr, start = Offset(midX, midY), end = Offset(midX, dropY), strokeWidth = 3f, pathEffect = dashFx)
                                    drawLine(color = clr, start = Offset(midX, dropY), end = Offset(childPos.x, dropY), strokeWidth = 3f, pathEffect = dashFx)
                                    drawLine(color = clr, start = Offset(childPos.x, dropY), end = childPos, strokeWidth = 3f, pathEffect = dashFx)
                                } else if (pA != null) {
                                    drawLine(color = clr, start = pA, end = childPos, strokeWidth = 3f, pathEffect = dashFx)
                                } else if (pB != null) {
                                    drawLine(color = clr, start = pB, end = childPos, strokeWidth = 3f, pathEffect = dashFx)
                                }
                            }
                        }
                        
                        // 3. Laços Emocionais (Curvas Oculares)
                        state.emotionalBonds.forEach { bond ->
                            val pA = nodePositions[bond.membroAId]
                            val pB = nodePositions[bond.membroBId]
                            if (pA != null && pB != null) {
                                drawEmotionalBondCurve(pA, pB, bond.tipo)
                            }
                        }
                    }
                    
                    // 2. Desenho das Famílias Inteiramente Clicável
                    state.members.forEach { member ->
                        val center = nodePositions[member.id]
                        if (center != null) {
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((center.x - halfShape).roundToInt(), (center.y - halfShape).roundToInt()) }
                                    .size(with(density) { shapeSize.toDp() })
                                    .clickable { selectedMember = member }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val locCenter = Offset(halfShape, halfShape)
                                    when(member.sexo) {
                                        "M" -> {
                                            drawRect(color = Color.Black, topLeft = Offset(0f, 0f), size = Size(shapeSize, shapeSize), style = Stroke(width = if(member.isEgo) 10f else 5f))
                                            if(member.isEgo) {
                                                drawRect(color = Color.Black, topLeft = Offset(15f, 15f), size = Size(shapeSize - 30f, shapeSize - 30f), style = Stroke(width = 3f))
                                            }
                                        }
                                        "F" -> {
                                            drawCircle(color = Color.Black, radius = halfShape, center = locCenter, style = Stroke(width = if(member.isEgo) 10f else 5f))
                                            if(member.isEgo) {
                                                drawCircle(color = Color.Black, radius = halfShape - 15f, center = locCenter, style = Stroke(width = 3f))
                                            }
                                        }
                                        else -> {
                                            val path = Path().apply {
                                                moveTo(locCenter.x, 0f)
                                                lineTo(shapeSize, locCenter.y)
                                                lineTo(locCenter.x, shapeSize)
                                                lineTo(0f, locCenter.y)
                                                close()
                                            }
                                            drawPath(path = path, color = Color.Black, style = Stroke(width = if(member.isEgo) 10f else 5f))
                                            if(member.isEgo) {
                                                val innerPath = Path().apply {
                                                    moveTo(locCenter.x, 18f)
                                                    lineTo(shapeSize - 18f, locCenter.y)
                                                    lineTo(locCenter.x, shapeSize - 18f)
                                                    lineTo(18f, locCenter.y)
                                                    close()
                                                }
                                                drawPath(innerPath, color = Color.Black, style = Stroke(width = 3f))
                                            }
                                        }
                                    }
                                    
                                    if(!member.vivo) {
                                        drawLine(color = Color.Black, start = Offset(0f, 0f), end = Offset(shapeSize, shapeSize), strokeWidth = 6f)
                                        drawLine(color = Color.Black, start = Offset(shapeSize, 0f), end = Offset(0f, shapeSize), strokeWidth = 6f)
                                    }
                                }
                            }
                            
                            // Letreiros por fora do Box clicável 
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((center.x - 100f).roundToInt(), (center.y + halfShape + 10f).roundToInt()) }
                                    .width(with(density) { 200f.toDp() }),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = member.nome.take(15), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    val birthText = if(member.nascimento.isNotEmpty()) member.nascimento.takeLast(4) else ""
                                    var meta = birthText
                                    if(!member.vivo && member.falecimento.isNotEmpty()) meta = "$birthText - ${member.falecimento.takeLast(4)}"
                                    if(meta.isNotEmpty()) Text(text = meta, color = Color.DarkGray, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // LEGENDA FLUTUANTE
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .width(170.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Simbologia:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("🔲: Masculino", fontSize = 12.sp)
                    Text("◯: Feminino", fontSize = 12.sp)
                    Text("⬦: Não-Binário", fontSize = 12.sp)
                    Text("❌: Falecimento", fontSize = 12.sp)
                    Text("◎: Paciente Central", fontSize = 12.sp)
                }
            }
        }

        // DIALOGO INTERATIVO DE PARENTE (ModalBottomSheet SimulEdit)
        if (selectedMember != null) {
            val sm = selectedMember!!
            ModalBottomSheet(
                onDismissRequest = { selectedMember = null },
                modifier = Modifier.fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    Text("Interação com: ${sm.nome}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
                    Text("Geração: ${sm.geracao} | Ocupação: ${sm.ocupacao.ifEmpty { "N/A" }} | " + if(sm.vivo) "Vivo" else "In Memorian", fontSize = 14.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onEditMember(sm.id); selectedMember = null }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar Perfil Básico (Nome, Idade, Morte)")
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    // UNIÕES DELE
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Casamentos & Uniões", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { editingUnionId = null; showUnionDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF3F51B5)) }
                    }
                    state.unions.filter { it.membroA == sm.id || it.membroB == sm.id }.forEach { un ->
                        val pt = if(un.membroA == sm.id) state.members.find{ it.id == un.membroB }?.nome else state.members.find{ it.id == un.membroA }?.nome
                        FamilyMemberItem(
                            title = "Com: ${pt ?: "Desconhecido"}", subtitle = "${un.tipo} (${un.status})", isEgo = false,
                            onEdit = { editingUnionId = un.id; showUnionDialog = true },
                            onDelete = { viewModel.deleteUnion(patientId, genogramaId, un.id) }, showEdit = true
                        )
                    }

                    // FILIAÇÕES DELE
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Relações Familiares (Pai/Mãe/Filho)", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { editingFiliationId = null; showFiliationDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF3F51B5)) }
                    }
                    state.filiations.filter { it.filhoId == sm.id || it.paiId == sm.id || it.maeId == sm.id }.forEach { fil ->
                        val role = if(fil.filhoId == sm.id) "É Filho de: ${state.members.find{ it.id == fil.paiId }?.nome} e ${state.members.find{ it.id == fil.maeId }?.nome}" else "É Pai/Mãe de: ${state.members.find{ it.id == fil.filhoId }?.nome}"
                        FamilyMemberItem(
                            title = role, subtitle = fil.tipo, isEgo = false,
                            onEdit = { editingFiliationId = fil.id; showFiliationDialog = true },
                            onDelete = { viewModel.deleteFiliation(patientId, genogramaId, fil.id) }, showEdit = true
                        )
                    }

                    // LAÇOS EMOCIONAIS DELE
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Traços Psicossociais", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { editingBondId = null; showEmotionalDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF3F51B5)) }
                    }
                    state.emotionalBonds.filter { it.membroAId == sm.id || it.membroBId == sm.id }.forEach { bond ->
                        val target = if(bond.membroAId == sm.id) state.members.find{ it.id == bond.membroBId }?.nome else state.members.find{ it.id == bond.membroAId }?.nome
                        FamilyMemberItem(
                            title = "Com: ${target ?: "Desconhecido"}", subtitle = bond.tipo, isEgo = false,
                            onEdit = { editingBondId = bond.id; showEmotionalDialog = true },
                            onDelete = { viewModel.deleteEmotionalBond(patientId, genogramaId, bond.id) }, showEdit = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Dialogs do SimulEdit (só sobrepõem o resto)
        if (showUnionDialog) {
            SharedUnionDialog(
                members = state.members,
                initialUnion = if(editingUnionId != null) state.unions.find { it.id == editingUnionId } else null,
                prefilledMemberA = if(editingUnionId == null) selectedMember?.id else null,
                onDismiss = { showUnionDialog = false; editingUnionId = null },
                onSave = { newUnion -> viewModel.saveUnion(patientId, genogramaId, newUnion); showUnionDialog = false; editingUnionId = null },
                genogramaId = genogramaId, patientId = patientId
            )
        }
        if (showFiliationDialog) {
            SharedFiliationDialog(
                members = state.members,
                initialFiliation = if(editingFiliationId != null) state.filiations.find { it.id == editingFiliationId } else null,
                prefilledParent = if(editingFiliationId == null) selectedMember?.id else null,
                onDismiss = { showFiliationDialog = false; editingFiliationId = null },
                onSave = { newFil -> viewModel.saveFiliation(patientId, genogramaId, newFil); showFiliationDialog = false; editingFiliationId = null },
                genogramaId = genogramaId, patientId = patientId
            )
        }
        if (showEmotionalDialog) {
            SharedEmotionalDialog(
                members = state.members,
                initialBond = if(editingBondId != null) state.emotionalBonds.find { it.id == editingBondId } else null,
                prefilledMemberA = if(editingBondId == null) selectedMember?.id else null,
                onDismiss = { showEmotionalDialog = false; editingBondId = null },
                onSave = { newBond -> viewModel.saveEmotionalBond(patientId, genogramaId, newBond); showEmotionalDialog = false; editingBondId = null },
                genogramaId = genogramaId, patientId = patientId
            )
        }
    }
}


fun se(condition: Boolean, trueVal: Float, falseVal: Float): Float = if(condition) trueVal else falseVal

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEmotionalBondCurve(pA: Offset, pB: Offset, tipo: String) {
    if (pA.x == pB.x && pA.y == pB.y) return
    val dx = pB.x - pA.x
    val dy = pB.y - pA.y
    val len = kotlin.math.hypot(dx, dy)
    if (len == 0f) return
    
    val nx = -dy / len
    val ny = dx / len
    val arcHeight = 120f
    
    fun getBezierPoint(t: Float): Offset {
        val parabola = 4 * arcHeight * t * (1 - t)
        return Offset(pA.x + dx * t + nx * parabola, pA.y + dy * t + ny * parabola)
    }
    
    val color = when(tipo) {
        "Conflituoso", "Rompido" -> Color.Red
        "Distante" -> Color.Gray
        else -> Color(0xFF2E7D32) // Verde para Harmonicos e Proximos
    }
    
    if (tipo == "Conflituoso") {
        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(pA.x, pA.y)
        val segments = 30
        for (i in 1..segments) {
            val t = i / segments.toFloat()
            val bp = getBezierPoint(t)
            val sign = if (i % 2 == 0) 1 else -1
            if (i == segments) {
                path.lineTo(pB.x, pB.y)
            } else {
                path.lineTo(bp.x + nx * (15f * sign), bp.y + ny * (15f * sign))
            }
        }
        drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
    } else {
        val style = if (tipo == "Distante") {
            androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
        } else androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        
        fun drawArch(offsetMultiplier: Float) {
            val offsetDist = offsetMultiplier * 15f
            val basePath = androidx.compose.ui.graphics.Path()
            basePath.moveTo(pA.x + nx * offsetDist, pA.y + ny * offsetDist)
            basePath.quadraticBezierTo(
                pA.x + dx * 0.5f + nx * (arcHeight + offsetDist),
                pA.y + dy * 0.5f + ny * (arcHeight + offsetDist),
                pB.x + nx * offsetDist, pB.y + ny * offsetDist
            )
            drawPath(basePath, color = color, style = style)
        }

        drawArch(0f)
        if (tipo == "Próximo" || tipo == "Muito Próximo") drawArch(1f)
        if (tipo == "Muito Próximo") drawArch(-1f)
        
        if (tipo == "Rompido") {
            val midp = getBezierPoint(0.5f)
            val cutDirX = dx / len
            val cutDirY = dy / len
            drawLine(color, start = Offset(midp.x - cutDirX*10f - nx*20f, midp.y - cutDirY*10f - ny*20f), end = Offset(midp.x - cutDirX*10f + nx*20f, midp.y - cutDirY*10f + ny*20f), strokeWidth = 5f)
            drawLine(color, start = Offset(midp.x + cutDirX*10f - nx*20f, midp.y + cutDirY*10f - ny*20f), end = Offset(midp.x + cutDirX*10f + nx*20f, midp.y + cutDirY*10f + ny*20f), strokeWidth = 5f)
        }
    }
}


fun generateGenogramaBitmap(
    members: List<FamilyMember>,
    unions: List<com.project.data.model.GenogramUnion>,
    filiations: List<com.project.data.model.GenogramFiliation>,
    emotionalBonds: List<com.project.data.model.EmotionalBond>,
    nodePositions: Map<String, Offset>,
    shapeSize: Float
): Bitmap {
    var minX = 0f
    var maxX = 0f
    var minY = 0f
    var maxY = 0f

    nodePositions.values.forEach { offset ->
        if (offset.x < minX) minX = offset.x
        if (offset.x > maxX) maxX = offset.x
        if (offset.y < minY) minY = offset.y
        if (offset.y > maxY) maxY = offset.y
    }

    val padding = 300f
    val spanX = maxX - minX + padding * 2
    val spanY = maxY - minY + padding * 2

    val width = java.lang.Math.max(1200f, spanX).toInt()
    val height = java.lang.Math.max(1200f, spanY).toInt()

    val renderCenterOffset = Offset(width / 2f - (maxX + minX) / 2f, height / 2f - (maxY + minY) / 2f)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val composeCanvas = androidx.compose.ui.graphics.Canvas(bitmap.asImageBitmap())
    val canvasDrawScope = CanvasDrawScope()

    val textPaintPath = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 34f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }

    canvasDrawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = composeCanvas,
        size = Size(width.toFloat(), height.toFloat())
    ) {
        drawRect(Color.White)
        
        // 1. Draw Edges first internally (Underneath)
        unions.forEach { union ->
            val relA = nodePositions[union.membroA]
            val relB = nodePositions[union.membroB]
            if (relA != null && relB != null) {
                val posA = renderCenterOffset + relA
                val posB = renderCenterOffset + relB
                val corVnculo = if (union.tipo == "Casamento") Color.Black else Color.DarkGray
                
                drawLine(
                    color = corVnculo,
                    start = posA,
                    end = posB,
                    strokeWidth = 4f,
                    pathEffect = if(union.tipo == "União Estável" || union.tipo == "Namoro") androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f) else null
                )
                
                if (union.status == "Separado" || union.status == "Divorciado") {
                    val midX = (posA.x + posB.x) / 2f
                    val midY = (posA.y + posB.y) / 2f
                    drawLine(color = Color.Red, start = Offset(midX - 10f, midY - 20f), end = Offset(midX + 10f, midY + 20f), strokeWidth = 5f)
                    if (union.status == "Divorciado") {
                        drawLine(color = Color.Red, start = Offset(midX - 25f, midY - 20f), end = Offset(midX - 5f, midY + 20f), strokeWidth = 5f)
                    }
                }
            }
        }
        
        // 2. Linhas Filiais (Biological Edges)
        filiations.forEach { fil ->
            val relChild = nodePositions[fil.filhoId]
            if (relChild != null) {
                val childPos = renderCenterOffset + relChild
                val pA = nodePositions[fil.paiId]?.let { renderCenterOffset + it }
                val pB = nodePositions[fil.maeId]?.let { renderCenterOffset + it }
                
                val dashFx = if(fil.tipo == "Adotivo") androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f) else null
                val clr = if(fil.tipo == "Adotivo") Color.Blue else Color.Black
                
                if (pA != null && pB != null) {
                    val midX = (pA.x + pB.x) / 2f
                    val midY = (pA.y + pB.y) / 2f
                    val dropY = childPos.y - 120f
                    
                    drawLine(color = clr, start = Offset(midX, midY), end = Offset(midX, dropY), strokeWidth = 3f, pathEffect = dashFx)
                    drawLine(color = clr, start = Offset(midX, dropY), end = Offset(childPos.x, dropY), strokeWidth = 3f, pathEffect = dashFx)
                    drawLine(color = clr, start = Offset(childPos.x, dropY), end = childPos, strokeWidth = 3f, pathEffect = dashFx)
                } else if (pA != null) {
                    drawLine(color = clr, start = pA, end = childPos, strokeWidth = 3f, pathEffect = dashFx)
                } else if (pB != null) {
                    drawLine(color = clr, start = pB, end = childPos, strokeWidth = 3f, pathEffect = dashFx)
                }
            }
        }
        
        // 3. Tracos Psicologicos Flexiveis (Bézier Curves)
        emotionalBonds.forEach { bond ->
            val posA = nodePositions[bond.membroAId]?.let { renderCenterOffset + it }
            val posB = nodePositions[bond.membroBId]?.let { renderCenterOffset + it }
            if(posA != null && posB != null) {
                drawEmotionalBondCurve(posA, posB, bond.tipo)
            }
        }
        
        val halfShape = shapeSize / 2f

        members.forEach { member ->
            val relativeOffset = nodePositions[member.id]
            if (relativeOffset != null) {
                val pt = renderCenterOffset + relativeOffset
                
                when(member.sexo) {
                    "M" -> {
                        drawRect(color = Color.Black, topLeft = Offset(pt.x - halfShape, pt.y - halfShape), size = Size(shapeSize, shapeSize), style = Stroke(width = if(member.isEgo) 10f else 5f))
                        if(member.isEgo) drawRect(color = Color.Black, topLeft = Offset(pt.x - halfShape + 15f, pt.y - halfShape + 15f), size = Size(shapeSize - 30f, shapeSize - 30f), style = Stroke(width = 3f))
                    }
                    "F" -> {
                        drawCircle(color = Color.Black, radius = halfShape, center = pt, style = Stroke(width = if(member.isEgo) 10f else 5f))
                        if(member.isEgo) drawCircle(color = Color.Black, radius = halfShape - 15f, center = pt, style = Stroke(width = 3f))
                    }
                    else -> {
                        val path = Path().apply {
                            moveTo(pt.x, pt.y - halfShape)
                            lineTo(pt.x + halfShape, pt.y)
                            lineTo(pt.x, pt.y + halfShape)
                            lineTo(pt.x - halfShape, pt.y)
                            close()
                        }
                        drawPath(path = path, color = Color.Black, style = Stroke(width = if(member.isEgo) 10f else 5f))
                        if(member.isEgo) {
                            val ipath = Path().apply {
                                moveTo(pt.x, pt.y - halfShape + 18f)
                                lineTo(pt.x + halfShape - 18f, pt.y)
                                lineTo(pt.x, pt.y + halfShape - 18f)
                                lineTo(pt.x - halfShape + 18f, pt.y)
                                close()
                            }
                            drawPath(ipath, color = Color.Black, style = Stroke(width = 3f))
                        }
                    }
                }
                
                if(!member.vivo) {
                    drawLine(color = Color.Black, start = Offset(pt.x - halfShape, pt.y - halfShape), end = Offset(pt.x + halfShape, pt.y + halfShape), strokeWidth = 6f)
                    drawLine(color = Color.Black, start = Offset(pt.x + halfShape, pt.y - halfShape), end = Offset(pt.x - halfShape, pt.y + halfShape), strokeWidth = 6f)
                }

                drawContext.canvas.nativeCanvas.drawText(member.nome.take(15), pt.x, pt.y + halfShape + 40f, textPaintPath)
                val birthText = if(member.nascimento.isNotEmpty()) member.nascimento.takeLast(4) else ""
                var meta = birthText
                if(!member.vivo && member.falecimento.isNotEmpty()) meta = "$birthText - ${member.falecimento.takeLast(4)}"
                if(meta.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(meta, pt.x, pt.y + halfShape + 80f, textPaintPath.apply { textSize = 24f; isFakeBoldText = false })
                }
            }
        }
    }
    return bitmap
}

fun saveGenogramaToGallery(context: Context, bitmap: Bitmap, patientName: String) {
    val filename = "Genograma_${patientName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        resolver.openOutputStream(it)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Toast.makeText(context, "Imagem de Genograma salva!", Toast.LENGTH_LONG).show()
    } ?: run {
        Toast.makeText(context, "Erro ao exportar a imagem.", Toast.LENGTH_SHORT).show()
    }
}
