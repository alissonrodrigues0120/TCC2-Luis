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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
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
import com.project.ui.components.TooltipIconButton

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
    var unionToDelete by remember { mutableStateOf<com.project.data.model.GenogramUnion?>(null) }
    var filiationToDelete by remember { mutableStateOf<com.project.data.model.GenogramFiliation?>(null) }
    var bondToDelete by remember { mutableStateOf<com.project.data.model.EmotionalBond?>(null) }
    
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showDeleteMemberDialog by remember { mutableStateOf(false) }
    
    // Parametros Fisicos
    val verticalSpacing = 280f
    val horizontalSpacing = 220f
    val nodePositions = remember { mutableStateMapOf<String, Offset>() }
    val manualDragOffsets = remember { mutableStateMapOf<String, Offset>() }
    val shapeSize = 80f
    val halfShape = shapeSize / 2f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visualizador", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    TooltipIconButton(tooltipText = "Voltar", onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TooltipIconButton(tooltipText = "Baixar Imagem", onClick = {
                        val bitmap = generateGenogramaBitmap(
                            members = state.members,
                            unions = state.unions,
                            filiations = state.filiations,
                            emotionalBonds = state.emotionalBonds,
                            nodePositions = nodePositions,
                            manualDragOffsets = manualDragOffsets,
                            shapeSize = shapeSize
                        )
                        saveGenogramaToGallery(context, bitmap, "Paciente_$patientId")
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Salvar na Galeria", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FloatingActionButton(
                    onClick = {
                        scale = 1f
                        pan = Offset.Zero
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "Centralizar Visão")
                }
                ExtendedFloatingActionButton(
                    text = { Text("Novo Membro") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar Membro") },
                    onClick = { showQuickAddDialog = true }
                )
            }
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

            // Motor de Matriz Hierárquico (Tree Layout Algorithm)
            val localMembers = state.members
            val localFiliations = state.filiations
            val localUnions = state.unions

            LaunchedEffect(localMembers, localFiliations, localUnions, canvasCenter) {
                nodePositions.clear()
                
                localMembers.forEach {
                    if (it.offsetX != 0f || it.offsetY != 0f) {
                        manualDragOffsets[it.id] = Offset(it.offsetX, it.offsetY)
                    }
                }
                
                val ego = localMembers.find { it.isEgo }
                if (ego == null) {
                    localMembers.forEachIndexed { i, m -> nodePositions[m.id] = Offset(canvasCenter.x + (i*100f), canvasCenter.y) }
                    return@LaunchedEffect
                }

                val processed = mutableSetOf<String>()
                val logicalGrid = mutableMapOf<String, Pair<Float, Float>>()
                
                fun getBirthYear(m: FamilyMember): Int {
                    val y = m.nascimento.takeLast(4).filter { it.isDigit() }
                    return y.toIntOrNull() ?: 9999
                }

                // ==========================================
                // LÓGICA DE ÁRVORE DESCENDENTE (Filhos, Netos...) - Declarado antes para calcUpWidths
                // ==========================================
                val downWidths = mutableMapOf<String, Float>()
                fun calcDownWidth(id: String): Float {
                    if (downWidths.containsKey(id)) return downWidths[id]!!
                    val children = localFiliations.filter { it.paiId == id || it.maeId == id }.map { it.filhoId }.distinct()
                    val spouses = localUnions.filter { it.membroA == id || it.membroB == id }.map { if (it.membroA == id) it.membroB else it.membroA }.distinct()
                    
                    var w = 1.2f + (spouses.size * 1.5f)
                    
                    if (children.isNotEmpty()) {
                        var cw = 0f
                        children.forEach { cw += calcDownWidth(it) }
                        cw += (children.size - 1) * 0.5f
                        w = maxOf(w, cw)
                    }
                    downWidths[id] = w
                    return w
                }

                // ==========================================
                // LÓGICA DE ÁRVORE ASCENDENTE (Pais, Avós...)
                // ==========================================
                val upLeftWidths = mutableMapOf<String, Float>()
                val upRightWidths = mutableMapOf<String, Float>()
                
                fun calcUpWidths(id: String, direction: Int) {
                    if (upLeftWidths.containsKey(id)) return
                    
                    val f = localFiliations.find { it.filhoId == id }
                    if (f == null || (f.paiId.isEmpty() && f.maeId.isEmpty())) {
                        upLeftWidths[id] = 0.6f
                        upRightWidths[id] = 0.6f
                        return
                    }
                    
                    if (f.paiId.isNotEmpty()) calcUpWidths(f.paiId, if (direction == 0) -1 else direction)
                    if (f.maeId.isNotEmpty()) calcUpWidths(f.maeId, if (direction == 0) 1 else direction)
                    
                    var wP_L = 0.6f; var wP_R = 0.6f
                    if (f.paiId.isNotEmpty()) {
                        wP_L = upLeftWidths[f.paiId] ?: 0.6f
                        wP_R = upRightWidths[f.paiId] ?: 0.6f
                    }
                    var wM_L = 0.6f; var wM_R = 0.6f
                    if (f.maeId.isNotEmpty()) {
                        wM_L = upLeftWidths[f.maeId] ?: 0.6f
                        wM_R = upRightWidths[f.maeId] ?: 0.6f
                    }
                    
                    val gap = 0.5f
                    var directLeft = 0.6f
                    var directRight = 0.6f
                    
                    if (f.paiId.isNotEmpty() && f.maeId.isNotEmpty()) {
                        directLeft = wP_L + wP_R + gap/2
                        directRight = wM_L + wM_R + gap/2
                    } else if (f.paiId.isNotEmpty()) {
                        directLeft = wP_L
                        directRight = wP_R
                    } else {
                        directLeft = wM_L
                        directRight = wM_R
                    }
                    
                    val parents = setOf(f.paiId, f.maeId).filter { it.isNotEmpty() }
                    val siblingsIds = localFiliations.filter { 
                        it.filhoId != id && (parents.contains(it.paiId) || parents.contains(it.maeId)) 
                    }.map { it.filhoId }.distinct()
                    
                    var olderSiblingsWidth = 0f
                    var youngerSiblingsWidth = 0f
                    var totalSiblingsWidth = 0f
                    
                    if (siblingsIds.isNotEmpty()) {
                        val sortedSiblings = localMembers.filter { it.id in siblingsIds }.sortedBy { getBirthYear(it) }
                        val member = localMembers.find { it.id == id }
                        val year = if(member != null) getBirthYear(member) else 9999
                        
                        sortedSiblings.forEach { sib ->
                            calcDownWidth(sib.id)
                            val w = (downWidths[sib.id] ?: 1.2f) + 0.5f
                            totalSiblingsWidth += w
                            if (getBirthYear(sib) <= year) olderSiblingsWidth += w else youngerSiblingsWidth += w
                        }
                    }
                    
                    if (direction == 0) {
                        upLeftWidths[id] = directLeft + olderSiblingsWidth
                        upRightWidths[id] = directRight + youngerSiblingsWidth
                    } else if (direction < 0) {
                        upLeftWidths[id] = directLeft + totalSiblingsWidth
                        upRightWidths[id] = directRight
                    } else {
                        upLeftWidths[id] = directLeft
                        upRightWidths[id] = directRight + totalSiblingsWidth
                    }
                }

                fun assignUp(id: String, x: Float, y: Float, direction: Int) {
                    if (!processed.contains(id)) {
                        logicalGrid[id] = Pair(x, y)
                        processed.add(id)
                    }
                    val f = localFiliations.find { it.filhoId == id } ?: return
                    val pId = f.paiId
                    val mId = f.maeId
                    
                    val gap = 0.5f
                    
                    if (pId.isNotEmpty() && mId.isNotEmpty()) {
                        val wP_R = upRightWidths[pId] ?: 0.6f
                        val wM_L = upLeftWidths[mId] ?: 0.6f
                        
                        val parents = setOf(pId, mId)
                        val siblingsIds = localFiliations.filter { 
                            it.filhoId != id && (parents.contains(it.paiId) || parents.contains(it.maeId)) 
                        }.map { it.filhoId }.distinct()
                        
                        var olderSiblingsW = 0f
                        var youngerSiblingsW = 0f
                        if (direction == 0 && siblingsIds.isNotEmpty()) {
                            val sortedSiblings = localMembers.filter { it.id in siblingsIds }.sortedBy { getBirthYear(it) }
                            val member = localMembers.find { it.id == id }
                            val year = if(member != null) getBirthYear(member) else 9999
                            sortedSiblings.forEach { sib ->
                                val w = (downWidths[sib.id] ?: 1.2f) + 0.5f
                                if (getBirthYear(sib) <= year) olderSiblingsW += w else youngerSiblingsW += w
                            }
                        }
                        
                        val paiX = x - gap/2 - wP_R - olderSiblingsW
                        val maeX = x + gap/2 + wM_L + youngerSiblingsW
                        
                        assignUp(pId, paiX, y - 1f, if(direction == 0) -1 else direction)
                        assignUp(mId, maeX, y - 1f, if(direction == 0) 1 else direction)
                    } else if (pId.isNotEmpty()) {
                        assignUp(pId, x, y - 1f, if(direction == 0) -1 else direction)
                    } else if (mId.isNotEmpty()) {
                        assignUp(mId, x, y - 1f, if(direction == 0) 1 else direction)
                    }
                }



                fun assignDown(id: String, x: Float, y: Float, direction: Int = 0) {
                    if (!processed.contains(id)) {
                        logicalGrid[id] = Pair(x, y)
                        processed.add(id)
                    }
                    
                    val spouses = localUnions.filter { it.membroA == id || it.membroB == id }.map { if (it.membroA == id) it.membroB else it.membroA }.distinct()
                    var spX = if (direction < 0) x - 1.5f else x + 1.5f
                    var unplacedSpousesCount = 0
                    spouses.forEach { spId ->
                        if (!processed.contains(spId)) {
                            logicalGrid[spId] = Pair(spX, y)
                            processed.add(spId)
                            spX += if (direction < 0) -1.5f else 1.5f
                            unplacedSpousesCount++
                        }
                    }
                    
                    val childrenIds = localFiliations.filter { it.paiId == id || it.maeId == id }.map { it.filhoId }.distinct()
                    val sortedChildren = localMembers.filter { it.id in childrenIds }.sortedBy { getBirthYear(it) }
                    
                    if (sortedChildren.isNotEmpty()) {
                        val totalW = sortedChildren.sumOf { (downWidths[it.id] ?: 1.2f).toDouble() }.toFloat() + (sortedChildren.size - 1) * 0.5f
                        val blockCenterX = if (direction < 0) x - (unplacedSpousesCount * 1.5f) / 2f else x + (unplacedSpousesCount * 1.5f) / 2f
                        
                        var currentX = blockCenterX - totalW / 2f
                        sortedChildren.forEach { child ->
                            val wC = downWidths[child.id] ?: 1.2f
                            val childX = if (direction < 0) currentX + wC - 0.6f else currentX + 0.6f
                            assignDown(child.id, childX, y + 1f, direction)
                            currentX += wC + 0.5f
                        }
                    }
                }

                // 1. Processar Ascendentes do Ego
                calcUpWidths(ego.id, 0)
                assignUp(ego.id, 0f, 0f, 0)
                
                // 2. Colocar Esposas do Ego
                val spouses = localUnions.filter { it.membroA == ego.id || it.membroB == ego.id }.map { if (it.membroA == ego.id) it.membroB else it.membroA }.distinct()
                var spX = 1.5f
                var unplacedSpousesCount = 0
                spouses.forEach { spId ->
                    if (!processed.contains(spId)) {
                        logicalGrid[spId] = Pair(spX, 0f)
                        processed.add(spId)
                        spX += 1.5f
                        unplacedSpousesCount++
                    }
                }

                // 3. Processar Descendentes do Ego
                calcDownWidth(ego.id)
                val childrenIds = localFiliations.filter { it.paiId == ego.id || it.maeId == ego.id }.map { it.filhoId }.distinct()
                val sortedChildren = localMembers.filter { it.id in childrenIds }.sortedBy { getBirthYear(it) }
                if (sortedChildren.isNotEmpty()) {
                    val totalW = sortedChildren.sumOf { (downWidths[it.id] ?: 1.2f).toDouble() }.toFloat() + (sortedChildren.size - 1) * 0.5f
                    val blockCenterX = (unplacedSpousesCount * 1.5f) / 2f
                    var currentX = blockCenterX - totalW / 2f
                    sortedChildren.forEach { child ->
                        val wC = downWidths[child.id] ?: 1.2f
                        val childX = currentX + wC / 2f
                        assignDown(child.id, childX, 1f)
                        currentX += wC + 0.5f
                    }
                }

                // 4. Processar Ramificações Colaterais (Irmãos, Tios, Tios-Avôs)
                val ancestors = processed.toList()
                ancestors.forEach { ancId ->
                    val ancPos = logicalGrid[ancId]!!
                    val ancY = ancPos.second
                    
                    val ancFil = localFiliations.find { it.filhoId == ancId }
                    if (ancFil != null && (ancFil.paiId.isNotEmpty() || ancFil.maeId.isNotEmpty())) {
                        val ancParents = setOf(ancFil.paiId, ancFil.maeId).filter { it.isNotEmpty() }
                        
                        val siblingsIds = localFiliations.filter { 
                            it.filhoId != ancId && !processed.contains(it.filhoId) &&
                            (ancParents.contains(it.paiId) || ancParents.contains(it.maeId)) 
                        }.map { it.filhoId }.distinct()
                        
                        if (siblingsIds.isNotEmpty()) {
                            val sortedSiblings = localMembers.filter { it.id in siblingsIds }.sortedBy { getBirthYear(it) }
                            val ancMember = localMembers.find { it.id == ancId }
                            val ancYear = if(ancMember != null) getBirthYear(ancMember) else 9999
                            
                            val direction = if (ancId == ego.id) 0 else if (ancPos.first < 0) -1 else 1
                            val ancSpousesCount = localUnions.filter { it.membroA == ancId || it.membroB == ancId }.map { if (it.membroA == ancId) it.membroB else it.membroA }.distinct().size
                            
                            if (direction == 0) {
                                // Ego: Mantém o comportamento original (velhos à esquerda, novos à direita)
                                val older = sortedSiblings.filter { getBirthYear(it) <= ancYear }
                                val younger = sortedSiblings.filter { getBirthYear(it) > ancYear }
                                
                                var leftX = ancPos.first - 0.6f - 0.5f
                                older.reversed().forEach { sib ->
                                    calcDownWidth(sib.id)
                                    val w = downWidths[sib.id] ?: 1.2f
                                    val sibX = leftX - w + 0.6f
                                    assignDown(sib.id, sibX, ancY)
                                    leftX -= w + 0.5f
                                }
                                
                                var rightX = ancPos.first + 0.6f + (ancSpousesCount * 1.5f) + 0.5f
                                younger.forEach { sib ->
                                    calcDownWidth(sib.id)
                                    val w = downWidths[sib.id] ?: 1.2f
                                    val sibX = rightX + 0.6f
                                    assignDown(sib.id, sibX, ancY)
                                    rightX += w + 0.5f
                                }
                            } else if (direction < 0) {
                                // Lado Paterno: Todos os colaterais são empurrados para a esquerda
                                var leftX = ancPos.first - 0.6f - 0.5f
                                sortedSiblings.reversed().forEach { sib ->
                                    calcDownWidth(sib.id)
                                    val w = downWidths[sib.id] ?: 1.2f
                                    val sibX = leftX - 0.6f
                                    assignDown(sib.id, sibX, ancY, direction)
                                    leftX -= w + 0.5f
                                }
                            } else {
                                // Lado Materno: Todos os colaterais são empurrados para a direita
                                var rightX = ancPos.first + 0.6f + (ancSpousesCount * 1.5f) + 0.5f
                                sortedSiblings.forEach { sib ->
                                    calcDownWidth(sib.id)
                                    val w = downWidths[sib.id] ?: 1.2f
                                    val sibX = rightX + 0.6f
                                    assignDown(sib.id, sibX, ancY, direction)
                                    rightX += w + 0.5f
                                }
                            }
                        }
                    }
                }

                // 5. Sobras (Orfãos Livres)
                val orphans = localMembers.filter { !processed.contains(it.id) }
                var orphanX = -4f
                var orphanY = -3f
                orphans.forEachIndexed { i, orphan ->
                    calcDownWidth(orphan.id)
                    assignDown(orphan.id, orphanX, orphanY)
                    orphanX += (downWidths[orphan.id] ?: 1.2f) + 1f
                    if (i % 5 == 4) { 
                        orphanY += 1f
                        orphanX = -4f
                    }
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
                        // Helper
                        fun getFinalPos(id: String): Offset? {
                            val autoPos = nodePositions[id] ?: return null
                            val manualOffset = manualDragOffsets[id] ?: Offset.Zero
                            return autoPos + manualOffset
                        }

                        // Linhas Maritais (Casamentos/Uniões)
                        state.unions.forEach { union ->
                            val posA = getFinalPos(union.membroA)
                            val posB = getFinalPos(union.membroB)
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
                            val childPos = getFinalPos(fil.filhoId)
                            if (childPos != null) {
                                val pA = getFinalPos(fil.paiId)
                                val pB = getFinalPos(fil.maeId)
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
                            val pA = getFinalPos(bond.membroAId)
                            val pB = getFinalPos(bond.membroBId)
                            if (pA != null && pB != null) {
                                drawEmotionalBondCurve(pA, pB, bond.tipo)
                            }
                        }
                    }
                    
                    // 2. Desenho das Famílias Inteiramente Clicável
                    state.members.forEach { member ->
                        val autoCenter = nodePositions[member.id]
                        if (autoCenter != null) {
                            val manualOffset = manualDragOffsets[member.id] ?: Offset.Zero
                            val center = autoCenter + manualOffset
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((center.x - halfShape).roundToInt(), (center.y - halfShape).roundToInt()) }
                                    .size(with(density) { shapeSize.toDp() })
                                    .pointerInput(member.id) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                val myAutoPos = nodePositions[member.id]
                                                if (myAutoPos != null) {
                                                    var resolved = false
                                                    var iterations = 0
                                                    val minRadiusDist = shapeSize + 20f
                                                    
                                                    while (!resolved && iterations < 5) {
                                                        resolved = true
                                                        val myManual = manualDragOffsets[member.id] ?: Offset.Zero
                                                        var myPos = myAutoPos + myManual

                                                        state.members.forEach { other ->
                                                            if (other.id != member.id) {
                                                                val otherAuto = nodePositions[other.id]
                                                                if (otherAuto != null) {
                                                                    val otherManual = manualDragOffsets[other.id] ?: Offset.Zero
                                                                    val otherPos = otherAuto + otherManual
                                                                    
                                                                    val dx = myPos.x - otherPos.x
                                                                    val dy = myPos.y - otherPos.y
                                                                    val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                                                                    
                                                                    if (dist < minRadiusDist && dist > 0.1f) {
                                                                        resolved = false
                                                                        val overlap = minRadiusDist - dist
                                                                        val pushX = (dx / dist) * overlap
                                                                        val pushY = (dy / dist) * overlap
                                                                        myPos = Offset(myPos.x + pushX, myPos.y + pushY)
                                                                    } else if (dist <= 0.1f) {
                                                                        resolved = false
                                                                        myPos = Offset(myPos.x + minRadiusDist, myPos.y)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        
                                                        manualDragOffsets[member.id] = myPos - myAutoPos
                                                        iterations++
                                                    }
                                                    val finalOffset = manualDragOffsets[member.id] ?: Offset.Zero
                                                    viewModel.updateMemberPosition(patientId, genogramaId, member.id, finalOffset.x, finalOffset.y)
                                                }
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            val currentOffset = manualDragOffsets[member.id] ?: Offset.Zero
                                            manualDragOffsets[member.id] = currentOffset + dragAmount
                                        }
                                    }
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
                ) {
                    Text("Interação com: ${sm.nome}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
                    Text("Geração: ${sm.geracao} | Ocupação: ${sm.ocupacao.ifEmpty { "N/A" }} | " + if(sm.vivo) "Vivo" else "In Memorian", fontSize = 14.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onEditMember(sm.id); selectedMember = null }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Editar Perfil Básico", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { showDeleteMemberDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Deletar", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Deletar", fontSize = 13.sp)
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    val pagerState = rememberPagerState(pageCount = { 3 })
                    val coroutineScope = rememberCoroutineScope()
                    val tabs = listOf("Casamentos", "Parentesco", "Psicossocial")

                    TabRow(selectedTabIndex = pagerState.currentPage) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                text = { Text(title, fontSize = 12.sp) }
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
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                .padding(vertical = 16.dp)
                        ) {
                            when (page) {
                                0 -> {
                                    // UNIÕES DELE
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Casamentos & Uniões", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                        TooltipIconButton(tooltipText = "Add", onClick = { editingUnionId = null; showUnionDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF3F51B5)) }
                                    }
                                    state.unions.filter { it.membroA == sm.id || it.membroB == sm.id }.forEach { un ->
                                        val pt = if(un.membroA == sm.id) state.members.find{ it.id == un.membroB }?.nome else state.members.find{ it.id == un.membroA }?.nome
                                        FamilyMemberItem(
                                            title = "Com: ${pt ?: "Desconhecido"}", subtitle = "${un.tipo} (${un.status})", isEgo = false,
                                            onEdit = { editingUnionId = un.id; showUnionDialog = true },
                                            onDelete = { unionToDelete = un }, showEdit = true
                                        )
                                    }
                                }
                                1 -> {
                                    // FILIAÇÕES DELE
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Relações Familiares (Pai/Mãe/Filho)", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                        TooltipIconButton(tooltipText = "Add", onClick = { editingFiliationId = null; showFiliationDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF3F51B5)) }
                                    }
                                    state.filiations.filter { it.filhoId == sm.id || it.paiId == sm.id || it.maeId == sm.id }.forEach { fil ->
                                        val role = if(fil.filhoId == sm.id) "É Filho de: ${state.members.find{ it.id == fil.paiId }?.nome} e ${state.members.find{ it.id == fil.maeId }?.nome}" else "É Pai/Mãe de: ${state.members.find{ it.id == fil.filhoId }?.nome}"
                                        FamilyMemberItem(
                                            title = role, subtitle = fil.tipo, isEgo = false,
                                            onEdit = { editingFiliationId = fil.id; showFiliationDialog = true },
                                            onDelete = { filiationToDelete = fil }, showEdit = true
                                        )
                                    }
                                }
                                2 -> {
                                    // LAÇOS EMOCIONAIS DELE
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Traços Psicossociais", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                        TooltipIconButton(tooltipText = "Add", onClick = { editingBondId = null; showEmotionalDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF3F51B5)) }
                                    }
                                    state.emotionalBonds.filter { it.membroAId == sm.id || it.membroBId == sm.id }.forEach { bond ->
                                        val target = if(bond.membroAId == sm.id) state.members.find{ it.id == bond.membroBId }?.nome else state.members.find{ it.id == bond.membroAId }?.nome
                                        FamilyMemberItem(
                                            title = "Com: ${target ?: "Desconhecido"}", subtitle = bond.tipo, isEgo = false,
                                            onEdit = { editingBondId = bond.id; showEmotionalDialog = true },
                                            onDelete = { bondToDelete = bond }, showEdit = true
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
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

        if (showQuickAddDialog) {
            QuickAddMemberDialog(
                onDismiss = { showQuickAddDialog = false },
                onSave = { novoMembro ->
                    viewModel.saveMember(patientId, genogramaId, novoMembro)
                    showQuickAddDialog = false
                }
            )
        }

        if (showDeleteMemberDialog && selectedMember != null) {
            AlertDialog(
                onDismissRequest = { showDeleteMemberDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = "Atenção", tint = MaterialTheme.colorScheme.error) },
                title = { Text("Deletar Membro") },
                text = { Text("Tem certeza que deseja deletar ${selectedMember!!.nome}? Esta ação removerá também todas as conexões (uniões, filiações) atreladas a este membro.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteMember(patientId, genogramaId, selectedMember!!.id)
                            showDeleteMemberDialog = false
                            selectedMember = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Deletar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteMemberDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (unionToDelete != null) {
            AlertDialog(
                onDismissRequest = { unionToDelete = null },
                title = { Text("Excluir União") },
                text = { Text("Deseja deletar este casamento/união?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteUnion(patientId, genogramaId, unionToDelete!!.id)
                            unionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    Button(onClick = { unionToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (filiationToDelete != null) {
            AlertDialog(
                onDismissRequest = { filiationToDelete = null },
                title = { Text("Excluir Filiação") },
                text = { Text("Deseja deletar o registro de parentesco biológico?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteFiliation(patientId, genogramaId, filiationToDelete!!.id)
                            filiationToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    Button(onClick = { filiationToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (bondToDelete != null) {
            AlertDialog(
                onDismissRequest = { bondToDelete = null },
                title = { Text("Excluir Vínculo") },
                text = { Text("Deseja deletar a afinidade psicológica?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteEmotionalBond(patientId, genogramaId, bondToDelete!!.id)
                            bondToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    Button(onClick = { bondToDelete = null }) {
                        Text("Cancelar")
                    }
                }
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
    manualDragOffsets: Map<String, Offset>,
    shapeSize: Float
): Bitmap {
    var minX = 0f
    var maxX = 0f
    var minY = 0f
    var maxY = 0f

    nodePositions.forEach { (id, offset) ->
        val finalOffset = offset + (manualDragOffsets[id] ?: Offset.Zero)
        if (finalOffset.x < minX) minX = finalOffset.x
        if (finalOffset.x > maxX) maxX = finalOffset.x
        if (finalOffset.y < minY) minY = finalOffset.y
        if (finalOffset.y > maxY) maxY = finalOffset.y
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
        
        fun getFinalPos(id: String): Offset? {
            val autoPos = nodePositions[id] ?: return null
            val manualOffset = manualDragOffsets[id] ?: Offset.Zero
            return autoPos + manualOffset
        }

        // 1. Draw Edges first internally (Underneath)
        unions.forEach { union ->
            val relA = getFinalPos(union.membroA)
            val relB = getFinalPos(union.membroB)
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
            val relChild = getFinalPos(fil.filhoId)
            if (relChild != null) {
                val childPos = renderCenterOffset + relChild
                val pA = getFinalPos(fil.paiId)?.let { renderCenterOffset + it }
                val pB = getFinalPos(fil.maeId)?.let { renderCenterOffset + it }
                
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
            val posA = getFinalPos(bond.membroAId)?.let { renderCenterOffset + it }
            val posB = getFinalPos(bond.membroBId)?.let { renderCenterOffset + it }
            if(posA != null && posB != null) {
                drawEmotionalBondCurve(posA, posB, bond.tipo)
            }
        }
        
        val halfShape = shapeSize / 2f

        members.forEach { member ->
            val relativeOffset = getFinalPos(member.id)
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
