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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode.Companion.Screen
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.project.data.model.Patient
import com.project.di.ViewModelFactory
import com.project.ui.home.AddPatientScreen
import com.project.ui.home.HomeScreen
import com.project.ui.home.HomeViewModel
import com.project.ui.login.AuthScreen
import com.project.ui.login.LoginScreen
import com.project.ui.login.RegisterScreen
import com.project.ui.login.ResetPasswordScreen
import com.project.ui.theme.TcctwoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = FirebaseApp.initializeApp(this)
            ?: FirebaseApp.getInstance()

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        FirebaseFirestore.getInstance(app).firestoreSettings = settings


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
    var isEditingPatient by rememberSaveable { mutableStateOf(false) }


    val homeViewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory()
    )

    val state by homeViewModel.screenState.collectAsState()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = {
                        currentDestination = it
                        isAddingPatient = false
                        isEditingPatient = false
                    }
                )


            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> {
                    if (isAddingPatient) {
                        AddPatientScreen(
                            onBack = { isAddingPatient = false },
                            onSave = { newPatient : Patient->
                                homeViewModel.addPatient(newPatient) {
                                    isAddingPatient = false
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        HomeScreen(
                            patients = state.patients,
                            isRefreshing = state.isRefreshing,
                            onRefresh = { homeViewModel.refresh() },
                            onLogout = onLogout,
                            onAddPatient = { isAddingPatient = true },
                            onEditPatient = { isEditingPatient = true },
                            onImportCsv = {},
                            onDeletePatient = { homeViewModel.deletePatient(it) },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                AppDestinations.PROFILE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Seção de Perfil em construção",
                            style = MaterialTheme.typography.titleLarge
                        )
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
    PROFILE("Profile", Icons.Default.AccountBox),
}