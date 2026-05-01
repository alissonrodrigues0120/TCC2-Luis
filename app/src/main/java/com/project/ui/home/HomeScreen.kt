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
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.ui.components.TooltipIconButton

import com.project.data.model.Patient


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    userName: String,
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







    val homeTitle = userName.trim().takeIf { it.isNotBlank() }?.let { "Pacientes de $it" } ?: "Pacientes"

    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = homeTitle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = isMenuExpanded,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        MenuButton(
                            icon = Icons.Default.ExitToApp,
                            text = "Desconectar",
                            onClick = {
                                isMenuExpanded = false
                                showLogoutDialog = true
                            }
                        )

                        MenuButton(
                            icon = Icons.Default.Share,
                            text = "Importar Paciente",
                            onClick = {
                                isMenuExpanded = false
                                onImportCsv()
                            }
                        )

                        MenuButton(
                            icon = Icons.Default.Add,
                            text = "Adicionar",
                            onClick = {
                                isMenuExpanded = false
                                onAddPatient()
                            }
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { isMenuExpanded = !isMenuExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = if (isMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isMenuExpanded) "Fechar menu" else "Abrir menu"
                    )
                }
            }
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPatientDialog(
    patient: Patient?,
    onDismiss: () -> Unit,
    onSave: (Patient) -> Unit
) {
    var name by remember { mutableStateOf(patient?.name ?: "") }
    var age by remember { mutableStateOf(patient?.age?.toString() ?: "") }
    var gender by remember { mutableStateOf(patient?.gender ?: "Masculino") }
    var birthDate by remember { mutableStateOf(patient?.birthDate ?: "") }

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
                            birthDate = birthDate
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showDatePicker by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = {},
                        label = { Text("Data Nasc.") },
                        placeholder = { Text("15/02/1990") },
                        modifier = Modifier.weight(0.45f).padding(top = 8.dp),
                        readOnly = true,
                        trailingIcon = {
                            TooltipIconButton(tooltipText = "Calendário", onClick = { showDatePicker = true }) {
                                Icon(androidx.compose.material.icons.Icons.Default.DateRange, contentDescription = "Selecionar Data")
                            }
                        }
                    )

                    if (showDatePicker) {
                        val datePickerState = androidx.compose.material3.rememberDatePickerState()
                        androidx.compose.material3.DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        val dateStr = DateUtils.formatMillisToDateString(millis)
                                        birthDate = dateStr
                                        
                                        val cal = java.util.Calendar.getInstance()
                                        cal.timeInMillis = millis
                                        val today = java.util.Calendar.getInstance()
                                        var calcAge = today.get(java.util.Calendar.YEAR) - cal.get(java.util.Calendar.YEAR)
                                        if (today.get(java.util.Calendar.DAY_OF_YEAR) < cal.get(java.util.Calendar.DAY_OF_YEAR)) {
                                            calcAge--
                                        }
                                        age = if (calcAge < 0) "0" else calcAge.toString()
                                    }
                                    showDatePicker = false
                                }) { Text("OK") }
                            },
                        ) {
                            androidx.compose.material3.DatePicker(state = datePickerState)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Idade") },
                        modifier = Modifier.weight(0.3f).padding(top = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                GenderDropdown(
                    selectedGender = gender,
                    onGenderSelected = { gender = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Paciente")
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
                            tint = MaterialTheme.colorScheme.primary,
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
                
                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                val createdStr = dateFormat.format(java.util.Date(patient.createdAt))
                val updatedStr = dateFormat.format(java.util.Date(patient.remoteLastUpdate))
                Text(
                    text = "Criado: $createdStr | Atualizado: $updatedStr",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            TooltipIconButton(tooltipText = "Editar", onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            TooltipIconButton(tooltipText = "Excluir", onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .width(200.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
