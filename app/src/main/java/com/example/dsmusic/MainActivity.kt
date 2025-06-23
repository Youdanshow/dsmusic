package com.example.dsmusic

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dsmusic.adapter.SongAdapter
import com.example.dsmusic.model.Playlist
import com.example.dsmusic.model.Song
import com.example.dsmusic.utils.MusicScanner
import com.example.dsmusic.service.MusicService
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var seekBar: SeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtTotalTime: TextView
    private lateinit var btnPlayPause: Button
    private lateinit var btnNext: Button
    private lateinit var btnPrev: Button
    private lateinit var btnForward10: Button
    private lateinit var btnRewind10: Button
    private lateinit var btnRefresh: Button
    private lateinit var btnSearch: Button
    private lateinit var songAdapter: SongAdapter
    private lateinit var songs: MutableList<Song>
    private lateinit var allSongs: MutableList<Song>
    private var currentIndex = 0
    private var isShuffling = false
    private var repeatMode = 0 // 0 = none, 1 = song, 2 = playlist
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerSongs)
        seekBar = findViewById(R.id.seekBar)
        txtCurrentTime = findViewById(R.id.txtCurrentTime)
        txtTotalTime = findViewById(R.id.txtTotalTime)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnNext = findViewById(R.id.btnNext)
        btnPrev = findViewById(R.id.btnPrev)
        btnForward10 = findViewById(R.id.btnForward10)
        btnRewind10 = findViewById(R.id.btnRewind10)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnSearch = findViewById(R.id.btnSearch)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 1)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }

        val playlistJson = intent.getStringExtra("PLAYLIST")
        songs = if (playlistJson != null) {
            Gson().fromJson(playlistJson, Playlist::class.java).songs
        } else {
            MusicScanner.getAllAudioFiles(this).toMutableList()
        }
        allSongs = songs.toMutableList()
        recyclerView.layoutManager = LinearLayoutManager(this)
        songAdapter = SongAdapter(songs) { song ->
            currentIndex = songs.indexOf(song)
            playSong(song)
        }
        recyclerView.adapter = songAdapter

        // SeekBar disabled when using background service


        findViewById<Button>(R.id.btnPlaylists).setOnClickListener {
            val intent = Intent(this, PlaylistActivity::class.java)
            startActivity(intent)
        }

        btnSearch.setOnClickListener {
            val input = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Rechercher une musique")
                .setView(input)
                .setPositiveButton("Rechercher") { _, _ ->
                    val query = input.text.toString().trim()
                    val filtered = if (query.isEmpty()) {
                        allSongs
                    } else {
                        allSongs.filter {
                            it.title.contains(query, ignoreCase = true) ||
                                    it.artist.contains(query, ignoreCase = true)
                        }
                    }
                    songs.clear()
                    songs.addAll(filtered)
                    songAdapter.notifyDataSetChanged()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        btnRefresh.setOnClickListener {
            val newSongs = MusicScanner.getAllAudioFiles(this)
            songs.clear()
            songs.addAll(newSongs)
            songAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Liste mise à jour", Toast.LENGTH_SHORT).show()
        }

        btnNext.setOnClickListener { nextSong() }
        btnPrev.setOnClickListener { previousSong() }
        btnPlayPause.setOnClickListener {
            val action = if (isPlaying) MusicService.ACTION_PAUSE else MusicService.ACTION_PLAY
            val intent = Intent(this, MusicService::class.java).apply { this.action = action }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            isPlaying = !isPlaying
            btnPlayPause.text = if (isPlaying) "⏸️" else "▶️"
        }

        btnForward10.setOnClickListener {
            Toast.makeText(this, "Fonction indisponible", Toast.LENGTH_SHORT).show()
        }

        btnRewind10.setOnClickListener {
            Toast.makeText(this, "Fonction indisponible", Toast.LENGTH_SHORT).show()
        }

        val btnShuffle = findViewById<Button>(R.id.btnShuffle)
        val btnRepeat = findViewById<Button>(R.id.btnRepeat)

        btnShuffle.setOnClickListener {
            isShuffling = !isShuffling
            btnShuffle.text = if (isShuffling) "✅🔀" else "🔀"
            Toast.makeText(
                this,
                if (isShuffling) "Aléatoire activé" else "Aléatoire désactivé",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnRepeat.setOnClickListener {
            repeatMode = (repeatMode + 1) % 3
            val modeText = when (repeatMode) {
                1 -> "🔂"
                2 -> "🔁"
                else -> "⏭️"
            }
            btnRepeat.text = modeText
            val toastText = when (repeatMode) {
                1 -> "Répéter la chanson"
                2 -> "Répéter la playlist"
                else -> "Répétition désactivée"
            }
            Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show()
        }
    }

    private fun playSong(song: Song) {
        val intent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_START
            putExtra(MusicService.EXTRA_SONGS, Gson().toJson(songs))
            putExtra(MusicService.EXTRA_INDEX, currentIndex)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        btnPlayPause.text = "⏸️"
        Toast.makeText(this, "Lecture : ${song.title}", Toast.LENGTH_SHORT).show()
        isPlaying = true
    }

    private fun nextSong() {
        currentIndex = if (isShuffling) {
            (songs.indices - currentIndex).random()
        } else {
            (currentIndex + 1) % songs.size
        }

        if (repeatMode == 0 && currentIndex == 0 && !isShuffling) {
            Toast.makeText(this, "Fin de la playlist", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, MusicService::class.java).apply { action = MusicService.ACTION_NEXT }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun previousSong() {
        val atFirst = currentIndex == 0
        currentIndex = if (isShuffling) {
            (songs.indices - currentIndex).random()
        } else {
            if (currentIndex - 1 < 0) songs.size - 1 else currentIndex - 1
        }

        if (repeatMode == 0 && atFirst && !isShuffling) {
            Toast.makeText(this, "Début de la playlist", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, MusicService::class.java).apply { action = MusicService.ACTION_PREV }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun formatTime(ms: Int): String {
        val minutes = ms / 1000 / 60
        val seconds = (ms / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent(this, MusicService::class.java).apply { action = MusicService.ACTION_STOP }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }
}
