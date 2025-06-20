package com.example.dsmusic

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dsmusic.adapter.SongAdapter
import com.example.dsmusic.model.Playlist
import com.example.dsmusic.model.Song
import com.example.dsmusic.utils.MusicScanner
import com.example.dsmusic.utils.PlaylistManager
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private lateinit var seekBar: SeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtTotalTime: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var songs: MutableList<Song>
    private var currentIndex = 0
    private var isShuffling = false
    private var repeatMode = 0 // 0 = none, 1 = song, 2 = playlist

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerSongs)
        seekBar = findViewById(R.id.seekBar)
        txtCurrentTime = findViewById(R.id.txtCurrentTime)
        txtTotalTime = findViewById(R.id.txtTotalTime)

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
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SongAdapter(songs) { song ->
            currentIndex = songs.indexOf(song)
            playSong(song)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<Button>(R.id.btnEqualizer).setOnClickListener {
            mediaPlayer?.let {
                val intent = Intent(this, EqualizerActivity::class.java)
                intent.putExtra("AUDIO_SESSION_ID", it.audioSessionId)
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnPlaylists).setOnClickListener {
            val intent = Intent(this, PlaylistActivity::class.java)
            startActivity(intent)
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
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.path)
            prepare()
            start()

            setOnCompletionListener {
                when (repeatMode) {
                    1 -> playSong(songs[currentIndex])
                    else -> nextSong()
                }
            }

            seekBar.max = duration
            txtTotalTime.text = formatTime(duration)
            updateSeekBar()
        }

        // Release previous effects
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()

        equalizer = Equalizer(0, mediaPlayer!!.audioSessionId).apply {
            enabled = true
        }

        bassBoost = BassBoost(0, mediaPlayer!!.audioSessionId).apply {
            setStrength(800.toShort())
            enabled = true
        }

        virtualizer = Virtualizer(0, mediaPlayer!!.audioSessionId).apply {
            setStrength(800.toShort())
            enabled = true
        }
        Toast.makeText(this, "Lecture : ${song.title}", Toast.LENGTH_SHORT).show()
    }

    private fun nextSong() {
        mediaPlayer?.stop()
        mediaPlayer?.release()

        currentIndex = if (isShuffling) {
            (songs.indices - currentIndex).random()
        } else {
            (currentIndex + 1) % songs.size
        }

        if (repeatMode == 0 && currentIndex == 0 && !isShuffling) {
            Toast.makeText(this, "Fin de la playlist", Toast.LENGTH_SHORT).show()
            return
        }

        playSong(songs[currentIndex])
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    seekBar.progress = it.currentPosition
                    txtCurrentTime.text = formatTime(it.currentPosition)
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
    }
}
