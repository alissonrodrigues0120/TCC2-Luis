package com.project.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.data.model.Patient
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.foundation.layout.width
import com.project.ui.components.TooltipIconButton

import java.util.Calendar
import java.util.UUID




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(
    onBack: () -> Unit,
    onSave: (Patient) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("0") }
    var gender by remember { mutableStateOf("Masculino") }
    var condition by remember { mutableStateOf("Em tratamento") }
    var observations by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var DialogSave by remember { mutableStateOf(false) }
    var photoBase64 by remember { mutableStateOf("") }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Paciente") },
                navigationIcon = {
                    TooltipIconButton(tooltipText = "Voltar", onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Seleção de Foto
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val context = LocalContext.current
                    var showImagePicker by remember { mutableStateOf(false) }
                    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }
                    
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .clickable {
                                tempUri = com.project.utils.ImageCompressor.createTempImageUri(context)
                                showImagePicker = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoBase64.isNotBlank()) {
                            val decodedBytes = com.project.utils.ImageCompressor.decodeBase64ToByteArray(photoBase64)
                            AsyncImage(
                                model = decodedBytes,
                                contentDescription = "Foto",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
                        }
                    }
                    
                    if (showImagePicker && tempUri != null) {
                        ImagePickerDialog(
                            showDialog = showImagePicker,
                            onDismiss = { showImagePicker = false },
                            onImageSelected = { selectedUri ->
                                val base64 = com.project.utils.ImageCompressor.compressAndEncodeToBase64(context, selectedUri)
                                if (base64 != null) {
                                    photoBase64 = base64
                                }
                            },
                            tempImageUri = tempUri!!
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                // Campo Nom
                // e
                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue ->
                        name = newValue.filter { it.isLetter() || it.isWhitespace() }
                        nameError = false
                    },
                    label = { Text("Nome") },
                    placeholder = { Text("Digite o nome") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError
                )

                if (nameError) {
                    Text(
                        text = "Nome é obrigatório",
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Data de Nascimento e Idade Calculada
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    var showDatePicker by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = {},
                        label = { Text("Data Nasc.") },
                        placeholder = { Text("15/02/1990") },
                        modifier = Modifier.weight(0.45f),
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
                                        val dateStr = com.project.ui.home.DateUtils.formatMillisToDateString(millis)
                                        birthDate = dateStr
                                        
                                        // Inline Age Calculation
                                        val cal = Calendar.getInstance()
                                        cal.timeInMillis = millis
                                        val today = Calendar.getInstance()
                                        var calcAge = today.get(Calendar.YEAR) - cal.get(Calendar.YEAR)
                                        if (today.get(Calendar.DAY_OF_YEAR) < cal.get(Calendar.DAY_OF_YEAR)) {
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
                        onValueChange = {},
                        label = { Text("Idade") },
                        modifier = Modifier.weight(0.3f),
                        readOnly = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gênero e Condição
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GenderDropdown(
                        selectedGender = gender,
                        onGenderSelected = { gender = it },
                        modifier = Modifier.weight(0.45f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    ConditionDropdown(
                        selectedCondition = condition,
                        onConditionSelected = { condition = it },
                        modifier = Modifier.weight(0.45f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Observações
                OutlinedTextField(
                    value = observations,
                    onValueChange = { observations = it },
                    label = { Text("Observações") },
                    placeholder = { Text("Digite as observações...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4,
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botão Salvar
                Button(
                    onClick = {
                        val hasNameError = name.isEmpty()
                        nameError = hasNameError

                        if (!hasNameError) {
                            onSave(
                                Patient(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    age = age.toIntOrNull() ?: 0,
                                    gender = gender,
                                    condition = condition,
                                    birthDate = birthDate,
                                    observations = observations,
                                    photoBase64 = photoBase64
                                )
                            )

                           DialogSave = true
                        }


                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB39DDB))
                ) {
                    Text("Salvar", color = Color.White)
                }

                if(DialogSave){
                   ShowSavePatientToast(message = "Paciente salvo com sucesso!", context = LocalContext.current)
                }
            }
        }
    )
}


private fun getAgeErrorState(age: String): Boolean {
    return when {
        age.isEmpty() -> true
        age.toIntOrNull() == null -> true
        age.toInt() < 1 -> true
        age.toInt() > 120 -> true
        else -> false
    }
}

@Composable
private fun getAgeErrorMessage(age: String): String? {
    return when {
        age.isEmpty() -> "Idade é obrigatória"
        age.toIntOrNull() == null -> "Valor inválido"
        age.toInt() < 1 -> "Idade deve ser maior que 0"
        age.toInt() > 120 -> "Idade máxima é 120 anos"
        else -> null
    }
}



@Composable
private fun ShowSavePatientToast(
    message: String,
    context: Context
) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropdown(
    selectedGender: String,
    onGenderSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val genders = listOf("Masculino", "Feminino", "Outro")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedGender,
            onValueChange = { },
            label = { Text("Gênero") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier.menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            genders.forEach { gender ->
                DropdownMenuItem(
                    text = { Text(gender) },
                    onClick = {
                        onGenderSelected(gender)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionDropdown(
    selectedCondition: String,
    onConditionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val conditions = listOf("Em tratamento", "Alta", "Estável", "Grave")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedCondition,
            onValueChange = { },
            label = { Text("Condição") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier.menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            conditions.forEach { cond ->
                DropdownMenuItem(
                    text = { Text(cond) },
                    onClick = {
                        onConditionSelected(cond)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}