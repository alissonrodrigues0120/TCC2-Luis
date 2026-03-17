package com.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.project.data.repository.PatientRepository
import com.project.ui.home.AddPatientScreen
import com.project.ui.home.HomeScreen
import com.project.ui.home.HomeViewModel
import com.project.ui.home.HomeViewModelFactory
import com.project.ui.login.AuthScreen
import com.project.ui.login.LoginScreen
import com.project.ui.login.RegisterScreen
import com.project.ui.login.ResetPasswordScreen
import com.project.ui.theme.TcctwoTheme

class MainActivity : ComponentActivity() {

    private val viewModelFactory: ViewModelProvider.Factory by lazy {
        // ✅ Obtém o UID do usuário logado
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // ✅ Se não estiver logado, o Repository vai lidar com isso gracefully
        val repository = PatientRepository(userId)
        HomeViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseApp.initializeApp(this) ?: FirebaseApp.getInstance()

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings

        setContent {
            TcctwoTheme {
                TcctwoApp(viewModelFactory = viewModelFactory)
            }
        }
    }
}

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val state by homeViewModel.screenState.collectAsState()

    Scaffold(modifier = modifier) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestinations.HOME.route) {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    patients = state.patients,
                    isRefreshing = state.isRefreshing,
                    onRefresh = { homeViewModel.refresh() },
                    onLogout = onLogout,
                    onAddPatient = {
                        navController.navigate(AppDestinations.ADD_PATIENT.route)
                    },
                    onEditPatient = {},
                    onImportCsv = {
                        // Implementar importação CSV se necessário
                    },
                    onDeletePatient = { patientId ->
                        homeViewModel.deletePatient(patientId)
                    }
                )
            }

            composable(AppDestinations.ADD_PATIENT.route) {
                AddPatientScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onSave = { patient ->
                        homeViewModel.addPatient(patient){
                            navController.popBackStack()
                        }
                        navController.navigate(AppDestinations.HOME.route)
                    }
                )
            }

            composable(AppDestinations.PROFILE.route) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Perfil em construção")
                }
            }
        }
    }
}

@Composable
fun TcctwoApp(
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier
) {
    var isLoggedIn by rememberSaveable {
        mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
    }

    var authScreen by rememberSaveable {
        mutableStateOf(AuthScreen.LOGIN)
    }

    when {
        isLoggedIn -> {
            val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)

            MainScreen(
                homeViewModel = homeViewModel,
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    isLoggedIn = false
                    authScreen = AuthScreen.LOGIN
                }
            )
        }

        authScreen == AuthScreen.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
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

sealed class AppDestinations(val route: String) {
    object HOME : AppDestinations("home")
    object ADD_PATIENT : AppDestinations("add_patient")
    object PROFILE : AppDestinations("profile")
}