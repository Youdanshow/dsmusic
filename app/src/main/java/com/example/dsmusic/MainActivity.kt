package com.example.dsmusic

import android.Manifest
import android.content.Intent
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import java.text.Normalizer
import java.util.Locale
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dsmusic.adapter.SongAdapter
import com.example.dsmusic.model.Playlist
import com.example.dsmusic.model.Song
import com.example.dsmusic.service.MusicService
import com.example.dsmusic.utils.MusicScanner
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private var musicService: MusicService? = null
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
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var songs: MutableList<Song>
    private lateinit var allSongs: MutableList<Song>
    private var currentIndex = 0
    private var isShuffling = false
    private var repeatMode = 0 // 0 = none, 1 = song, 2 = playlist

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as MusicService.LocalBinder).getService()
            updateSeekBar()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    private val ACTION_TOGGLE_PLAY = MusicService.ACTION_TOGGLE_PLAY
    private val ACTION_NEXT = MusicService.ACTION_NEXT
    private val ACTION_PREVIOUS = MusicService.ACTION_PREVIOUS


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)


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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                1
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
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

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


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
                    val normalizedQuery = normalize(query)
                    val filtered = if (normalizedQuery.isEmpty()) {
                        allSongs
                    } else {
                        allSongs.filter {
                            val title = normalize(it.title)
                            val artist = normalize(it.artist)
                            title.contains(normalizedQuery) || artist.contains(normalizedQuery)
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
            val intent = Intent(this, MusicService::class.java).apply {
                action = MusicService.ACTION_TOGGLE_PLAY
            }
            ContextCompat.startForegroundService(this, intent)
            btnPlayPause.text = if (musicService?.isPlaying() == true) "▶️" else "⏸️"
        }

        btnForward10.setOnClickListener {
            musicService?.let {
                val newPos = it.getCurrentPosition() + 10000
                it.seekTo(if (newPos > it.getDuration()) it.getDuration() else newPos)
            }
        }

        btnRewind10.setOnClickListener {
            musicService?.let {
                val newPos = it.getCurrentPosition() - 10000
                it.seekTo(if (newPos < 0) 0 else newPos)
            }
        }

        val btnShuffle = findViewById<Button>(R.id.btnShuffle)
        val btnRepeat = findViewById<Button>(R.id.btnRepeat)

        btnShuffle.setOnClickListener {
            isShuffling = !isShuffling
            musicService?.toggleShuffle()
            btnShuffle.text = if (isShuffling) "✅🔀" else "🔀"
            Toast.makeText(
                this,
                if (isShuffling) "Aléatoire activé" else "Aléatoire désactivé",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnRepeat.setOnClickListener {
            repeatMode = (repeatMode + 1) % 3
            musicService?.cycleRepeatMode()
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
            putExtra("SONGS", Gson().toJson(songs))
            putExtra("INDEX", currentIndex)
        }
        ContextCompat.startForegroundService(this, intent)
        handler.post { updateSeekBar() }
        btnPlayPause.text = "⏸️"
        Toast.makeText(this, "Lecture : ${song.title}", Toast.LENGTH_SHORT).show()
    }

    private fun nextSong() {
        currentIndex = if (isShuffling) {
            (songs.indices - currentIndex).random()
        } else {
            (currentIndex + 1) % songs.size
        }
        val intent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_NEXT
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun previousSong() {
        val atFirst = currentIndex == 0
        currentIndex = if (isShuffling) {
            (songs.indices - currentIndex).random()
        } else {
            if (currentIndex - 1 < 0) songs.size - 1 else currentIndex - 1
        }
        val intent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_PREVIOUS
        }
        ContextCompat.startForegroundService(this, intent)
        if (repeatMode == 0 && atFirst && !isShuffling) {
            Toast.makeText(this, "Début de la playlist", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                musicService?.let {
                    seekBar.max = it.getDuration()
                    val pos = it.getCurrentPosition()
                    seekBar.progress = pos
                    txtCurrentTime.text = formatTime(pos)
                    txtTotalTime.text = formatTime(it.getDuration())
                    handler.postDelayed(this, 500)
                }
            }
        }, 0)
    }

    private fun formatTime(ms: Int): String {
        val minutes = ms / 1000 / 60
        val seconds = (ms / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun normalize(text: String): String {
        val temp = Normalizer.normalize(text.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
        val noDiacritics = temp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return noDiacritics.replace("[^a-z0-9]".toRegex(), "")
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        unbindService(serviceConnection)
    }
}
