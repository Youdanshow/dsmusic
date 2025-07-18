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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistPlay
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.dsmusic.model.Song
import com.example.dsmusic.service.MusicService
import com.example.dsmusic.ui.theme.DSMusicTheme
import com.example.dsmusic.ui.theme.TextWhite
import com.example.dsmusic.utils.MusicScanner
import com.example.dsmusic.utils.PlaybackHolder
import com.example.dsmusic.utils.ThemePreference
import com.example.dsmusic.utils.PlaylistManager
import com.example.dsmusic.model.Playlist
import com.google.gson.Gson
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.view.WindowCompat
import android.widget.Toast
import android.graphics.Color as AndroidColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        requestAudioPermission()
        setContent {
            DSMusicTheme {
                MusicApp()
            }
        }
    }

    private fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = mutableListOf<String>()
            if (PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PermissionChecker.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (PermissionChecker.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (permissions.isNotEmpty()) {
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
                    .launch(permissions.toTypedArray())
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
    var selectedTheme by rememberSaveable { mutableStateOf(ThemePreference.loadTheme(context)) }
    val backgroundRes = when (selectedTheme) {
        1 -> R.drawable.back_1
        2 -> R.drawable.back_2
        3 -> R.drawable.back_3
        else -> R.drawable.back_4
    }
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

    DSMusicTheme {
        Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(8.dp)
                .alpha(0.6f)
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Color.Transparent) {
                    BottomScreen.values().forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            when (screen) {
                                BottomScreen.Home -> Icon(Icons.Default.Home, contentDescription = null)
                                BottomScreen.Search -> Icon(Icons.Default.Search, contentDescription = null)
                                BottomScreen.Playlist -> Icon(Icons.Default.PlaylistPlay, contentDescription = null)
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
                    }, currentSong = currentSong, showFilter = true, onThemeSelected = { theme ->
                        selectedTheme = theme
                        ThemePreference.saveTheme(context, theme)
                    })
                    BottomScreen.Search -> SearchScreen(songs, onSongClick = { song, index, list ->
                        startPlayback(context, list, index)
                        playlist = list
                        currentIndex = index
                        currentSong = song
                        isPlaying = true
                    }, currentSong = currentSong)
                    BottomScreen.Playlist -> PlaylistScreen()
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
}

}

enum class BottomScreen(val label: String) { Home("Accueil"), Search("Recherche"), Playlist("Playlists") }

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
    onThemeSelected: (Int) -> Unit = {}
) {
    var sortField by remember { mutableStateOf(SortField.TITLE) }
    var ascending by remember { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }

    val selectedSongs = remember { mutableStateListOf<Song>() }
    var selectionMode by remember { mutableStateOf(false) }
    var actionsExpanded by remember { mutableStateOf(false) }
    var addDialogOpen by remember { mutableStateOf(false) }
    var createDialogOpen by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var settingsOpen by remember { mutableStateOf(false) }
                if (selectionMode) {
                    IconButton(onClick = { selectionMode = false; selectedSongs.clear() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                } else {
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Paramètres", tint = Color.White)
                    }
                }
                if (settingsOpen) {
                    SettingsScreen(
                        onBack = { settingsOpen = false },
                        onThemeSelected = onThemeSelected
                    )
                }
                Box {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filtrer",
                                tint = Color.White
                            )
                        }
                        if (selectionMode) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedSongs.size.toString(), color = Color.White, fontSize = 12.sp)
                                IconButton(onClick = { actionsExpanded = true }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        SortField.values().forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field.label, color = Color.White) },
                                onClick = {
                                    sortField = field
                                    menuExpanded = false
                                },
                                modifier = Modifier.background(Color.Transparent)
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Ordre croissant", color = Color.White) },
                            onClick = {
                                ascending = true
                                menuExpanded = false
                            },
                            modifier = Modifier.background(Color.Transparent)
                        )
                        DropdownMenuItem(
                            text = { Text("Ordre décroissant", color = Color.White) },
                            onClick = {
                                ascending = false
                                menuExpanded = false
                            },
                            modifier = Modifier.background(Color.Transparent)
                        )
                        DropdownMenuItem(
                            text = { Text("Réinitialiser", color = Color.White) },
                            onClick = {
                                sortField = SortField.TITLE
                                ascending = true
                                menuExpanded = false
                            },
                            modifier = Modifier.background(Color.Transparent)
                        )
                    }
                    DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_playlist), color = Color.White) },
                            onClick = {
                                actionsExpanded = false
                                addDialogOpen = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Créer une playlist", color = Color.White) },
                            onClick = {
                                actionsExpanded = false
                                createDialogOpen = true
                            }
                        )
                    }
                }
            }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(sortedSongs) { index, song ->
                val isSel = selectedSongs.contains(song)
                SongItem(
                    song = song,
                    onClick = {
                        if (selectionMode) {
                            if (isSel) selectedSongs.remove(song) else selectedSongs.add(song)
                            if (selectedSongs.isEmpty()) selectionMode = false
                        } else {
                            onSongClick(song, index, sortedSongs)
                        }
                    },
                    isCurrent = song.uri == currentSong?.uri,
                    selectionMode = selectionMode,
                    isSelected = isSel,
                    onSelectionChange = { checked ->
                        if (checked) {
                            if (!isSel) selectedSongs.add(song)
                        } else {
                            selectedSongs.remove(song)
                        }
                        if (selectedSongs.isEmpty()) selectionMode = false
                    },
                    onLongClick = {
                        if (!selectionMode) {
                            selectionMode = true
                            selectedSongs.add(song)
                        }
                    }
                )
            }
        }

        if (addDialogOpen) {
            AddSongsToPlaylistDialog(
                songs = selectedSongs.toList(),
                onDismiss = { addDialogOpen = false }
            )
        }

        if (createDialogOpen) {
            CreatePlaylistWithSongsDialog(
                songs = selectedSongs.toList(),
                onDismiss = { createDialogOpen = false }
            )
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
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filtrer",
                        tint = Color.White
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    SortField.values().forEach { field ->
                        DropdownMenuItem(
                            text = { Text(field.label, color = Color.White) },
                            onClick = {
                                sortField = field
                                menuExpanded = false
                            },
                            modifier = Modifier.background(Color.Transparent)
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Ordre croissant", color = Color.White) },
                        onClick = {
                            ascending = true
                            menuExpanded = false
                        },
                        modifier = Modifier.background(Color.Transparent)
                    )
                    DropdownMenuItem(
                        text = { Text("Ordre décroissant", color = Color.White) },
                        onClick = {
                            ascending = false
                            menuExpanded = false
                        },
                        modifier = Modifier.background(Color.Transparent)
                    )
                    DropdownMenuItem(
                        text = { Text("Réinitialiser", color = Color.White) },
                        onClick = {
                            sortField = SortField.TITLE
                            ascending = true
                            menuExpanded = false
                        },
                        modifier = Modifier.background(Color.Transparent)
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
            placeholder = { Text("Rechercher", color = Color.White) },
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
                                    Icon(icon, contentDescription = null, tint = Color.White)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
                                    Icon(icon, contentDescription = null, tint = Color.White)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
                                Icon(icon, contentDescription = null, tint = Color.White)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
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
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
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
fun PlaylistScreen() {
    val context = LocalContext.current
    var playlists by remember { mutableStateOf(PlaylistManager.getAllPlaylists(context)) }
    var dialogOpen by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(true) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var songsFor by remember { mutableStateOf<Playlist?>(null) }

    LaunchedEffect(renameTarget) {
        renameText = renameTarget ?: ""
    }

    if (songsFor == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = { dialogOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    "+ Créer une playlist",
                    fontSize = 20.sp
                )
            }

            ListItem(
                headlineContent = { Text("Playlists") },
                trailingContent = {
                    IconButton(onClick = { expanded = !expanded }) {
                        val icon = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore
                        Icon(icon, contentDescription = null, tint = Color.White)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            )
            HorizontalDivider()
            if (expanded) {
                playlists.forEach { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { menuFor = playlist.name }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.White)
                                }
                                DropdownMenu(expanded = menuFor == playlist.name, onDismissRequest = { menuFor = null }) {
                                    DropdownMenuItem(
                                        text = { Text("Renommer", color = Color.White) },
                                        onClick = {
                                            renameTarget = playlist.name
                                            menuFor = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Supprimer", color = Color.White) },
                                        onClick = {
                                            deleteTarget = playlist.name
                                            menuFor = null
                                        }
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { songsFor = playlist }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newName.trim()
                        if (trimmed.isNotEmpty()) {
                            val playlist = Playlist(trimmed, mutableListOf())
                            PlaylistManager.addPlaylist(context, playlist)
                            playlists = PlaylistManager.getAllPlaylists(context)
                            newName = ""
                            dialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("Créer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { dialogOpen = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("Annuler", color = Color.White)
                }
            },
            title = { Text("Nom de la playlist", color = Color.White) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { if (it.length <= 20) newName = it },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        cursorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        )
    }

    renameTarget?.let { name ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = renameText.trim()
                        if (trimmed.isNotEmpty()) {
                            PlaylistManager.renamePlaylist(context, name, trimmed)
                            playlists = PlaylistManager.getAllPlaylists(context)
                            renameTarget = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) { Text("Renommer", color = Color.White) }
            },
            dismissButton = {
                TextButton(
                    onClick = { renameTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) { Text("Annuler", color = Color.White) }
            },
            title = { Text("Renommer la playlist", color = Color.White) },
            text = {
                TextField(
                    value = renameText,
                    onValueChange = { if (it.length <= 20) renameText = it },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        )
    }

    deleteTarget?.let { name ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        PlaylistManager.removePlaylist(context, name)
                        playlists = PlaylistManager.getAllPlaylists(context)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) { Text("Supprimer", color = Color.White) }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) { Text("Annuler", color = Color.White) }
            },
            title = { Text("Supprimer \"$name\" ?", color = Color.White) },
            text = { Text("Cette action est définitive.", color = Color.White) }
        )
    }

    songsFor?.let { playlist ->
        PlaylistSongsScreen(
            playlist = playlist,
            onBack = {
                playlists = PlaylistManager.getAllPlaylists(context)
                songsFor = null
            },
            onUpdate = {
                playlists = PlaylistManager.getAllPlaylists(context)
            }
        )
    }
}
@Composable
fun AlbumItem(album: String, onClick: () -> Unit) {
    val colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    ListItem(
        headlineContent = { Text(album) },
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
    HorizontalDivider()
}

@Composable
fun ArtistItem(artist: String, onClick: () -> Unit) {
    val colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    ListItem(
        headlineContent = { Text(artist) },
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
    HorizontalDivider()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    isCurrent: Boolean,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val colors = ListItemDefaults.colors(
        containerColor = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
        headlineColor = if (isCurrent) TextWhite else MaterialTheme.colorScheme.onSurface,
        supportingColor = TextWhite
    )

    ListItem(
        headlineContent = {
            Text(
                song.title,
                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                song.artist,
                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionChange(it) }
                )
            }
        },
        trailingContent = {
            if (!selectionMode) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.White)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_playlist)) },
                            onClick = {
                                menuExpanded = false
                                showDialog = true
                            }
                        )
                    }
                }
            }
        },
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    )

    if (showDialog) {
        AddToPlaylistDialog(song = song, onDismiss = { showDialog = false })
    }

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
fun AddToPlaylistDialog(song: Song, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playlists = remember { PlaylistManager.getAllPlaylists(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Annuler", color = Color.White)
            }
        },
        title = { Text(stringResource(R.string.add_to_playlist), color = Color.White) },
        text = {
            if (playlists.isEmpty()) {
                Text("Aucune playlist", color = Color.White)
            } else {
                Column {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = {
                                if (!playlist.songs.contains(song)) {
                                    playlist.songs.add(song)
                                    PlaylistManager.updatePlaylist(context, playlist)
                                    Toast.makeText(context, "Ajouté à ${playlist.name}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.song_already_in_playlist),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text(playlist.name, color = Color.White)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun AddSongsToPlaylistDialog(songs: List<Song>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playlists = remember { PlaylistManager.getAllPlaylists(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Annuler", color = Color.White)
            }
        },
        title = { Text(stringResource(R.string.add_to_playlist), color = Color.White) },
        text = {
            if (playlists.isEmpty()) {
                Text("Aucune playlist", color = Color.White)
            } else {
                Column {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = {
                                var updated = false
                                songs.forEach { if (!playlist.songs.contains(it)) {
                                    playlist.songs.add(it); updated = true }
                                }
                                if (updated) {
                                    PlaylistManager.updatePlaylist(context, playlist)
                                    Toast.makeText(context, "Ajouté à ${playlist.name}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.songs_already_in_playlist),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text(playlist.name, color = Color.White)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun CreatePlaylistWithSongsDialog(songs: List<Song>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        val playlist = Playlist(trimmed, songs.toMutableList())
                        PlaylistManager.addPlaylist(context, playlist)
                        Toast.makeText(context, "Playlist créée", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) { Text("Créer", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Annuler", color = Color.White) }
        },
        title = { Text("Nom de la playlist", color = Color.White) },
        text = {
            TextField(
                value = name,
                onValueChange = { if (it.length <= 20) name = it },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
    )
}

@Composable
fun PlaylistSongItem(song: Song, onClick: () -> Unit, onRemove: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(song.title, color = Color.White) },
        supportingContent = { Text(song.artist, color = Color.White) },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.White)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove_from_playlist), color = Color.White) },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        }
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
    HorizontalDivider()
}

@Composable
fun PlaylistSongsScreen(
    playlist: Playlist,
    onBack: () -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    var songs by remember { mutableStateOf(playlist.songs.toMutableList()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune musique", color = Color.White)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(songs) { index, song ->
                    PlaylistSongItem(
                        song = song,
                        onClick = { startPlayback(context, songs, index) },
                        onRemove = {
                            val updated = songs.toMutableList()
                            updated.remove(song)
                            songs = updated
                            PlaylistManager.updatePlaylist(context, playlist.copy(songs = updated))
                            onUpdate()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onThemeSelected: (Int) -> Unit) {
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Text("Param\u00e8tres", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(16.dp))
            Box {
                Button(
                    onClick = { themeMenuExpanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Changer de th\u00e8me")
                }
                DropdownMenu(expanded = themeMenuExpanded, onDismissRequest = { themeMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Th\u00e8me 1", color = Color.White) },
                        onClick = { onThemeSelected(1); themeMenuExpanded = false },
                        modifier = Modifier.background(Color.Transparent)
                    )
                    DropdownMenuItem(
                        text = { Text("Th\u00e8me 2", color = Color.White) },
                        onClick = { onThemeSelected(2); themeMenuExpanded = false },
                        modifier = Modifier.background(Color.Transparent)
                    )
                    DropdownMenuItem(
                        text = { Text("Th\u00e8me 3", color = Color.White) },
                        onClick = { onThemeSelected(3); themeMenuExpanded = false },
                        modifier = Modifier.background(Color.Transparent)
                    )
                    DropdownMenuItem(
                        text = { Text("Th\u00e8me 4", color = Color.White) },
                        onClick = { onThemeSelected(4); themeMenuExpanded = false },
                        modifier = Modifier.background(Color.Transparent)
                    )
                }
            }
        }
    }
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

    Surface(shadowElevation = 4.dp, color = Color.Transparent) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
                IconButton(onClick = onShuffle) {
                    val tint = if (shuffleOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = tint)
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                }
                IconButton(onClick = onToggle) {
                    val icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                    val desc = if (isPlaying) "Pause" else "Play"
                    Icon(icon, contentDescription = desc, tint = Color.White)
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
                }
                IconButton(onClick = onRepeat) {
                    val icon = when (repeatMode) {
                        1 -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    }
                    val tint = if (repeatMode == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
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
