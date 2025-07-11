package com.example.dsmusic

import android.Manifest
import android.content.Intent
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.blur
import androidx.compose.material3.*
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.example.dsmusic.utils.PlaybackHolder
import com.google.gson.Gson
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

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
    var playlist by remember { mutableStateOf<List<Song>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var shuffleOn by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(0) }
    var musicService by remember { mutableStateOf<MusicService?>(null) }
    val backgrounds = remember {
        listOf(
            R.drawable.back_1,
            R.drawable.back_2,
            R.drawable.back_3,
            R.drawable.back_4,
        )
    }
    val backgroundRes = remember { backgrounds.random() }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                musicService = (binder as MusicService.LocalBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                musicService = null
            }
        }
    }

    DisposableEffect(Unit) {
        context.bindService(
            Intent(context, MusicService::class.java),
            connection,
            android.content.Context.BIND_AUTO_CREATE
        )
        onDispose { context.unbindService(connection) }
    }

    LaunchedEffect(musicService) {
        while (true) {
            musicService?.let { service ->
                val serviceSong = service.getCurrentSong()
                if (serviceSong != null && serviceSong.uri != currentSong?.uri) {
                    currentSong = serviceSong
                    playlist = service.getSongs()
                    currentIndex = service.getCurrentIndex()
                }
                shuffleOn = service.isShuffle()
                repeatMode = service.getRepeatMode()
            }
            delay(500)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(20.dp)
                .graphicsLayer(alpha = 0.5f)
        )
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
                        playlist = list
                        currentIndex = index
                        currentSong = song
                        isPlaying = true
                    }, currentSong = currentSong, showFilter = true)
                    BottomScreen.Search -> SearchScreen(songs, onSongClick = { song, index, list ->
                        startPlayback(context, list, index)
                        playlist = list
                        currentIndex = index
                        currentSong = song
                        isPlaying = true
                    }, currentSong = currentSong)
                    BottomScreen.Library -> SongList(songs, onSongClick = { song, index, list ->
                        startPlayback(context, list, index)
                        playlist = list
                        currentIndex = index
                        currentSong = song
                        isPlaying = true
                    }, currentSong = currentSong)
                }
            }
            currentSong?.let { song ->
                MiniPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    shuffleOn = shuffleOn,
                    repeatMode = repeatMode,
                    service = musicService,
                    onToggle = {
                        togglePlayback(context)
                        isPlaying = !isPlaying
                    },
                    onNext = {
                        musicService?.nextSong()
                        if (playlist.isNotEmpty()) {
                            currentIndex = (currentIndex + 1) % playlist.size
                            currentSong = playlist[currentIndex]
                        }
                    },
                    onPrevious = {
                        musicService?.previousSong()
                        if (playlist.isNotEmpty()) {
                            currentIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
                            currentSong = playlist[currentIndex]
                        }
                    },
                    onShuffle = {
                        musicService?.toggleShuffle()
                        shuffleOn = musicService?.isShuffle() ?: shuffleOn
                    },
                    onRepeat = {
                        musicService?.cycleRepeatMode()
                        repeatMode = musicService?.getRepeatMode() ?: repeatMode
                    }
                )
            }
        }
    }
}

enum class BottomScreen(val label: String) { Home("Accueil"), Search("Recherche"), Library("Bibliothèque") }

enum class SortField(val label: String) {
    TITLE("Nom"),
    ALBUM("Album"),
    ARTIST("Artiste"),
    DURATION("Durée"),
    SIZE("Taille")
}
@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: (Song, Int, List<Song>) -> Unit,
    currentSong: Song?,
    showFilter: Boolean = false,
) {
    var sortField by remember { mutableStateOf(SortField.TITLE) }
    var ascending by remember { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }

    val sortedSongs = remember(songs, sortField, ascending) {
        val sorted = when (sortField) {
            SortField.TITLE -> songs.sortedBy { it.title }
            SortField.ALBUM -> songs.sortedBy { it.album }
            SortField.ARTIST -> songs.sortedBy { it.artist }
            SortField.DURATION -> songs.sortedBy { it.duration }
            SortField.SIZE -> songs.sortedBy { it.size }
        }
        if (ascending) sorted else sorted.reversed()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (showFilter) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrer")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        SortField.values().forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field.label) },
                                onClick = {
                                    sortField = field
                                    menuExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Ordre croissant") },
                            onClick = {
                                ascending = true
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ordre décroissant") },
                            onClick = {
                                ascending = false
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Réinitialiser") },
                            onClick = {
                                sortField = SortField.TITLE
                                ascending = true
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(sortedSongs) { index, song ->
                SongItem(
                    song = song,
                    onClick = { onSongClick(song, index, sortedSongs) },
                    isCurrent = song.uri == currentSong?.uri
                )
            }
        }
    }
}

@Composable
fun SearchScreen(
    allSongs: List<Song>,
    onSongClick: (Song, Int, List<Song>) -> Unit,
    currentSong: Song?,
) {
    var query by remember { mutableStateOf("") }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var albumsExpanded by remember { mutableStateOf(false) }
    var artistsExpanded by remember { mutableStateOf(false) }
    var songsExpanded by remember { mutableStateOf(false) }
    var sortField by remember { mutableStateOf(SortField.TITLE) }
    var ascending by remember { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val filtered = allSongs.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
    val sortedFiltered = remember(filtered, sortField, ascending) {
        val sorted = when (sortField) {
            SortField.TITLE -> filtered.sortedBy { it.title }
            SortField.ALBUM -> filtered.sortedBy { it.album }
            SortField.ARTIST -> filtered.sortedBy { it.artist }
            SortField.DURATION -> filtered.sortedBy { it.duration }
            SortField.SIZE -> filtered.sortedBy { it.size }
        }
        if (ascending) sorted else sorted.reversed()
    }
    val albums = allSongs.map { it.album }.distinct().filter { it.contains(query, true) }
    val sortedAlbums = remember(albums, ascending) {
        val base = albums.sorted()
        if (ascending) base else base.reversed()
    }
    val artists = allSongs.map { it.artist }.distinct().filter { it.contains(query, true) }
    val sortedArtists = remember(artists, ascending) {
        val base = artists.sorted()
        if (ascending) base else base.reversed()
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtrer")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    SortField.values().forEach { field ->
                        DropdownMenuItem(
                            text = { Text(field.label) },
                            onClick = {
                                sortField = field
                                menuExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Ordre croissant") },
                        onClick = {
                            ascending = true
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ordre décroissant") },
                        onClick = {
                            ascending = false
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Réinitialiser") },
                        onClick = {
                            sortField = SortField.TITLE
                            ascending = true
                            menuExpanded = false
                        }
                    )
                }
            }
        }
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            placeholder = { Text("Rechercher") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (selectedAlbum == null && selectedArtist == null) {
                if (sortedArtists.isNotEmpty()) {
                    item {
                        ListItem(
                            headlineContent = { Text("Artistes") },
                            trailingContent = {
                                IconButton(onClick = { artistsExpanded = !artistsExpanded }) {
                                    val icon = if (artistsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore
                                    Icon(icon, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { artistsExpanded = !artistsExpanded }
                        )
                        HorizontalDivider()
                    }
                    if (artistsExpanded) {
                        items(sortedArtists) { artist ->
                            ArtistItem(artist) { selectedArtist = artist }
                        }
                    }
                }
                if (sortedAlbums.isNotEmpty()) {
                    item {
                        ListItem(
                            headlineContent = { Text("Albums") },
                            trailingContent = {
                                IconButton(onClick = { albumsExpanded = !albumsExpanded }) {
                                    val icon = if (albumsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore
                                    Icon(icon, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { albumsExpanded = !albumsExpanded }
                        )
                        HorizontalDivider()
                    }
                    if (albumsExpanded) {
                        items(sortedAlbums) { album ->
                            AlbumItem(album) { selectedAlbum = album }
                        }
                    }
                }
                item {
                    ListItem(
                        headlineContent = { Text("Chansons") },
                        trailingContent = {
                            IconButton(onClick = { songsExpanded = !songsExpanded }) {
                                val icon = if (songsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore
                                Icon(icon, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { songsExpanded = !songsExpanded }
                    )
                    HorizontalDivider()
                }
                if (songsExpanded) {
                    itemsIndexed(sortedFiltered) { index, song ->
                        SongItem(
                            song = song,
                            onClick = { onSongClick(song, index, sortedFiltered) },
                            isCurrent = song.uri == currentSong?.uri
                        )
                    }
                }
            } else if (selectedArtist != null) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { selectedArtist = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                        Text(text = selectedArtist ?: "")
                    }
                }
                val artistSongs = allSongs.filter { it.artist == selectedArtist }
                val sortedArtist = when (sortField) {
                    SortField.TITLE -> artistSongs.sortedBy { it.title }
                    SortField.ALBUM -> artistSongs.sortedBy { it.album }
                    SortField.ARTIST -> artistSongs.sortedBy { it.artist }
                    SortField.DURATION -> artistSongs.sortedBy { it.duration }
                    SortField.SIZE -> artistSongs.sortedBy { it.size }
                }.let { if (ascending) it else it.reversed() }

                itemsIndexed(sortedArtist) { index, song ->
                    SongItem(
                        song = song,
                        onClick = { onSongClick(song, index, sortedArtist) },
                        isCurrent = song.uri == currentSong?.uri
                    )
                }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { selectedAlbum = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                        Text(text = selectedAlbum ?: "")
                    }
                }
                val albumSongs = allSongs.filter { it.album == selectedAlbum }
                val sortedAlbum = when (sortField) {
                    SortField.TITLE -> albumSongs.sortedBy { it.title }
                    SortField.ALBUM -> albumSongs.sortedBy { it.album }
                    SortField.ARTIST -> albumSongs.sortedBy { it.artist }
                    SortField.DURATION -> albumSongs.sortedBy { it.duration }
                    SortField.SIZE -> albumSongs.sortedBy { it.size }
                }.let { if (ascending) it else it.reversed() }

                itemsIndexed(sortedAlbum) { index, song ->
                    SongItem(
                        song = song,
                        onClick = { onSongClick(song, index, sortedAlbum) },
                        isCurrent = song.uri == currentSong?.uri
                    )
                }
            }
        }
    }
}
@Composable
fun AlbumItem(album: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(album) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
    HorizontalDivider()
}

@Composable
fun ArtistItem(artist: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(artist) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
    HorizontalDivider()
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
    PlaybackHolder.songs = songs
    val intent = Intent(context, MusicService::class.java).apply {
        action = MusicService.ACTION_START
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
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    shuffleOn: Boolean,
    repeatMode: Int,
    service: MusicService?,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    var sliderPosition by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(service, isPlaying) {
        while (true) {
            if (service != null) {
                duration = service.getDuration().toFloat()
                if (!dragging && service.isPlaying()) {
                    sliderPosition = service.getCurrentPosition().toFloat()
                }
            }
            delay(500)
        }
    }

    Surface(shadowElevation = 4.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                IconButton(onClick = onShuffle) {
                    val tint = if (shuffleOn) PinkAccent else MaterialTheme.colorScheme.onSurface
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = tint)
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(onClick = onToggle) {
                    val icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                    val desc = if (isPlaying) "Pause" else "Play"
                    Icon(icon, contentDescription = desc)
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
                IconButton(onClick = onRepeat) {
                    val icon = when (repeatMode) {
                        1 -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    }
                    val tint = if (repeatMode == 0) MaterialTheme.colorScheme.onSurface else PinkAccent
                    Icon(icon, contentDescription = "Repeat", tint = tint)
                }
            }
            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                    dragging = true
                },
                onValueChangeFinished = {
                    service?.seekTo(sliderPosition.toInt())
                    dragging = false
                },
                valueRange = 0f..duration,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
}
