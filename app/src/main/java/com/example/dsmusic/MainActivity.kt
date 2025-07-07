package com.example.dsmusic

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.dsmusic.model.Song
import com.example.dsmusic.service.MusicService
import com.example.dsmusic.ui.theme.DSMusicTheme
import com.example.dsmusic.utils.MusicScanner
import com.google.gson.Gson

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
    val songs by remember { mutableStateOf(MusicScanner.getAllAudioFiles(LocalContext.current)) }
    var currentScreen by remember { mutableStateOf(BottomScreen.Home) }

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
    ) { padding ->
        when (currentScreen) {
            BottomScreen.Home -> SongList(songs)
            BottomScreen.Search -> SearchScreen(songs)
            BottomScreen.Library -> SongList(songs)
        }
    }
}

enum class BottomScreen(val label: String) { Home("Accueil"), Search("Recherche"), Library("Bibliothèque") }

@Composable
fun SongList(songs: List<Song>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(songs) { index, song ->
            SongItem(song) { startPlayback(LocalContext.current, songs, index) }
        }
    }
}

@Composable
fun SearchScreen(allSongs: List<Song>) {
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
                SongItem(song) { startPlayback(LocalContext.current, filtered, index) }
            }
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(song.title) },
        supportingContent = { Text(song.artist) },
        modifier = Modifier
            .fillMaxSize(),
        onClick = onClick
    )
    Divider()
}

fun startPlayback(context: android.content.Context, songs: List<Song>, index: Int) {
    val intent = Intent(context, MusicService::class.java).apply {
        action = MusicService.ACTION_START
        putExtra("SONGS", Gson().toJson(songs))
        putExtra("INDEX", index)
    }
    ContextCompat.startForegroundService(context, intent)
}
