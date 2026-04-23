package progetto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.progetto.ui.screens.LoginScreen
import com.example.progetto.ui.screens.RegisterScreen
import com.example.progetto.ui.screens.WelcomeScreen
import com.example.progetto.ui.theme.HeartMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HeartMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("login") {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onLoginSuccess = { navController.navigate("home") }
            )
        }
        composable("register") {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { 
                    // Dopo la registrazione, mostriamo la conferma o andiamo alla home
                    navController.navigate("home") 
                }
            )
        }
        composable("home") {
            // Schermata Home (da implementare)
        }
    }
}