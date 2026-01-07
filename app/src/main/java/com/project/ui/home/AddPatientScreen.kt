package com.project.ui.home

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(
    onBack: () -> Unit,
    onSave: (Patient) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Masculino") }
    var observations by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var ageError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Paciente") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                // Campo Nome
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
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

                // Idade e Gênero
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Campo Idade CORRIGIDO
                    Column(modifier = Modifier.weight(0.45f)) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { newValue ->
                                val filtered = newValue.filter { char -> char.isDigit() }
                                age = filtered
                                if (filtered.isNotEmpty()) {
                                    ageError = false
                                }
                            },
                            label = { Text("Idade") },
                            placeholder = { Text("Digite a idade") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            isError = getAgeErrorState(age),
                            singleLine = true
                        )

                        getAgeErrorMessage(age)?.let { errorMessage ->
                            Text(
                                text = errorMessage,
                                color = Color.Red,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Campo Gênero
                    GenderDropdown(
                        selectedGender = gender,
                        onGenderSelected = { gender = it },
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
                        val hasAgeError = getAgeErrorState(age)

                        nameError = hasNameError
                        ageError = hasAgeError

                        if (!hasNameError && !hasAgeError) {
                            onSave(
                                Patient(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    age = age.toInt(),
                                    gender = gender,
                                    condition = "Em tratamento"
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB39DDB))
                ) {
                    Text("Salvar", color = Color.White)
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