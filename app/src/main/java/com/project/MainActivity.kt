package com.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.project.ui.home.AddPatientScreen
import com.project.ui.home.HomeScreen
import com.project.ui.home.HomeViewModel
import com.project.ui.home.Patient
import com.project.ui.login.AuthScreen
import com.project.ui.login.LoginScreen
import com.project.ui.login.RegisterScreen
import com.project.ui.login.ResetPasswordScreen
import com.project.ui.theme.TcctwoTheme
import kotlin.properties.ReadOnlyProperty

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializa Firebase
        FirebaseApp.initializeApp(this)

        setContent {
            TcctwoTheme {
                TcctwoApp()
            }
        }
    }
}

@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    var currentDestination by rememberSaveable {
        mutableStateOf(AppDestinations.HOME)
    }
    var isAddingPatient by rememberSaveable { mutableStateOf(false) }

    // Estado compartilhado da lista de pacientes
    val patients = rememberSaveable { mutableStateListOf<Patient>() }
    val homeViewModel: HomeViewModel = viewModel()

    // Carrega pacientes quando a tela é criada
    LaunchedEffect(Unit) {
        homeViewModel.loadPatients()
    }


    val patientsState by homeViewModel.patients.collectAsState()
    LaunchedEffect(patientsState) {
        patients.clear()
        patients.addAll(patientsState)
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = {
                        currentDestination = it
                        isAddingPatient = false // Fecha formulário ao trocar aba
                    }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // Navegação entre telas principais
            when (currentDestination) {
                AppDestinations.HOME -> {
                    if (isAddingPatient) {
                        // Formulário de adição de paciente
                        AddPatientScreen(
                            onBack = { isAddingPatient = false },
                            onSave = { newPatient ->
                                homeViewModel.addPatient(newPatient) {
                                    isAddingPatient = false
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        // HomeScreen principal
                        HomeScreen(
                            patients = patients,
                            onLogout = onLogout,
                            onAddPatient = { isAddingPatient = true },
                            onImportCsv = {
                                // Implementar lógica de importação
                            },
                            onEditPatient = { patient ->
                                // Implementar edição
                            },
                            onDeletePatient = { patientId ->
                                homeViewModel.deletePatient(patientId)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                AppDestinations.FAVORITES -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Seção de Favoritos em construção", style = MaterialTheme.typography.titleLarge)
                    }
                }

                AppDestinations.PROFILE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Seção de Perfil em construção", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}



@Composable
fun TcctwoApp() {
    // Estado persistente de login (sobrevive a rotações)
    var isLoggedIn by rememberSaveable { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
    var authScreen by rememberSaveable { mutableStateOf(AuthScreen.LOGIN) }

    // Verifica login persistente ao iniciar
    LaunchedEffect(Unit) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            isLoggedIn = true
        }
    }

    when {
        isLoggedIn -> {
            MainScreen(onLogout = {
                FirebaseAuth.getInstance().signOut()
                isLoggedIn = false
                authScreen = AuthScreen.LOGIN
            })
        }

        authScreen == AuthScreen.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    // ✅ ESTA É A LINHA QUE FAZ A TRANSIÇÃO!
                    isLoggedIn = true
                },
                onRegisterClick = {
                    authScreen = AuthScreen.REGISTER
                },
                onForgotPasswordClick = {
                    authScreen = AuthScreen.RESET_PASSWORD
                },
                viewModel = viewModel()
            )
        }

        authScreen == AuthScreen.REGISTER -> {
            RegisterScreen(
                onRegisterSuccess = {
                    authScreen = AuthScreen.LOGIN
                },
                onBackToLogin = {
                    authScreen = AuthScreen.LOGIN
                }
            )
        }

        authScreen == AuthScreen.RESET_PASSWORD -> {
            ResetPasswordScreen(
                onBackToLogin = {
                    authScreen = AuthScreen.LOGIN
                }
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}