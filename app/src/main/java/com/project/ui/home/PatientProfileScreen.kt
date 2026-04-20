package com.project.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Add
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.ui.components.TooltipIconButton

import com.project.data.model.Patient
import com.project.ui.home.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(
    patientId: String,
    homeViewModel: HomeViewModel,
    ecomapaViewModel: EcomapaViewModel,
    genogramaViewModel: com.project.ui.home.GenogramaViewModel,
    onBack: () -> Unit,
    onCreateEcomapa: () -> Unit,
    onCreateGenograma: (com.project.data.model.Patient) -> Unit,
    onOpenEcomapa: (String) -> Unit,
    onOpenGenograma: (String) -> Unit,
    onOpenEcomapaView: (String) -> Unit,
    onOpenGenogramaView: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenState by homeViewModel.screenState.collectAsState()
    
    // Refresh patient when screenState updates
    val patient = remember(screenState, patientId) {
        homeViewModel.getPatientById(patientId)
    }

    val ecomapas by ecomapaViewModel.ecomapas.collectAsState()
    val genogramasState by genogramaViewModel.state.collectAsState()

    LaunchedEffect(patientId) {
        ecomapaViewModel.loadEcomapas(patientId)
        genogramaViewModel.loadGenogramas(patientId)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var ecomapaToDelete by remember { mutableStateOf<String?>(null) }
    var genogramaToDelete by remember { mutableStateOf<String?>(null) }
    
    var showObsDialog by remember { mutableStateOf(false) }
    var inlineObsText by remember { mutableStateOf("") }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (patient == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Paciente não encontrado.")
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                Text("Voltar")
            }
        }
        return
    }

    if (showEditDialog) {
        EditPatientDialog(
            patient = patient,
            onDismiss = { showEditDialog = false },
            onSave = { updatedPatient ->
                homeViewModel.updatePatient(updatedPatient)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        DeleteProfileConfirmationDialog(
            patientName = patient.name,
            onConfirm = {
                homeViewModel.deletePatient(patient.id)
                showDeleteDialog = false
                onBack() // Volta pra home após deletar
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (ecomapaToDelete != null) {
        AlertDialog(
            onDismissRequest = { ecomapaToDelete = null },
            title = { Text("Excluir Ecomapa") },
            text = { Text("Tem certeza que deseja excluir permanentemente este ecomapa e todas as suas redes de apoio associadas?") },
            confirmButton = {
                Button(
                    onClick = {
                        ecomapaViewModel.deleteEcomapa(patientId, ecomapaToDelete!!)
                        ecomapaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                Button(onClick = { ecomapaToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (genogramaToDelete != null) {
        AlertDialog(
            onDismissRequest = { genogramaToDelete = null },
            title = { Text("Excluir Genograma") },
            text = { Text("Tem certeza que deseja excluir permanentemente este genograma?") },
            confirmButton = {
                Button(
                    onClick = {
                        genogramaViewModel.deleteGenograma(patientId, genogramaToDelete!!)
                        genogramaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                Button(onClick = { genogramaToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showObsDialog) {
        AlertDialog(
            onDismissRequest = { showObsDialog = false },
            title = { Text("Editar Observações") },
            text = {
                OutlinedTextField(
                    value = inlineObsText,
                    onValueChange = { inlineObsText = it },
                    label = { Text("Anotações Clínicas") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    maxLines = 6
                )
            },
            confirmButton = {
                Button(onClick = {
                    patient?.let {
                        homeViewModel.updatePatient(it.copy(observations = inlineObsText))
                    }
                    showObsDialog = false
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showObsDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil do Paciente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TooltipIconButton(tooltipText = "Voltar", onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    val toggle = com.project.LocalThemeToggle.current
                    val isDark = com.project.LocalIsDarkTheme.current
                    Switch(
                        checked = isDark,
                        onCheckedChange = { toggle() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
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
                        Button(
                            onClick = { 
                                isMenuExpanded = false 
                                patient?.let { onCreateGenograma(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Criar Genograma")
                        }
                        Button(
                            onClick = { isMenuExpanded = false; onCreateEcomapa() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Criar Ecomapa")
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { isMenuExpanded = !isMenuExpanded },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (isMenuExpanded) Icons.Default.Close else Icons.Default.Add, 
                        contentDescription = "Mais",
                        tint = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Patient Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    val context = LocalContext.current
                    var showImagePicker by remember { mutableStateOf(false) }
                    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }
                    
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF3E5F5),
                        modifier = Modifier
                            .size(80.dp)
                            .clickable {
                                tempUri = com.project.utils.ImageCompressor.createTempImageUri(context)
                                showImagePicker = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (patient.photoBase64.isNotBlank()) {
                                AsyncImage(
                                    model = com.project.utils.ImageCompressor.decodeBase64ToByteArray(patient.photoBase64),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF7E57C2)
                                )
                            }
                        }
                    }
                    
                    // Camera icon badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(28.dp)
                            .offset(x = 4.dp, y = 4.dp)
                            .clickable {
                                tempUri = com.project.utils.ImageCompressor.createTempImageUri(context)
                                showImagePicker = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Mudar Foto", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    if (showImagePicker && tempUri != null) {
                        ImagePickerDialog(
                            showDialog = showImagePicker,
                            onDismiss = { showImagePicker = false },
                            onImageSelected = { selectedUri ->
                                val base64 = com.project.utils.ImageCompressor.compressAndEncodeToBase64(context, selectedUri)
                                if (base64 != null) {
                                    homeViewModel.updatePatient(patient.copy(photoBase64 = base64))
                                }
                            },
                            tempImageUri = tempUri!!
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name & Age
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${patient.age} anos • ${patient.gender}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    var expandedCondition by remember { mutableStateOf(false) }
                    Box {
                        Text(
                            text = "Condição: ${patient.condition} ▾",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { expandedCondition = true }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                        DropdownMenu(
                            expanded = expandedCondition,
                            onDismissRequest = { expandedCondition = false }
                        ) {
                            listOf("Em tratamento", "Alta", "Desistiu").forEach { cond ->
                                DropdownMenuItem(
                                    text = { Text(cond) },
                                    onClick = { 
                                        expandedCondition = false
                                        homeViewModel.updatePatient(patient.copy(condition = cond)) 
                                    }
                                )
                            }
                        }
                    }
                }

                // Actions
                Row {
                    TooltipIconButton(tooltipText = "Editar", onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF512DA8))
                    }
                    TooltipIconButton(tooltipText = "Excluir", onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
                    }
                    TooltipIconButton(tooltipText = "Compartilhar", onClick = { 
                        coroutineScope.launch {
                            val syncManager = com.project.data.repository.DataSyncManager(context)
                            val (ecomapas, networks) = ecomapaViewModel.exportEcomapasData(patientId)
                            val uri = syncManager.exportPatientData(patient, ecomapas, networks)
                            if (uri != null) {
                                syncManager.shareExportedFile(uri)
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (patient.observations.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { 
                            inlineObsText = patient.observations
                            showObsDialog = true 
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Observações",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Edit, contentDescription = "Editar Observações", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = patient.observations,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { 
                        inlineObsText = ""
                        showObsDialog = true 
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Observação", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Observação Inicial")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            val pagerState = rememberPagerState(pageCount = { 2 })
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                divider = { }
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Ecomapas") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Genogramas") }
                )
            }

            var itemToDuplicate by remember { mutableStateOf<Triple<String, String, String>?>(null) } // Triple(Type, Id, Title)

            if (itemToDuplicate != null) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { itemToDuplicate = null },
                    title = { Text("Duplicar Documento") },
                    text = { Text("Deseja criar uma cópia chamada '${itemToDuplicate!!.third} Cópia'?") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            val (type, id, title) = itemToDuplicate!!
                            if (type == "Ecomapa") {
                                ecomapaViewModel.duplicateEcomapa(patientId, id, title)
                            } else {
                                genogramaViewModel.duplicateGenograma(patientId, id, title)
                            }
                            itemToDuplicate = null
                        }) { Text("Copiar") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { itemToDuplicate = null }) { Text("Cancelar") }
                    }
                )
            }

            var itemToRename by remember { mutableStateOf<Triple<String, String, String>?>(null) }
            var newTitleName by remember { mutableStateOf("") }

            if (itemToRename != null) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { itemToRename = null },
                    title = { Text("Renomear Documento") },
                    text = { 
                        OutlinedTextField(
                            value = newTitleName,
                            onValueChange = { newTitleName = it },
                            label = { Text("Novo Título") },
                            singleLine = true
                        ) 
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            val (type, id, _) = itemToRename!!
                            if (newTitleName.isNotBlank()) {
                                if (type == "Ecomapa") {
                                    ecomapaViewModel.renameEcomapa(patientId, id, newTitleName)
                                } else {
                                    genogramaViewModel.renameGenograma(patientId, id, newTitleName)
                                }
                            }
                            itemToRename = null
                        }) { Text("Salvar") }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { itemToRename = null }) { Text("Cancelar") }
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(550.dp)
            ) { page ->
                when (page) {
                    0 -> {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (ecomapas.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Nenhum ecomapa criado.", fontSize = 14.sp, color = Color.Gray)
                                    }
                                }
                            } else {
                                items(ecomapas.size) { index ->
                                    val ecomapa = ecomapas[index]
                                    val ecomapaTitle = ecomapa.title.ifBlank { "Ecomapa Clínico" }
                                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    val creationString = dateFormat.format(Date(ecomapa.createdAt))
                                    val updatedString = dateFormat.format(Date(ecomapa.updatedAt))
                                    val dateString = "Criado em: $creationString | Editado: $updatedString"
                                    DocumentItem(
                                        title = ecomapaTitle,
                                        date = dateString,
                                        onView = { onOpenEcomapaView(ecomapa.id) },
                                        onEdit = { onOpenEcomapa(ecomapa.id) },
                                        onDelete = { ecomapaToDelete = ecomapa.id },
                                        onDuplicate = { 
                                            itemToDuplicate = Triple("Ecomapa", ecomapa.id, ecomapaTitle)
                                        },
                                        onRename = {
                                            newTitleName = ecomapaTitle
                                            itemToRename = Triple("Ecomapa", ecomapa.id, ecomapaTitle)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (genogramasState.genogramas.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Nenhum genograma criado.", fontSize = 14.sp, color = Color.Gray)
                                    }
                                }
                            } else {
                                items(genogramasState.genogramas.size) { index ->
                                    val genograma = genogramasState.genogramas[index]
                                    val genogramaTitle = genograma.title.ifBlank { "Genograma Clínico" }
                                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    val creationString = dateFormat.format(Date(genograma.createdAt))
                                    val updatedString = dateFormat.format(Date(genograma.updatedAt))
                                    val dateString = "Criado em: $creationString | Editado: $updatedString"
                                    DocumentItem(
                                        title = genogramaTitle,
                                        date = dateString,
                                        onView = { onOpenGenogramaView(genograma.id) },
                                        onEdit = { onOpenGenograma(genograma.id) },
                                        onDelete = { genogramaToDelete = genograma.id },
                                        onDuplicate = { 
                                            itemToDuplicate = Triple("Genograma", genograma.id, genogramaTitle)
                                        },
                                        onRename = {
                                            newTitleName = genogramaTitle
                                            itemToRename = Triple("Genograma", genograma.id, genogramaTitle)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentItem(
    title: String, 
    date: String,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person, // Mock icon
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onRename() }) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Edit, contentDescription = "Renomear", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Text(
                text = date,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Row {
            TooltipIconButton(tooltipText = "Duplicar", onClick = onDuplicate) {
                Icon(Icons.Default.Add, contentDescription = "Copiar", tint = MaterialTheme.colorScheme.onSurface)
            }
            TooltipIconButton(tooltipText = "Editar", onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.onSurface)
            }
            TooltipIconButton(tooltipText = "Excluir", onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
            }
            TooltipIconButton(tooltipText = "Visualizar", onClick = onView) {
                Icon(Icons.Default.Search, contentDescription = "Visualizar", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// Extracted from HomeScreen just to not mess with visibility if it's too much trouble
@Composable
fun DeleteProfileConfirmationDialog(
    patientName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir paciente") },
        text = { Text("Tem certeza que deseja excluir $patientName?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Excluir")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
