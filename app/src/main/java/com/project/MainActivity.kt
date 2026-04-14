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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.project.data.repository.PatientRepository
import com.project.ui.home.AddPatientScreen
import com.project.ui.home.AddNetworkScreen
import com.project.ui.home.EcomapaFormScreen
import com.project.ui.home.EcomapaViewScreen
import com.project.ui.home.EcomapaViewModel
import com.project.ui.home.EcomapaViewModelFactory
import com.project.ui.home.HomeScreen
import com.project.ui.home.HomeViewModel
import com.project.ui.home.HomeViewModelFactory
import com.project.ui.home.PatientProfileScreen
import com.project.data.repository.EcomapaRepository
import com.project.ui.login.AuthScreen
import com.project.ui.login.LoginScreen
import com.project.ui.login.RegisterScreen
import com.project.ui.login.ResetPasswordScreen
import com.project.ui.theme.TcctwoTheme

// Global CompositionLocals for Theme Toggling across all screens
val LocalThemeToggle = staticCompositionLocalOf<() -> Unit> { {} }
val LocalIsDarkTheme = staticCompositionLocalOf<Boolean> { false }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseApp.initializeApp(this) ?: FirebaseApp.getInstance()

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings

        val action = intent.action
        val dataUri = if (android.content.Intent.ACTION_VIEW == action) intent.data else null

        setContent {
            TcctwoApp(initialPendingUri = dataUri)
        }
    }
}

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    ecomapaViewModel: EcomapaViewModel,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val state by homeViewModel.screenState.collectAsState()
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val syncManager = com.project.data.repository.DataSyncManager(context)
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                if (userId.isNotEmpty()) {
                    val success = syncManager.importPatientData(it, userId) { }
                    if (success) {
                        homeViewModel.refresh()
                    }
                }
            }
        }
    }

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
                        filePickerLauncher.launch(arrayOf("*/*"))
                    },
                    onDeletePatient = { patientId ->
                        homeViewModel.deletePatient(patientId)
                    },
                    onPatientClick = { patientId ->
                        navController.navigate(AppDestinations.PATIENT_PROFILE.route.replace("{patientId}", patientId))
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

            composable(
                route = AppDestinations.PATIENT_PROFILE.route,
                arguments = listOf(navArgument("patientId") { type = NavType.StringType })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                PatientProfileScreen(
                    patientId = patientId,
                    homeViewModel = homeViewModel,
                    onBack = { navController.popBackStack() },
                    onCreateEcomapa = {
                        ecomapaViewModel.createEcomapa(patientId) { ecomapaId ->
                            navController.navigate(AppDestinations.ECOMAPA_FORM.route
                                .replace("{patientId}", patientId)
                                .replace("{ecomapaId}", ecomapaId))
                        }
                    },
                    onOpenEcomapa = { ecomapaId ->
                        navController.navigate(AppDestinations.ECOMAPA_FORM.route
                            .replace("{patientId}", patientId)
                            .replace("{ecomapaId}", ecomapaId))
                    },
                    onOpenEcomapaView = { ecomapaId ->
                        navController.navigate(AppDestinations.ECOMAPA_VIEW.route
                            .replace("{patientId}", patientId)
                            .replace("{ecomapaId}", ecomapaId))
                    },
                    ecomapaViewModel = ecomapaViewModel
                )
            }

            composable(
                route = AppDestinations.ECOMAPA_FORM.route,
                arguments = listOf(
                    navArgument("patientId") { type = NavType.StringType },
                    navArgument("ecomapaId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                val ecomapaId = backStackEntry.arguments?.getString("ecomapaId") ?: return@composable
                EcomapaFormScreen(
                    patientId = patientId,
                    ecomapaId = ecomapaId,
                    viewModel = ecomapaViewModel,
                    onBack = { navController.popBackStack() },
                    onAddInstitution = {
                        navController.navigate(AppDestinations.ADD_NETWORK.route
                            .replace("{patientId}", patientId)
                            .replace("{ecomapaId}", ecomapaId))
                    },
                    onEditInstitution = { networkId ->
                        navController.navigate(AppDestinations.ADD_NETWORK.route
                            .replace("{patientId}", patientId)
                            .replace("{ecomapaId}", ecomapaId) + "?networkId=$networkId")
                    }
                )
            }

            composable(
                route = AppDestinations.ADD_NETWORK.route + "?networkId={networkId}",
                arguments = listOf(
                    navArgument("patientId") { type = NavType.StringType },
                    navArgument("ecomapaId") { type = NavType.StringType },
                    navArgument("networkId") { 
                        type = NavType.StringType 
                        nullable = true 
                        defaultValue = null 
                    }
                )
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                val ecomapaId = backStackEntry.arguments?.getString("ecomapaId") ?: return@composable
                val networkId = backStackEntry.arguments?.getString("networkId")
                AddNetworkScreen(
                    patientId = patientId,
                    ecomapaId = ecomapaId,
                    networkId = networkId,
                    viewModel = ecomapaViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppDestinations.ECOMAPA_VIEW.route,
                arguments = listOf(
                    navArgument("patientId") { type = NavType.StringType },
                    navArgument("ecomapaId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                val ecomapaId = backStackEntry.arguments?.getString("ecomapaId") ?: return@composable
                EcomapaViewScreen(
                    patientId = patientId,
                    ecomapaId = ecomapaId,
                    viewModel = ecomapaViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun TcctwoApp(modifier: Modifier = Modifier, initialPendingUri: android.net.Uri? = null) {
    var isDarkTheme by rememberSaveable { mutableStateOf(false) }
    var pendingUri by rememberSaveable { mutableStateOf(initialPendingUri?.toString()) }

    TcctwoTheme(darkTheme = isDarkTheme) {
        CompositionLocalProvider(
            LocalThemeToggle provides { isDarkTheme = !isDarkTheme },
            LocalIsDarkTheme provides isDarkTheme
        ) {
            var isLoggedIn by rememberSaveable {
                mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
            }

    var authScreen by rememberSaveable {
        mutableStateOf(AuthScreen.LOGIN)
    }

    when {
        isLoggedIn -> {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val homeRepository = PatientRepository(userId)
            val homeViewModel: HomeViewModel = viewModel(
                key = "home_$userId", 
                factory = HomeViewModelFactory(homeRepository)
            )
            
            val ecomapaRepository = EcomapaRepository(userId)
            val ecomapaViewModel: EcomapaViewModel = viewModel(
                key = "ecomapa_$userId",
                factory = EcomapaViewModelFactory(ecomapaRepository)
            )

            var showImportDialog by remember { mutableStateOf(pendingUri != null) }
            val context = androidx.compose.ui.platform.LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            if (showImportDialog && pendingUri != null) {
                AlertDialog(
                    onDismissRequest = { 
                        showImportDialog = false
                        pendingUri = null
                    },
                    title = { Text("Importar Perfil do Paciente") },
                    text = { Text("Detectamos um arquivo do TCC2 externo (Ecomapas e Redes). Deseja importá-lo para a sua conta do Firebase?") },
                    confirmButton = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                val syncManager = com.project.data.repository.DataSyncManager(context)
                                val success = syncManager.importPatientData(android.net.Uri.parse(pendingUri), userId) { msg ->
                                    // Placeholder for progress 
                                }
                                if (success) {
                                    homeViewModel.refresh()
                                }
                                showImportDialog = false
                                pendingUri = null
                            }
                        }) {
                            Text("Confirmar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showImportDialog = false
                            pendingUri = null
                        }) {
                            Text("Ignorar")
                        }
                    }
                )
            }

            MainScreen(
                homeViewModel = homeViewModel,
                ecomapaViewModel = ecomapaViewModel,
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
    } // Close CompositionLocalProvider
    } // Close TcctwoTheme
}

sealed class AppDestinations(val route: String) {
    object HOME : AppDestinations("home")
    object ADD_PATIENT : AppDestinations("add_patient")
    object PROFILE : AppDestinations("profile")
    object PATIENT_PROFILE : AppDestinations("patient_profile/{patientId}")
    object ECOMAPA_FORM : AppDestinations("ecomapa_form/{patientId}/{ecomapaId}")
    object ADD_NETWORK : AppDestinations("add_network/{patientId}/{ecomapaId}")
    object ECOMAPA_VIEW : AppDestinations("ecomapa_view/{patientId}/{ecomapaId}")
}