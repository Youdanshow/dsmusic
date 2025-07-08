package com.example.dsmusic

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.dsmusic.model.Song
import com.example.dsmusic.service.MusicService
import com.example.dsmusic.ui.theme.DSMusicTheme
import com.example.dsmusic.ui.theme.PinkAccent
import com.example.dsmusic.ui.theme.TextWhite
import com.example.dsmusic.utils.MusicScanner
import com.google.gson.Gson
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAudioPermission()
        setContent {
            DSMusicTheme {
                MusicApp()
            }
        }
    }

    private fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PermissionChecker.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                    .launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                    .launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
}

@Composable
fun MusicApp() {
    val context = LocalContext.current
    val songs by remember(context) { mutableStateOf(MusicScanner.getAllAudioFiles(context)) }
    var currentScreen by remember { mutableStateOf(BottomScreen.Home) }
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomScreen.values().forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            when (screen) {
                                BottomScreen.Home -> Icon(Icons.Default.Home, contentDescription = null)
                                BottomScreen.Search -> Icon(Icons.Default.Search, contentDescription = null)
                                BottomScreen.Library -> Icon(Icons.Default.LibraryMusic, contentDescription = null)
                            }
                        },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    BottomScreen.Home -> SongList(songs, onSongClick = { song, index, list ->
                        startPlayback(context, list, index)
                        currentSong = song
                        isPlaying = true
                    }, currentSong = currentSong)
                    BottomScreen.Search -> SearchScreen(songs, onSongClick = { song, index, list ->
                        startPlayback(context, list, index)
                        currentSong = song
                        isPlaying = true
                    }, currentSong = currentSong)
                    BottomScreen.Library -> SongList(songs, onSongClick = { song, index, list ->
                        startPlayback(context, list, index)
                        currentSong = song
                        isPlaying = true
                    }, currentSong = currentSong)
                }
            }
            currentSong?.let { song ->
                MiniPlayer(song, isPlaying) {
                    togglePlayback(context)
                    isPlaying = !isPlaying
                }
            }
        }
    }
}

enum class BottomScreen(val label: String) { Home("Accueil"), Search("Recherche"), Library("Bibliothèque") }

@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: (Song, Int, List<Song>) -> Unit,
    currentSong: Song?
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(songs) { index, song ->
            SongItem(
                song = song,
                onClick = { onSongClick(song, index, songs) },
                isCurrent = song.uri == currentSong?.uri
            )
        }
    }
}

@Composable
fun SearchScreen(
    allSongs: List<Song>,
    onSongClick: (Song, Int, List<Song>) -> Unit,
    currentSong: Song?
) {
    var query by remember { mutableStateOf("") }
    val filtered = allSongs.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxSize(),
            placeholder = { Text("Rechercher") }
        )
        LazyColumn {
            itemsIndexed(filtered) { index, song ->
                SongItem(
                    song = song,
                    onClick = { onSongClick(song, index, filtered) },
                    isCurrent = song.uri == currentSong?.uri
                )
            }
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit, isCurrent: Boolean) {
    val colors = ListItemDefaults.colors(
        containerColor = if (isCurrent) PinkAccent else MaterialTheme.colorScheme.surface,
        headlineColor = if (isCurrent) TextWhite else MaterialTheme.colorScheme.onSurface,
        supportingColor = if (isCurrent) TextWhite else MaterialTheme.colorScheme.onSurfaceVariant
    )
    ListItem(
        headlineContent = { Text(song.title) },
        supportingContent = { Text(song.artist) },
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
    HorizontalDivider()
}

fun startPlayback(context: android.content.Context, songs: List<Song>, index: Int) {
    val intent = Intent(context, MusicService::class.java).apply {
        action = MusicService.ACTION_START
        putExtra("SONGS", Gson().toJson(songs))
        putExtra("INDEX", index)
    }
    ContextCompat.startForegroundService(context, intent)
}

fun togglePlayback(context: android.content.Context) {
    val intent = Intent(context, MusicService::class.java).apply {
        action = MusicService.ACTION_TOGGLE_PLAY
    }
    ContextCompat.startForegroundService(context, intent)
}

@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, onToggle: () -> Unit) {
    Surface(shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyLarge)
                Text(song.artist, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onToggle) {
                val icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                val desc = if (isPlaying) "Pause" else "Play"
                Icon(icon, contentDescription = desc)
            }
        }
    }
}
