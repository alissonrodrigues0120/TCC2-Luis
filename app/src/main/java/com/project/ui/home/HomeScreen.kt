package com.project.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.project.utils.ImageCompressor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.ui.components.TooltipIconButton

import com.project.data.model.Patient


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    patients: List<Patient>,
    isRefreshing: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onEditPatient: () -> Unit,
    onLogout: () -> Unit,
    onAddPatient: () -> Unit,
    onImportCsv: () -> Unit,
    onDeletePatient: (String) -> Unit,
    onPatientClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var patientToDeleteId by remember { mutableStateOf<String?>(null) }
    var editingPatient : Patient? by remember { mutableStateOf(null) }







    val purple500 = Color(0xFFB39DDB)
    val purple700 = Color(0xFF512DA8)

    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus Pacientes", fontWeight = FontWeight.Bold) },
                actions = {
                    val toggle = com.project.LocalThemeToggle.current
                    val isDark = com.project.LocalIsDarkTheme.current
                    Switch(
                        checked = isDark,
                        onCheckedChange = { toggle() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
        ) {

        // 🔹 Lista de pacientes
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (patients.isEmpty()) {
            EmptyPatientsList(onAddPatient = onAddPatient)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(patients, key = { it.id }) { patient ->
                    PatientItem(
                        patient = patient,
                        onEdit = { editingPatient = patient },
                        onDelete = { patientToDeleteId = patient.id },
                        onClick = { onPatientClick(patient.id) }
                    )
                }
            }
        }

        // 🔹 Menu expansível
        AnimatedVisibility(
            visible = isMenuExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {

                MenuButton(
                    icon = Icons.Default.ExitToApp,
                    text = "Desconectar",
                    backgroundColor = purple700,
                    onClick = {
                        isMenuExpanded = false
                        showLogoutDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                MenuButton(
                    icon = Icons.Default.Share,
                    text = "Importar Paciente",
                    backgroundColor = purple500,
                    onClick = {
                        isMenuExpanded = false
                        onImportCsv()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                MenuButton(
                    icon = Icons.Default.Add,
                    text = "Adicionar",
                    backgroundColor = purple500,
                    onClick = {
                        isMenuExpanded = false
                        onAddPatient()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                FloatingActionButton(
                    onClick = { isMenuExpanded = false },
                    containerColor = purple500
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar menu")
                }
            }
        }

        // 🔹 FAB principal
        if (!isMenuExpanded) {
            FloatingActionButton(
                onClick = { isMenuExpanded = true },
                containerColor = purple500,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Abrir menu")
            }
        }

        // 🔹 Dialog logout
        if (showLogoutDialog) {
            LogoutConfirmationDialog(
                onConfirm = {
                    showLogoutDialog = false
                    onLogout()
                },
                onDismiss = { showLogoutDialog = false }
            )
        }

        if (editingPatient != null) {
            EditPatientDialog(
                patient = editingPatient,
                onDismiss = { editingPatient = null },
                onSave = {
                    homeViewModel.updatePatient(it)
                    editingPatient = null
                }
            )
        }



        // 🔹 Dialog excluir paciente
        patientToDeleteId?.let { id ->
            val patient = patients.find { it.id == id }
            if (patient != null) {
                DeleteConfirmationDialog(
                    patientName = patient.name,
                    onConfirm = {
                        onDeletePatient(id)
                        patientToDeleteId = null
                    },
                    onDismiss = { patientToDeleteId = null }
                )
            }
        }
    }
    }
}


@Composable
fun EditPatientDialog(
    patient: Patient?,
    onDismiss: () -> Unit,
    onSave: (Patient) -> Unit
) {
    var name by remember { mutableStateOf(patient?.name ?: "") }
    var age by remember { mutableStateOf(patient?.age?.toString() ?: "") }
    var gender by remember { mutableStateOf(patient?.gender ?: "Masculino") }
    var condition by remember { mutableStateOf(patient?.condition ?: "") }
    var observations by remember { mutableStateOf(patient?.observations ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                patient?.let {
                    onSave(
                        it.copy(
                            name = name,
                            age = age.toIntOrNull() ?: it.age,
                            gender = gender,
                            condition = condition,
                            observations = observations
                        )
                    )
                }
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = {
            Text("Editar paciente")
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name.toString(),
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Idade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                GenderDropdown(
                    selectedGender = gender,
                    onGenderSelected = { gender = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                ConditionDropdown(
                    selectedCondition = condition,
                    onConditionSelected = { condition = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                OutlinedTextField(
                    value = observations,
                    onValueChange = { observations = it },
                    label = { Text("Observações") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(100.dp),
                    maxLines = 4
                )
            }
        }
    )
}


@Composable
private fun EmptyPatientsList(onAddPatient: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Sem pacientes",
                tint = Color.Gray,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nenhum paciente cadastrado",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Toque no botão + para adicionar seu primeiro paciente",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddPatient,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB39DDB))
            ) {
                Row {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Paciente", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PatientItem(
    patient: Patient,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color(0xFFF3E5F5),
                modifier = Modifier.size(50.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (patient.photoBase64.isNotBlank()) {
                        AsyncImage(
                            model = ImageCompressor.decodeBase64ToByteArray(patient.photoBase64),
                            contentDescription = "Foto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Paciente",
                            tint = Color(0xFF7E57C2),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${patient.age} anos • ${patient.condition}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            TooltipIconButton(tooltipText = "Editar", onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = Color(0xFF512DA8)
                )
            }

            TooltipIconButton(tooltipText = "Excluir", onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
private fun MenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .width(200.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar logout") },
        text = { Text("Você realmente deseja sair da aplicação?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB39DDB))
            ) {
                Text("Sim, sair")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(
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