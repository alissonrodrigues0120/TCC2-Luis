package com.project.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.data.model.SupportNetwork
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcomapaViewScreen(
    patientId: String,
    ecomapaId: String,
    viewModel: EcomapaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val networks by viewModel.supportNetworks.collectAsState()

    LaunchedEffect(ecomapaId) {
        viewModel.loadSupportNetworks(patientId, ecomapaId)
    }

    // Modal state for Tap details
    var selectedNetwork by remember { mutableStateOf<SupportNetwork?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Engine States for Node Relative Positions (in pixels from Center)
    val nodePositions = remember { mutableStateMapOf<String, Offset>() }

    // Camera (Pan/Zoom) States
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    // Constants for sizes
    val patientRadiusPx = 140f
    val networkRadiusPx = 120f

    // Trigonometric spawn calculation (Run once or when networks expand)
    LaunchedEffect(networks) {
        if (networks.isNotEmpty() && nodePositions.isEmpty()) {
            val orbitRadius = 450f // safe relative spawn radius
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
                title = { Text("Ecomapa Visual", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9F9FB)) // Fundo pastel claro
        ) {
            if (networks.isEmpty()) {
                Text(
                    text = "Não há redes de apoio para gerar o Ecomapa.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                val density = LocalDensity.current
                val centerPx = remember(maxWidth, maxHeight) {
                    with(density) { Offset(maxWidth.toPx() / 2f, maxHeight.toPx() / 2f) }
                }

                // Main Camera Controller Box
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
                    // Visual World Wrapper
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = pan.x,
                                translationY = pan.y
                            )
                    ) {
                        // 1. Draw solid Canvas Lines underneath everything
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

                        // 2. Draw Patient (Center) Using Modern Compose Box
                        NodeCircle(
                            label = "Paciente",
                            backgroundColor = Color(0xFF67E34D),
                            borderColor = Color(0xFF3BA724),
                            offsetX = centerPx.x,
                            offsetY = centerPx.y,
                            radiusPx = patientRadiusPx,
                            density = density,
                            onClick = { /* Does nothing directly */ }
                        )

                        // 3. Draw Institutions
                        networks.forEach { network ->
                            val relativeOffset = nodePositions[network.id]
                            if (relativeOffset != null) {
                                val absoluteNodeOffset = centerPx + relativeOffset
                                
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                x = (absoluteNodeOffset.x - networkRadiusPx).roundToInt(),
                                                y = (absoluteNodeOffset.y - networkRadiusPx).roundToInt()
                                            )
                                        }
                                        .size(with(density) { (networkRadiusPx * 2).toDp() })
                                        .clip(CircleShape)
                                        .background(Color(0xFF7E57C2))
                                        .pointerInput(network.id) {
                                            detectDragGestures(
                                                onDrag = { change, dragAmount ->
                                                    change.consume() // Impede a câmera do mapa de mover quando draga bolinha
                                                    val currentPos = nodePositions[network.id] ?: Offset.Zero
                                                    nodePositions[network.id] = currentPos + dragAmount
                                                }
                                            )
                                        }
                                        .clickable {
                                            selectedNetwork = network
                                            showDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Borda Manual via Box interna (opcional para dar mais robustez visual)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Transparent)
                                            .padding(3.dp)
                                    )

                                    val shortName = network.name.take(15).ifEmpty { "Instituição" }
                                    Text(
                                        text = shortName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Details Info
    if (showDialog && selectedNetwork != null) {
        val n = selectedNetwork!!
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = n.name.ifEmpty { "Instituição" }, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Categoria: ${n.category}")
                    Text("Frequência: ${n.contactFrequency}")
                    Text("Conexão: ${n.connectionType}")
                    Text("Direção: ${n.supportDirection}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Descrição: ${n.description}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}

@Composable
fun NodeCircle(
    label: String,
    backgroundColor: Color,
    borderColor: Color,
    offsetX: Float,
    offsetY: Float,
    radiusPx: Float,
    density: androidx.compose.ui.unit.Density,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (offsetX - radiusPx).roundToInt(),
                    y = (offsetY - radiusPx).roundToInt()
                )
            }
            .size(with(density) { (radiusPx * 2).toDp() })
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// Logic to draw lines based on Ecomapa theory
fun DrawScope.drawEcomapaConnection(network: SupportNetwork, start: Offset, end: Offset, paddingCenter: Float, paddingNode: Float) {
    val distance = Math.hypot((end.x - start.x).toDouble(), (end.y - start.y).toDouble()).toFloat()
    if (distance == 0f) return

    val dx = (end.x - start.x) / distance
    val dy = (end.y - start.y) / distance

    val adjustedStart = Offset(start.x + dx * paddingCenter, start.y + dy * paddingCenter)
    val adjustedEnd = Offset(end.x - dx * paddingNode, end.y - dy * paddingNode)

    var strokeWidth = 5f
    var pathEffect: PathEffect? = null
    var lineColor = Color.Gray

    // "Forte", "Fraca", "Conflituosa", "Neutra", "Compensatória"
    when (network.connectionType) {
        "Forte" -> {
            strokeWidth = 14f
            lineColor = Color(0xFF2E7D32)
        }
        "Fraca" -> {
            strokeWidth = 8f
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f), 0f)
            lineColor = Color.LightGray
        }
        "Conflituosa" -> {
            strokeWidth = 12f
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f) // Simulated zig-zag
            lineColor = Color(0xFFC62828)
        }
        "Compensatória" -> {
            strokeWidth = 8f
            lineColor = Color(0xFF1565C0)
        }
        "Neutra" -> {
            strokeWidth = 6f
            lineColor = Color.Gray
        }
    }

    drawLine(
        color = lineColor,
        start = adjustedStart,
        end = adjustedEnd,
        strokeWidth = strokeWidth,
        pathEffect = pathEffect
    )
}
