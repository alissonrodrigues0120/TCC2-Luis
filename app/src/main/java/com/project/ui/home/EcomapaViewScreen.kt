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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.data.model.SupportNetwork
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource

data class CategoryStyle(val bg: Color, val border: Color, val icon: String)

fun getCategoryStyle(category: String): CategoryStyle {
    return when (category) {
        "Família" -> CategoryStyle(bg = Color(0xFFFFF3E0), border = Color(0xFFE65100), icon = "🏠")
        "Trabalho" -> CategoryStyle(bg = Color(0xFFE3F2FD), border = Color(0xFF1565C0), icon = "💼")
        "Religioso" -> CategoryStyle(bg = Color(0xFFF3E5F5), border = Color(0xFF6A1B9A), icon = "✝")
        "Comunidade" -> CategoryStyle(bg = Color(0xFFE8F5E9), border = Color(0xFF2E7D32), icon = "👥")
        "Saúde" -> CategoryStyle(bg = Color(0xFFFCE4EC), border = Color(0xFFB71C1C), icon = "⚕")
        else -> CategoryStyle(bg = Color(0xFFF5F5F5), border = Color(0xFF424242), icon = "◆")
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EcomapaViewScreen(
    patientId: String,
    patientName: String = "Paciente",
    ecomapaId: String,
    viewModel: EcomapaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val networks by viewModel.supportNetworks.collectAsState()

    LaunchedEffect(ecomapaId) {
        viewModel.loadSupportNetworks(patientId, ecomapaId)
    }

    var selectedNetwork by remember { mutableStateOf<SupportNetwork?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showNetworkFormDialog by remember { mutableStateOf(false) }
    var networkToEdit by remember { mutableStateOf<SupportNetwork?>(null) }

    val nodePositions = remember { mutableStateMapOf<String, Offset>() }
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    val patientRadiusPx = 140f // ~60dp
    val networkRadiusPx = 90f // ~38dp

    LaunchedEffect(networks) {
        if (networks.isNotEmpty()) {
            val orbitRadius = if (networks.size > 8) 650f else 500f
            val angleStep = (2 * Math.PI) / networks.size

            networks.forEachIndexed { index, network ->
                if (!nodePositions.containsKey(network.id)) {
                    val angle = index * angleStep
                    val x = (orbitRadius * cos(angle)).toFloat()
                    val y = (orbitRadius * sin(angle)).toFloat()
                    nodePositions[network.id] = Offset(x, y)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visualizador", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = {
                        val bitmap = generateEcomapaBitmap(
                            networks = networks,
                            nodePositions = nodePositions,
                            patientName = patientName,
                            patientRadiusPx = patientRadiusPx,
                            networkRadiusPx = networkRadiusPx
                        )
                        saveBitmapToGallery(context, bitmap, patientName)
                    }) {
                        // Simulating a Download Icon 
                        Text("Baixar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF512DA8), modifier = Modifier.padding(end = 16.dp))
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            val density = LocalDensity.current
            val centerPx = remember(maxWidth, maxHeight) {
                with(density) { Offset(maxWidth.toPx() / 2f, maxHeight.toPx() / 2f) }
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
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        networks.forEach { network ->
                            val relativeOffset = nodePositions[network.id]
                            if (relativeOffset != null) {
                                val absoluteNodeOffset = centerPx + relativeOffset
                                drawEcomapaConnection(
                                    network = network,
                                    start = centerPx,
                                    end = absoluteNodeOffset,
                                    paddingCenter = patientRadiusPx,
                                    paddingNode = networkRadiusPx
                                )
                            }
                        }
                    }

                    // Patient Center Node
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((centerPx.x - patientRadiusPx).roundToInt(), (centerPx.y - patientRadiusPx).roundToInt()) }
                            .size(with(density) { (patientRadiusPx * 2).toDp() })
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                            .border(3.dp, Color(0xFF1A1A1A), CircleShape)
                            .clickable {
                                networkToEdit = null // Sinaliza criação
                                showNetworkFormDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = patientName.take(15),
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // External Nodes
                    networks.forEach { network ->
                        val relativeOffset = nodePositions[network.id]
                        if (relativeOffset != null) {
                            val absoluteNodeOffset = centerPx + relativeOffset
                            val style = getCategoryStyle(network.category)

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((absoluteNodeOffset.x - networkRadiusPx).roundToInt(), (absoluteNodeOffset.y - networkRadiusPx).roundToInt()) }
                                    .size(with(density) { (networkRadiusPx * 2).toDp() })
                                    .pointerInput(network.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val currentPos = nodePositions[network.id] ?: Offset.Zero
                                            nodePositions[network.id] = currentPos + dragAmount
                                        }
                                    }
                                    .clip(CircleShape)
                                    .background(style.bg)
                                    .border(if (network.generatesStress && network.connectionType != "Conflituosa") 3.dp else 2.dp, if (network.generatesStress && network.connectionType != "Conflituosa") Color(0xFFCC0000) else style.border, CircleShape)
                                    .clickable {
                                        selectedNetwork = network
                                        showOptionsDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = style.icon, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = network.name.take(12), color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = network.contactFrequency.take(10), color = Color.DarkGray, fontSize = 8.sp, fontStyle = FontStyle.Italic, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // Legend Overlay
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                Surface(color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp), shadowElevation = 4.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Traço:", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text("━ Forte  |  ─ Fraca", fontSize = 10.sp)
                        Text("╌ Conflituosa | ·· Compensatória", fontSize = 10.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Cores:", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text("🏠 Família | 💼 Trabalho", fontSize = 10.sp)
                        Text("👥 Comunidade | ⚕ Saúde", fontSize = 10.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("⚡ Vínculo Gera Estresse", fontSize = 10.sp, color = Color.Red)
                    }
                }
            }
        }
    }

    if (showOptionsDialog && selectedNetwork != null) {
    NetworkOptionsDialog(
        network = selectedNetwork!!,
        onDismiss = { showOptionsDialog = false },
        onDetails = {
            showOptionsDialog = false
            showDetailsDialog = true
        },
        onEdit = {
            networkToEdit = selectedNetwork
            showOptionsDialog = false
            showNetworkFormDialog = true
        },
        onDelete = {
            viewModel.deleteSupportNetwork(patientId, ecomapaId, selectedNetwork!!.id)
            showOptionsDialog = false
            Toast.makeText(context, "Rede removida", Toast.LENGTH_SHORT).show()
        }
    )
}

if (showNetworkFormDialog) {
    NetworkFormDialog(
        patientId = patientId,
        ecomapaId = ecomapaId,
        existingNetwork = networkToEdit,
        onDismiss = { showNetworkFormDialog = false },
        onSave = { updatedNetwork ->
            viewModel.addSupportNetwork(patientId, ecomapaId, updatedNetwork) {
                showNetworkFormDialog = false
                Toast.makeText(context, "Rede atualizada!", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

if (showDetailsDialog && selectedNetwork != null) {
    val n = selectedNetwork!!
    val style = getCategoryStyle(n.category)
    AlertDialog(
        onDismissRequest = { showDetailsDialog = false },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(style.icon, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                Text(text = n.name.ifEmpty { "Instituição" }, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (n.generatesStress) {
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("⚠ Gera Estresse", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Text("Categoria: ${n.category}${if (n.category == "Outro") " (${n.customCategory})" else ""}")
                Text("Freqüência: ${n.contactFrequency}")
                Text("Conexão: ${n.connectionType}")
                Text("Direção: ${n.supportDirection}")
                Spacer(modifier = Modifier.height(12.dp))

                Text("Tipos de Apoio:", fontWeight = FontWeight.SemiBold)
                FlowRow(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    n.supportTypes.forEach { apoio ->
                        Surface(color = Color(0xFFEDE7F6), shape = RoundedCornerShape(16.dp)) {
                            Text(apoio, fontSize = 12.sp, color = Color(0xFF4527A0), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Descrição:", fontWeight = FontWeight.SemiBold)
                Text(n.description.ifEmpty { "Sem descrição." })
            }
        },
        confirmButton = { TextButton(onClick = { showDetailsDialog = false }) { Text("OK") } }
    )
}
}

fun DrawScope.drawEcomapaConnection(network: SupportNetwork, start: Offset, end: Offset, paddingCenter: Float, paddingNode: Float) {
    val distance = Math.hypot((end.x - start.x).toDouble(), (end.y - start.y).toDouble()).toFloat()
    if (distance <= paddingCenter + paddingNode) return

    val dx = (end.x - start.x) / distance
    val dy = (end.y - start.y) / distance

    val adjustedStart = Offset(start.x + dx * paddingCenter, start.y + dy * paddingCenter)
    val adjustedEnd = Offset(end.x - dx * paddingNode, end.y - dy * paddingNode)

    var strokeWidthDef = 4f
    var pathEffectDef: PathEffect? = null
    var lineColorDef = Color(0xFF1A1A1A)

    when (network.connectionType) {
        "Forte" -> { strokeWidthDef = 8f; lineColorDef = Color(0xFF1A1A1A) }
        "Fraca" -> { strokeWidthDef = 2f; lineColorDef = Color(0xFF555555) }
        "Conflituosa" -> { strokeWidthDef = 6f; pathEffectDef = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f); lineColorDef = Color(0xFFCC0000) }
        "Compensatória" -> { strokeWidthDef = 4f; pathEffectDef = PathEffect.dashPathEffect(floatArrayOf(6f, 12f), 0f); lineColorDef = Color(0xFFFF8C00) }
        "Neutra" -> { strokeWidthDef = 3f; lineColorDef = Color(0xFF999999) }
    }

    drawLine(color = lineColorDef, start = adjustedStart, end = adjustedEnd, strokeWidth = strokeWidthDef, pathEffect = pathEffectDef)

    if (network.generatesStress) {
        val midX = (adjustedStart.x + adjustedEnd.x) / 2
        val midY = (adjustedStart.y + adjustedEnd.y) / 2
        val arrowSize = 40f
        val path = Path().apply {
            moveTo(midX, midY - arrowSize/2)
            lineTo(midX - arrowSize/3, midY + arrowSize/6)
            lineTo(midX + arrowSize/6, midY)
            lineTo(midX, midY + arrowSize/2)
            lineTo(midX + arrowSize/3, midY - arrowSize/6)
            lineTo(midX - arrowSize/6, midY)
            close()
        }
        drawPath(path, color = Color.Red, style = Fill)
    }

    val angle = atan2(dy, dx)
    val dir = network.supportDirection
    val arrowSize = 25f

    if (dir == "Recebe Apoio" || dir == "Mútuo") {
        drawArrow(adjustedStart, angle + Math.PI.toFloat(), arrowSize, lineColorDef)
    }
    if (dir == "Oferece Apoio" || dir == "Mútuo") {
        drawArrow(adjustedEnd, angle, arrowSize, lineColorDef)
    }
}

fun DrawScope.drawArrow(tip: Offset, angle: Float, size: Float, color: Color) {
    val angle1 = angle + Math.PI.toFloat() * 0.8f
    val angle2 = angle - Math.PI.toFloat() * 0.8f

    val pt1 = Offset(tip.x + size * cos(angle1), tip.y + size * sin(angle1))
    val pt2 = Offset(tip.x + size * cos(angle2), tip.y + size * sin(angle2))

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(pt1.x, pt1.y)
        lineTo(pt2.x, pt2.y)
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}

fun generateEcomapaBitmap(
    networks: List<SupportNetwork>,
    nodePositions: Map<String, Offset>,
    patientName: String,
    patientRadiusPx: Float,
    networkRadiusPx: Float
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

    val padding = 400f
    val spanX = maxX - minX + padding * 2
    val spanY = maxY - minY + padding * 2

    val width = java.lang.Math.max(1200f, spanX).toInt()
    val height = java.lang.Math.max(1200f, spanY).toInt()

    val centerPx = Offset(width / 2f - (maxX + minX) / 2f, height / 2f - (maxY + minY) / 2f)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val composeCanvas = androidx.compose.ui.graphics.Canvas(bitmap.asImageBitmap())
    val canvasDrawScope = CanvasDrawScope()

    val textPaintPath = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 40f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }
    val iconPaint = android.graphics.Paint().apply {
        textSize = 70f
        textAlign = android.graphics.Paint.Align.CENTER
    }

    canvasDrawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = composeCanvas,
        size = Size(width.toFloat(), height.toFloat())
    ) {
        drawRect(Color.White)

        networks.forEach { network ->
            val relativeOffset = nodePositions[network.id]
            if (relativeOffset != null) {
                val absoluteNodeOffset = centerPx + relativeOffset
                drawEcomapaConnection(network, centerPx, absoluteNodeOffset, patientRadiusPx, networkRadiusPx)
            }
        }

        drawCircle(Color(0xFFF5F5F5), patientRadiusPx, centerPx)
        drawCircle(Color(0xFF1A1A1A), patientRadiusPx, centerPx, style = Stroke(width = 6f))
        drawContext.canvas.nativeCanvas.drawText(patientName.take(15), centerPx.x, centerPx.y + 15f, textPaintPath)

        networks.forEach { network ->
            val relativeOffset = nodePositions[network.id]
            if (relativeOffset != null) {
                val pos = centerPx + relativeOffset
                val style = getCategoryStyle(network.category)
                drawCircle(style.bg, networkRadiusPx, pos)
                val sWidth = if (network.generatesStress && network.connectionType != "Conflituosa") 10f else 6f
                val sColor = if (network.generatesStress && network.connectionType != "Conflituosa") Color(0xFFCC0000) else style.border
                drawCircle(sColor, networkRadiusPx, pos, style = Stroke(width = sWidth))

                drawContext.canvas.nativeCanvas.drawText(style.icon, pos.x, pos.y - 10f, iconPaint)
                drawContext.canvas.nativeCanvas.drawText(network.name.take(12), pos.x, pos.y + 40f, textPaintPath.apply { textSize = 26f })
                val freqStyle = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 20f
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSkewX = -0.2f
                }
                drawContext.canvas.nativeCanvas.drawText(network.contactFrequency.take(10), pos.x, pos.y + 70f, freqStyle)
            }
        }
    }
    return bitmap
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap, patientName: String) {
    val filename = "Ecomapa_${patientName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
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
        Toast.makeText(context, "Imagem salva na Galeria!", Toast.LENGTH_LONG).show()
    } ?: run {
        Toast.makeText(context, "Erro ao salvar a imagem.", Toast.LENGTH_SHORT).show()
    }
}
