package com.example.progetto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.progetto.ui.screens.*
import com.example.progetto.ui.theme.HeartMusicTheme
import com.example.progetto.ui.viewmodels.AuthViewModel
import com.example.progetto.ui.viewmodels.PlayerViewModel
import com.example.progetto.utils.SensorAvailability
import com.example.progetto.utils.SensorCollectionViewModel
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

    val sensorViewModel = remember { SensorCollectionViewModel.get(context) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentUser = authViewModel.currentUser
    val username = currentUser?.username ?: stringResource(R.string.nav_guest)

    LaunchedEffect(currentUser) {
        playerViewModel.setUserId(currentUser?.userId)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                            Triple(stringResource(R.string.nav_emotion_analysis), Icons.Default.Analytics, "emotion_analysis"),
                            Triple(stringResource(R.string.nav_listening_mode), Icons.Default.Headset, "listening_mode"),
                            Triple(stringResource(R.string.nav_your_feelings), Icons.Default.Favorite, "your_feelings"),
                            Triple(stringResource(R.string.nav_insights), Icons.Default.BarChart, "insights"),
                            Triple(stringResource(R.string.nav_favourite_songs), Icons.Default.LibraryMusic, "favourite_songs")
                        )

                        allMenuItems.forEach { (label, icon, route) ->
                            NavigationDrawerItem(
                                label = { Text(label) },
                                selected = false,
                                onClick = {
                                    if (route == "emotion_analysis" &&
                                        !(SensorAvailability.hasEegSignal() && SensorAvailability.hasWatchSignal(context))
                                    ) {
                                        Toast.makeText(context, context.getString(R.string.toast_connect_sensors), Toast.LENGTH_SHORT).show()
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
                            label = { Text(stringResource(R.string.nav_logout), color = Color.Red) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    authViewModel.logout()
                                }
                                navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
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
                Scaffold(
                    topBar = {
                        val hideTopBarRoutes = listOf("welcome", "login", "register", "registration_success", "forgot_password", "player")
                        val isPlayerRoute = currentRoute?.startsWith("player") == true
                        if (currentRoute !in hideTopBarRoutes && !isPlayerRoute) {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ComposeImage(
                                            painter = painterResource(id = R.drawable.logo),
                                            contentDescription = stringResource(R.string.nav_logo_description),
                                            modifier = Modifier.size(80.dp).clickable {
                                                navController.navigate("home") { popUpTo("home") { inclusive = true } }
                                            },
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.app_name), fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.nav_menu_description),
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        AppNavigation(
                            navController = navController,
                            authViewModel = authViewModel,
                            playerViewModel = playerViewModel,
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
    var showRationale by remember { mutableStateOf(false) }
    var showSettingsRationale by remember { mutableStateOf(false) }

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
        onResult = { result ->
            val allGranted = result.values.all { it }
            if (!allGranted) {
                val permanentlyDenied = requiredPermissions.any { permission ->
                    ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED &&
                            (context as? ComponentActivity)?.shouldShowRequestPermissionRationale(permission) == false
                }
                if (permanentlyDenied) showSettingsRationale = true
                else Toast.makeText(context, context.getString(R.string.permission_denied_toast), Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            val shouldShowRationale = missingPermissions.any { (context as? ComponentActivity)?.shouldShowRequestPermissionRationale(it) == true }
            if (shouldShowRationale) showRationale = true else launcher.launch(missingPermissions.toTypedArray())
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.permission_title)) },
            text = { Text(stringResource(R.string.permission_rationale)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    launcher.launch(requiredPermissions.toTypedArray())
                }) { Text(stringResource(R.string.permission_grant)) }
            },
            dismissButton = { TextButton(onClick = { showRationale = false }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }

    if (showSettingsRationale) {
        AlertDialog(
            onDismissRequest = { showSettingsRationale = false },
            title = { Text(stringResource(R.string.permission_title)) },
            text = { Text(stringResource(R.string.permission_settings_rationale)) },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsRationale = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }
                    context.startActivity(intent)
                }) { Text(stringResource(R.string.permission_settings)) }
            },
            dismissButton = { TextButton(onClick = { showSettingsRationale = false }) { Text(stringResource(android.R.string.cancel)) } }
        )
    }
}

@Composable
fun AppNavigation(
    navController: androidx.navigation.NavHostController,
    authViewModel: AuthViewModel,
    playerViewModel: PlayerViewModel,
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
                onNavigateToLogin = { navController.navigate("login") { popUpTo("welcome") { inclusive = false } } }
            )
        }
        composable("forgot_password") { ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() }) }
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
                onNavigateToPlaylist = { navController.navigate("playlist_detail/$it") },
                onNavigateToPlayer = { t, a, u, s -> navigateToPlayer(t, a, u, s) },
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
                onNavigateToPlayer = { t, a, u, s -> navigateToPlayer(t, a, u, s) },
                onNavigateBack = { navController.popBackStack() },
                playerViewModel = playerViewModel
            )
        }
        composable("your_feelings") { YourFeelingsScreen(onOpenDrawer = onOpenDrawer, authViewModel = authViewModel) }
        composable("insights") {
            val currentUserId = authViewModel.currentUser?.userId ?: ""
            InsightsScreen(currentUserId = currentUserId, onOpenDrawer = onOpenDrawer)
        }
        composable("favourite_songs") {
            val currentUserId = authViewModel.currentUser?.userId ?: ""
            FavouriteSongsScreen(currentUserId = currentUserId, onOpenDrawer = onOpenDrawer, onNavigateToPlayer = { t, a, u, s -> navigateToPlayer(t, a, u, s) })
        }
        composable("emotion_analysis") {
            EmotionAnalysisScreen(
                onOpenDrawer = onOpenDrawer,
                onReviewSong = { navController.navigate("review_emotion") },
                onGoOn = { },
                onPlaySong = { t, a, u, s -> navigateToPlayer(t, a, u, s) },
                authViewModel = authViewModel,
                playerViewModel = playerViewModel
            )
        }
        composable("review_emotion") {
            ReviewEmotionScreen(onOpenDrawer = onOpenDrawer, onNavigateBack = { navController.popBackStack() }, onSaveFeeling = { navController.popBackStack() }, authViewModel = authViewModel)
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
            MusicPlayerScreen(songTitle = title, artistName = artist, songUrl = url, songId = songId, onNavigateBack = { navController.popBackStack() }, viewModel = playerViewModel)
        }
    }
}