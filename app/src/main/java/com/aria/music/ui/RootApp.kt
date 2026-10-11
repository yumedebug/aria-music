package com.aria.music.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aria.music.data.online.OnlineCollection
import com.aria.music.data.online.OnlineCollectionKind
import com.aria.music.data.online.OnlineCollectionNav
import com.aria.music.data.repo.AppSettings
import com.aria.music.ui.components.MiniPlayer
import com.aria.music.ui.detail.AlbumDetailScreen
import com.aria.music.ui.detail.OnlineCollectionDetailScreen
import com.aria.music.ui.detail.PlaylistDetailScreen
import com.aria.music.ui.home.HomeScreen
import com.aria.music.ui.library.LibraryScreen
import com.aria.music.ui.player.NowPlayingScreen
import com.aria.music.ui.search.SearchScreen
import com.aria.music.ui.settings.SettingsScreen
import com.aria.music.ui.theme.AriaTheme
import com.aria.music.ui.theme.BrandAccent
import com.aria.music.ui.theme.rememberArtworkAccent
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Rounded.Home),
    Tab("search", "Search", Icons.Rounded.Search),
    Tab("library", "Library", Icons.Rounded.LibraryMusic),
    Tab("settings", "Settings", Icons.Rounded.Settings),
)

@Composable
fun AriaRoot() {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val playbackState by container.playback.state.collectAsStateWithLifecycle()
    val artworkAccent = rememberArtworkAccent(playbackState.current)
    val accent = if (settings.artworkColors) (artworkAccent ?: BrandAccent) else BrandAccent

    // Media library permission, then a first scan so Home has data immediately.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Whatever the user chose, re-run the scan: it returns nothing without access.
        container.applicationScope.launch { container.libraryRepository.refresh() }
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(arrayOf(permission, Manifest.permission.POST_NOTIFICATIONS))
        container.playback.connect()
        container.libraryRepository.refresh()
    }

    AriaTheme(
        settings = settings,
        artworkAccent = if (settings.artworkColors) artworkAccent else null,
    ) {
        AppScaffold(accent = accent)
    }
}

@Composable
private fun AppScaffold(accent: androidx.compose.ui.graphics.Color) {
    val container = LocalAppContainer.current
    val playbackState by container.playback.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = tabs.any { it.route == currentRoute }

    var playerExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = playerExpanded) { playerExpanded = false }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                AnimatedVisibility(visible = playbackState.current != null) {
                    MiniPlayer(
                        state = playbackState,
                        onExpand = { playerExpanded = true },
                        onTogglePlay = { container.playback.togglePlayPause() },
                        onNext = { container.playback.next() },
                    )
                }
                if (isTopLevel) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    )
                    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(tab.route) {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = accent,
                                    selectedTextColor = accent,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        AriaNavHost(
            navController = navController,
            contentPadding = innerPadding,
            onOpenPlayer = { playerExpanded = true },
        )
    }

    AnimatedVisibility(
        visible = playerExpanded,
        enter = slideInVertically(animationSpec = tween(320)) { it } + fadeIn(tween(220)),
        exit = slideOutVertically(animationSpec = tween(260)) { it } + fadeOut(tween(180)),
        modifier = Modifier.fillMaxSize(),
    ) {
        NowPlayingScreen(
            state = playbackState,
            accent = accent,
            onCollapse = { playerExpanded = false },
            onTogglePlay = { container.playback.togglePlayPause() },
            onNext = { container.playback.next() },
            onPrevious = { container.playback.previous() },
            onSeek = { container.playback.seekTo(it) },
            onToggleShuffle = { container.playback.toggleShuffle() },
            onCycleRepeat = { container.playback.cycleRepeat() },
        )
    }
}

@Composable
private fun AriaNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    onOpenPlayer: () -> Unit,
) {
    val container = LocalAppContainer.current
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize(),
    ) {
        composable("home") {
            HomeScreen(
                onOpenAlbum = { navController.navigate("album/${it.id}") },
                onOpenPlaylist = { navController.navigate("playlist/$it") },
                onOpenCollection = { collection ->
                    // タイトルやサムネイルを route に載せず ID だけ運び、中身は
                    // OnlineCollectionNav で受け渡す。
                    OnlineCollectionNav.passTo(collection)
                    navController.navigate("online/${collection.kind}/${collection.id}")
                },
                onOpenSearch = { navController.navigate("search") },
                contentPadding = contentPadding,
            )
        }
        composable("search") {
            SearchScreen(contentPadding = contentPadding)
        }
        composable("library") {
            LibraryScreen(
                onOpenAlbum = { navController.navigate("album/${it.id}") },
                onOpenPlaylist = { navController.navigate("playlist/$it") },
                contentPadding = contentPadding,
            )
        }
        composable("settings") {
            SettingsScreen(contentPadding = contentPadding)
        }
        composable(
            route = "album/{albumId}",
            arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
        ) { entry ->
            val albumId = entry.arguments?.getLong("albumId") ?: 0L
            val albums by container.libraryRepository.albums
                .collectAsStateWithLifecycle(initialValue = emptyList())
            val album = albums.firstOrNull { it.id == albumId }
            if (album != null) {
                AlbumDetailScreen(
                    album = album,
                    onBack = { navController.popBackStack() },
                    onPlayTrack = { track, list -> container.playback.playTracks(list, list.indexOf(track)) },
                    onPlayAll = { list -> container.playback.playTracks(list, 0); onOpenPlayer() },
                    onShuffle = {
                        container.playback.playTracks(it.shuffled(), 0)
                        onOpenPlayer()
                    },
                    contentPadding = contentPadding,
                )
            }
        }
        composable(
            route = "playlist/{playlistId}",
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
        ) { entry ->
            val playlistId = entry.arguments?.getLong("playlistId") ?: 0L
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBack = { navController.popBackStack() },
                onPlayTrack = { track, list -> container.playback.playTracks(list, list.indexOf(track)) },
                onPlayAll = { list -> container.playback.playTracks(list, 0); onOpenPlayer() },
                onShuffle = {
                    container.playback.playTracks(it.shuffled(), 0)
                    onOpenPlayer()
                },
                contentPadding = contentPadding,
            )
        }
        composable(
            route = "online/{collectionKind}/{collectionId}",
            arguments = listOf(
                navArgument("collectionKind") { type = NavType.StringType },
                navArgument("collectionId") { type = NavType.StringType },
            ),
        ) { entry ->
            val kind = entry.arguments?.getString("collectionKind").orEmpty()
            val id = entry.arguments?.getString("collectionId").orEmpty()
            // 画面回転などで戻って来た時も同じ中身が見られるよう、保持している分が
            // 無ければ種別と ID だけで復元する。
            val collection = OnlineCollectionNav.current()?.takeIf { it.id == id }
                ?: OnlineCollection(
                    kind = OnlineCollectionKind.entries.firstOrNull { it.name == kind }
                        ?: OnlineCollectionKind.ALBUM,
                    id = id,
                    title = "",
                    subtitle = "",
                    thumbnail = null,
                )
            OnlineCollectionDetailScreen(
                collection = collection,
                onBack = { navController.popBackStack() },
                contentPadding = contentPadding,
            )
        }
    }
}
