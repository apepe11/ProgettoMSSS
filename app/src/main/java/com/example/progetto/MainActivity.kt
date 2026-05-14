package com.example.progetto

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.example.progetto.ui.screens.*
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.utils.SensorAvailability
import com.example.progetto.utils.EegSignalTracker
import com.example.progetto.utils.SensorCollectionViewModel
import com.example.progetto.ui.viewmodels.AuthViewModel
import com.example.progetto.ui.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch
import mylibrary.mindrove.SensorData
import mylibrary.mindrove.ServerManager
import java.util.concurrent.atomic.AtomicLong

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalDrawerNavigation() {
    EnsureRuntimePermissions()

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()
    val context = LocalContext.current
    val sensorViewModel: SensorCollectionViewModel = viewModel(
        factory = SensorCollectionViewModel.Factory(context)
    )
    var loggedFirstEegSample by remember { mutableStateOf(false) }
    val eegFrameCounter = remember { AtomicLong(0L) }
    val serverManager = remember {
        ServerManager { sensorData: SensorData ->
            val frameId = eegFrameCounter.incrementAndGet()
            if (!loggedFirstEegSample) {
                loggedFirstEegSample = true
                Log.d("MindRove", "First EEG sample: measurements=${sensorData.numberOfMeasurement}")
            }
            EegSignalTracker.markSample(System.currentTimeMillis())
            sensorViewModel.onEegDataReceived(0, sensorData.channel1.toDouble(), frameId)
            sensorViewModel.onEegDataReceived(1, sensorData.channel2.toDouble(), frameId)
            sensorViewModel.onEegDataReceived(2, sensorData.channel3.toDouble(), frameId)
            sensorViewModel.onEegDataReceived(3, sensorData.channel4.toDouble(), frameId)
            sensorViewModel.onEegDataReceived(4, sensorData.channel5.toDouble(), frameId)
            sensorViewModel.onEegDataReceived(5, sensorData.channel6.toDouble(), frameId)
        }
    }

    LaunchedEffect(Unit) {
        serverManager.start()
        Log.d("MindRove", "ServerManager started, ip=${serverManager.ipAddress}")
    }

    DisposableEffect(Unit) {
        onDispose {
            serverManager.stop()
        }
    }

    // Track current route to know when to show the top menu
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Usiamo remember per reagire ai cambiamenti nel ViewModel
    val currentUser = authViewModel.currentUser
    val username = currentUser?.username ?: "Guest"

    // Sincronizza lo userId nel PlayerViewModel
    LaunchedEffect(currentUser) {
        playerViewModel.setUserId(currentUser?.userId)
    }

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
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = username,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(16.dp))

                        val allMenuItems = listOf(
                            Triple("Emotion analysis", Icons.Default.Analytics, "emotion_analysis"),
                            Triple("Listening Mode", Icons.Default.Headset, "listening_mode"),
                            Triple("Your feelings", Icons.Default.Favorite, "your_feelings"),
                            Triple("Insights", Icons.Default.BarChart, "insights"),
                            Triple("Favourite Songs", Icons.Default.LibraryMusic, "favourite_songs")
                        )

                        allMenuItems.forEach { (label, icon, route) ->
                            NavigationDrawerItem(
                                label = { Text(label) },
                                selected = false,
                                onClick = {
                                    if (route == "emotion_analysis" &&
                                        !(SensorAvailability.hasEegSignal() && SensorAvailability.hasWatchSignal(context))
                                    ) {
                                        Toast.makeText(
                                            context,
                                            "Connect EEG and watch to use Emotion Analysis.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@NavigationDrawerItem
                                    }
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
                // The Global Scaffold
                Scaffold(
                    topBar = {
                        // Do not show the top bar on login/welcome screens
                        val hideTopBarRoutes = listOf("welcome", "login", "register", "registration_success", "forgot_password", "player")
                        val isPlayerRoute = currentRoute?.startsWith("player") == true
                        if (currentRoute !in hideTopBarRoutes && !isPlayerRoute) {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ComposeImage(
                                            painter = painterResource(id = R.drawable.logo),
                                            contentDescription = "Logo",
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clickable {
                                                    navController.navigate("home") {
                                                        popUpTo("home") { inclusive = true }
                                                    }
                                                },
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("HeartMusic", fontSize = 20.sp, color = Color.Black)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Menu",
                                            tint = Color.Black
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        AppNavigation(
                            navController = navController,
                            authViewModel = authViewModel,
                            playerViewModel = playerViewModel,
                            sensorViewModel = sensorViewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnsureRuntimePermissions() {
    val context = LocalContext.current
    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.BODY_SENSORS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            launcher.launch(missingPermissions.toTypedArray())
        }
    }
}

@Composable
fun AppNavigation(
    navController: androidx.navigation.NavHostController,
    authViewModel: AuthViewModel,
    playerViewModel: PlayerViewModel,
    sensorViewModel: SensorCollectionViewModel,
    onOpenDrawer: () -> Unit
) {
    fun navigateToPlayer(title: String, artist: String, url: String, songId: String) {
        val encodedTitle = Uri.encode(title)
        val encodedArtist = Uri.encode(artist)
        val encodedUrl = Uri.encode(url)
        val encodedSongId = Uri.encode(songId)
        navController.navigate("player?title=$encodedTitle&artist=$encodedArtist&url=$encodedUrl&songId=$encodedSongId")
    }

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
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate("playlist_detail/$playlistId")
                },
                onNavigateToPlayer = { title, artist, url, songId ->
                    navigateToPlayer(title, artist, url, songId)
                },
                onNavigateBack = { navController.popBackStack() },
                playerViewModel = playerViewModel
            )
        }
        composable(
            route = "playlist_detail/{playlistId}",
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            PlaylistDetailScreen(
                playlistId = playlistId,
                onNavigateToPlayer = { title, artist, url, songId ->
                    navigateToPlayer(title, artist, url, songId)
                },
                onNavigateBack = { navController.popBackStack() },
                playerViewModel = playerViewModel
            )
        }
        composable("your_feelings") {
            YourFeelingsScreen(
                onOpenDrawer = onOpenDrawer,
                authViewModel = authViewModel
            )
        }
        composable("insights") {
            InsightsScreen(
                onOpenDrawer = onOpenDrawer
            )
        }
        composable("favourite_songs") {
            // 1. Grab the real, currently logged-in user's ID from your AuthViewModel
            // If it's null for some reason, we pass an empty string as a safe fallback
            val currentUserId = authViewModel.currentUser?.userId ?: ""

            FavouriteSongsScreen(
                currentUserId = currentUserId, // 2. Pass it into the screen right here!
                onOpenDrawer = onOpenDrawer,
                onNavigateToPlayer = { title, artist, url, songId ->
                    navigateToPlayer(title, artist, url, songId)
                }
            )
        }
        composable("emotion_analysis") { backStackEntry ->
            val reviewSaved by backStackEntry.savedStateHandle
                .getStateFlow("review_saved", false)
                .collectAsState()
            EmotionAnalysisScreen(
                onOpenDrawer = onOpenDrawer,
                onReviewSong = { navController.navigate("review_emotion") },
                onGoOn = { /* Handled internally in screen */ },
                advanceAfterReview = reviewSaved,
                onAdvanceAfterReviewHandled = {
                    backStackEntry.savedStateHandle["review_saved"] = false
                },
                onPlaySong = { title, artist, url, songId ->
                    navigateToPlayer(title, artist, url, songId)
                },
                sensorViewModel = sensorViewModel,
                authViewModel = authViewModel,
                playerViewModel = playerViewModel
            )
        }
        composable("review_emotion") {
            ReviewEmotionScreen(
                onOpenDrawer = onOpenDrawer,
                onNavigateBack = { navController.popBackStack() },
                onSaveFeeling = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("review_saved", true)
                    navController.popBackStack()
                },
                authViewModel = authViewModel
            )
        }
        composable(
            route = "player?title={title}&artist={artist}&url={url}&songId={songId}",
            arguments = listOf(
                navArgument("title") { defaultValue = "Unknown" },
                navArgument("artist") { defaultValue = "Unknown" },
                navArgument("url") { defaultValue = "" },
                navArgument("songId") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "Unknown"
            val artist = backStackEntry.arguments?.getString("artist") ?: "Unknown"
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            MusicPlayerScreen(
                songTitle = title,
                artistName = artist,
                songUrl = url,
                songId = songId,
                onNavigateBack = { navController.popBackStack() },
                viewModel = playerViewModel
            )
        }
    }
}
