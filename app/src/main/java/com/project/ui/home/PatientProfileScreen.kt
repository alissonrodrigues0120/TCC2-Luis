package com.project.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
    onBack: () -> Unit,
    onCreateEcomapa: () -> Unit,
    onOpenEcomapa: (String) -> Unit,
    onOpenEcomapaView: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenState by homeViewModel.screenState.collectAsState()
    
    // Refresh patient when screenState updates
    val patient = remember(screenState, patientId) {
        homeViewModel.getPatientById(patientId)
    }

    val ecomapas by ecomapaViewModel.ecomapas.collectAsState()

    LaunchedEffect(patientId) {
        ecomapaViewModel.loadEcomapas(patientId)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var ecomapaToDelete by remember { mutableStateOf<String?>(null) }
    
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil do Paciente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                            onClick = { isMenuExpanded = false; /* TODO Criar Genograma */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
                        ) {
                            Text("Criar Genograma")
                        }
                        Button(
                            onClick = { isMenuExpanded = false; onCreateEcomapa() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
                        ) {
                            Text("Criar Ecomapa")
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { isMenuExpanded = !isMenuExpanded },
                    containerColor = Color(0xFF512DA8)
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Patient Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF3E5F5),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(36.dp),
                            tint = Color(0xFF7E57C2)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name & Age
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${patient.age} anos",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Actions
                Row {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF512DA8))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
                    }
                    IconButton(onClick = { 
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
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Real Items List
            if (ecomapas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum genograma ou ecomapa criado.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                ecomapas.forEach { ecomapa ->
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val dateString = dateFormat.format(Date(ecomapa.createdAt))
                    DocumentItem(
                        title = "Ecomapa",
                        date = dateString,
                        onView = { onOpenEcomapaView(ecomapa.id) },
                        onEdit = { onOpenEcomapa(ecomapa.id) },
                        onDelete = { ecomapaToDelete = ecomapa.id }
                    )
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
    onDelete: () -> Unit
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
            Text(
                text = title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = date,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Row {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Black)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Black)
            }
            IconButton(onClick = onView) {
                Icon(Icons.Default.Search, contentDescription = "Visualizar", tint = Color.Black)
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
