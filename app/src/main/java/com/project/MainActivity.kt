package com.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.project.ui.login.LoginScreen
import com.project.ui.login.ResetPasswordScreen
import com.project.ui.login.RegisterScreen
import com.project.ui.login.AuthScreen
import com.project.ui.theme.TcctwoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TcctwoTheme {
                TcctwoApp()
            }
        }
    }
}

@Preview
@Composable
fun MainScreen() {
    var currentDestination by rememberSaveable {
        mutableStateOf(AppDestinations.HOME)
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> Text(
                    "Home",
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestinations.FAVORITES -> Text(
                    "Favorites",
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestinations.PROFILE -> Text(
                    "Profile",
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
@Composable
fun TcctwoApp() {
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    var authScreen by rememberSaveable { mutableStateOf(AuthScreen.LOGIN) }

    when {
        isLoggedIn -> {
            MainScreen()
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
                }
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TcctwoTheme {
        Greeting("Android")
    }
}