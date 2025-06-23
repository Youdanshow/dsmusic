package com.example.dsmusic

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
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
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dsmusic.adapter.SongAdapter
import com.example.dsmusic.model.Playlist
import com.example.dsmusic.model.Song
import com.example.dsmusic.utils.MusicScanner
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private var mediaPlayer: MediaPlayer? = null
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

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationViews: RemoteViews
    private val CHANNEL_ID = "music_playback"
    private val NOTIFICATION_ID = 1
    private val ACTION_TOGGLE_PLAY = "com.example.dsmusic.TOGGLE_PLAY"
    private val ACTION_NEXT = "com.example.dsmusic.NEXT"
    private val ACTION_PREVIOUS = "com.example.dsmusic.PREVIOUS"
    private val ACTION_SEEK = "com.example.dsmusic.SEEK"
    private val EXTRA_PROGRESS = "PROGRESS"

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    btnPlayPause.text = "\u25B6\uFE0F"
                } else {
                    it.start()
                    btnPlayPause.text = "\u23F8\uFE0F"
                }
                songs.getOrNull(currentIndex)?.let { song ->
                    showNotification(song)
                }
            }
        }
    }

    private val nextReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            nextSong()
        }
    }

    private val previousReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            previousSong()
        }
    }

    private val seekReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val progress = intent?.getIntExtra(EXTRA_PROGRESS, -1) ?: -1
            if (progress >= 0) {
                mediaPlayer?.seekTo(progress)
                seekBar.progress = progress
                songs.getOrNull(currentIndex)?.let { showNotification(it) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ContextCompat.registerReceiver(
            this,
            toggleReceiver,
            IntentFilter(ACTION_TOGGLE_PLAY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this,
            nextReceiver,
            IntentFilter(ACTION_NEXT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this,
            previousReceiver,
            IntentFilter(ACTION_PREVIOUS),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this,
            seekReceiver,
            IntentFilter(ACTION_SEEK),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

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

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

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
                    mediaPlayer?.seekTo(progress)
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
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    btnPlayPause.text = "▶️"
                } else {
                    it.start()
                    btnPlayPause.text = "⏸️"
                }
            }
        }

        btnForward10.setOnClickListener {
            mediaPlayer?.let {
                val newPos = it.currentPosition + 10000
                it.seekTo(if (newPos > it.duration) it.duration else newPos)
            }
        }

        btnRewind10.setOnClickListener {
            mediaPlayer?.let {
                val newPos = it.currentPosition - 10000
                it.seekTo(if (newPos < 0) 0 else newPos)
            }
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

        showNotification(song)

        btnPlayPause.text = "⏸️"

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

    private fun previousSong() {
        mediaPlayer?.stop()
        mediaPlayer?.release()

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

        playSong(songs[currentIndex])
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    seekBar.progress = it.currentPosition
                    txtCurrentTime.text = formatTime(it.currentPosition)
                    if (::notificationBuilder.isInitialized && ::notificationViews.isInitialized) {
                        val progressText = "${formatTime(it.currentPosition)} / ${formatTime(it.duration)}"
                        notificationViews.setInt(R.id.notifSeekBar, "setProgress", it.currentPosition)
                        val playIcon = if (it.isPlaying) {
                            android.R.drawable.ic_media_pause
                        } else {
                            android.R.drawable.ic_media_play
                        }
                        notificationViews.setImageViewResource(R.id.btnNotifPlayPause, playIcon)
                        notificationBuilder.setSubText(progressText)
                        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
                    }
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

    private fun showNotification(song: Song) {
        val toggleIntent = Intent(ACTION_TOGGLE_PLAY).apply {
            `package` = packageName
        }
        val nextIntent = Intent(ACTION_NEXT).apply {
            `package` = packageName
        }
        val previousIntent = Intent(ACTION_PREVIOUS).apply {
            `package` = packageName
        }
        val seekIntent = Intent(ACTION_SEEK).apply {
            `package` = packageName
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val togglePending = PendingIntent.getBroadcast(this, 0, toggleIntent, flags)
        val nextPending = PendingIntent.getBroadcast(this, 1, nextIntent, flags)
        val previousPending = PendingIntent.getBroadcast(this, 2, previousIntent, flags)
        val seekPending = PendingIntent.getBroadcast(this, 3, seekIntent, flags)
        val playIcon = if (mediaPlayer?.isPlaying == true) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val current = mediaPlayer?.currentPosition ?: 0
        val duration = mediaPlayer?.duration ?: seekBar.max
        val progressText = "${formatTime(current)} / ${formatTime(duration)}"

        notificationViews = RemoteViews(packageName, R.layout.notification_playback)
        notificationViews.setTextViewText(R.id.notifTitle, song.title)
        notificationViews.setTextViewText(R.id.notifArtist, song.artist)
        notificationViews.setImageViewResource(R.id.btnNotifPlayPause, playIcon)
        notificationViews.setOnClickPendingIntent(R.id.btnNotifPrev, previousPending)
        notificationViews.setOnClickPendingIntent(R.id.btnNotifPlayPause, togglePending)
        notificationViews.setOnClickPendingIntent(R.id.btnNotifNext, nextPending)
        notificationViews.setInt(R.id.notifSeekBar, "setMax", seekBar.max)
        notificationViews.setInt(R.id.notifSeekBar, "setProgress", current)

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(playIcon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCustomContentView(notificationViews)
            .setCustomBigContentView(notificationViews)
            .setSubText(progressText)

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        notificationManager.cancel(NOTIFICATION_ID)
        unregisterReceiver(toggleReceiver)
        unregisterReceiver(nextReceiver)
        unregisterReceiver(previousReceiver)
        unregisterReceiver(seekReceiver)
    }
}

