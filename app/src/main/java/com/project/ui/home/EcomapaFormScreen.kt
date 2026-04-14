package com.project.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcomapaFormScreen(
    patientId: String,
    ecomapaId: String,
    viewModel: EcomapaViewModel,
    onBack: () -> Unit,
    onAddInstitution: () -> Unit,
    onEditInstitution: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Formulário - Ecomapa", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
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
                            onClick = { isMenuExpanded = false; onAddInstitution() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
                        ) {
                            Text("Adicionar Instituição", color = Color.White)
                        }
                        Button(
                            onClick = { isMenuExpanded = false; onBack() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
                        ) {
                            Text("Salvar Ecomapa", color = Color.White)
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { isMenuExpanded = !isMenuExpanded },
                    containerColor = Color(0xFF512DA8),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (isMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Menu"
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
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Redes de Apoio cadastrados",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Real Support Networks
            val networks by viewModel.supportNetworks.collectAsState()

            LaunchedEffect(ecomapaId) {
                viewModel.loadSupportNetworks(patientId, ecomapaId)
            }

            if (networks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma instituição cadastrada ainda.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                networks.forEach { network ->
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val dateString = dateFormat.format(Date(network.createdAt))
                    SupportNetworkItem(
                        title = network.name.ifBlank { "Instituição" }, 
                        date = dateString,
                        onEdit = { onEditInstitution(network.id) },
                        onDelete = { viewModel.deleteSupportNetwork(patientId, ecomapaId, network.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            
            // Preenche o espaco
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun SupportNetworkItem(
    title: String, 
    date: String,
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
                    Icons.Default.Person, // Mock icon matching the circle avatar shape
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
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
        }
    }
}
