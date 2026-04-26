package com.example.progetto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.progetto.ui.screens.*
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HeartMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GlobalDrawerNavigation()
                }
            }
        }
    }
}

@Composable
fun GlobalDrawerNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    
    // Usiamo remember per reagire ai cambiamenti nel ViewModel
    val currentUser = authViewModel.currentUser
    val username = currentUser?.username ?: "Guest"

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // Se l'utente è già loggato all'avvio, mandalo alla Home direttamente
        LaunchedEffect(authViewModel.currentUser) {
            if (authViewModel.currentUser != null && navController.currentDestination?.route == "welcome") {
                navController.navigate("home") {
                    popUpTo("welcome") { inclusive = true }
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier.fillMaxHeight().width(280.dp),
                        drawerContainerColor = Color.White
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = username,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black, // Forza il colore nero
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val menuItems = listOf(
                            Triple("Emotion analysis", Icons.Default.Analytics, "emotion_analysis"),
                            Triple("Listening Mode", Icons.Default.Headset, "listening_mode"),
                            Triple("Your feelings", Icons.Default.Favorite, "your_feelings"),
                            Triple("Insights", Icons.Default.BarChart, "insights")
                        )

                        menuItems.forEach { (label, icon, route) ->
                            NavigationDrawerItem(
                                label = { Text(label) },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(icon, contentDescription = null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        NavigationDrawerItem(
                            label = { Text("Logout", color = Color.Red) },
                            selected = false,
                            onClick = {
                                scope.launch { 
                                    drawerState.close() 
                                    authViewModel.logout()
                                }
                                navController.navigate("welcome") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AppNavigation(
                    navController = navController,
                    authViewModel = authViewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    navController: androidx.navigation.NavHostController,
    authViewModel: AuthViewModel,
    onOpenDrawer: () -> Unit
) {
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
                onLoginSuccess = { navController.navigate("home") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                onNavigateToRegister = { navController.navigate("register") },
                viewModel = authViewModel
            )
        }
        composable("register") {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { navController.navigate("registration_success") },
                viewModel = authViewModel
            )
        }
        composable("registration_success") {
            RegistrationSuccessScreen(
                onNavigateToLogin = { 
                    navController.navigate("login") {
                        popUpTo("welcome") { inclusive = false }
                    }
                }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("home") {
            HomeScreen(
                onOpenDrawer = onOpenDrawer,
                onNavigateToEmotionAnalysis = { navController.navigate("emotion_analysis") },
                onNavigateToListeningMode = { navController.navigate("listening_mode") }
            )
        }
        composable("listening_mode") {
            ListeningModeScreen(
                onOpenDrawer = onOpenDrawer,
                onNavigateToPlaylist = { playlistName -> 
                    navController.navigate("playlist_detail/$playlistName") 
                },
                onNavigateToPlayer = { navController.navigate("player") },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "playlist_detail/{playlistName}",
            arguments = listOf(navArgument("playlistName") { type = NavType.StringType })
        ) { backStackEntry ->
            val playlistName = backStackEntry.arguments?.getString("playlistName") ?: ""
            PlaylistDetailScreen(
                playlistName = playlistName,
                onOpenDrawer = onOpenDrawer,
                onNavigateToPlayer = { navController.navigate("player") },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("your_feelings") {
            YourFeelingsScreen(
                onOpenDrawer = onOpenDrawer
            )
        }
        composable("insights") {
            InsightsScreen(
                onOpenDrawer = onOpenDrawer
            )
        }
        composable("emotion_analysis") {
            EmotionAnalysisScreen(
                onOpenDrawer = onOpenDrawer,
                onGoOn = { navController.navigate("review_emotion") }
            )
        }
        composable("review_emotion") {
            ReviewEmotionScreen(
                onOpenDrawer = onOpenDrawer,
                onNavigateBack = { navController.popBackStack() },
                onSaveFeeling = { navController.popBackStack() }
            )
        }
        composable("player") {
            MusicPlayerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
