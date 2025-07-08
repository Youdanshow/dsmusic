package com.example.dsmusic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.dsmusic.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.media.MediaPlayer
import com.example.dsmusic.R
import android.net.Uri

class MusicService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private val binder = LocalBinder()

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val CHANNEL_ID = "music_playback"
    private val NOTIFICATION_ID = 1

    private var mediaPlayer: MediaPlayer? = null
    private var songs = mutableListOf<Song>()
    private var currentIndex = 0
    private var isShuffling = false
    private var repeatMode = 0 // 0 = none, 1 = song, 2 = playlist

    companion object {
        const val ACTION_START = "com.example.dsmusic.START"
        const val ACTION_TOGGLE_PLAY = "com.example.dsmusic.TOGGLE_PLAY"
        const val ACTION_NEXT = "com.example.dsmusic.NEXT"
        const val ACTION_PREVIOUS = "com.example.dsmusic.PREVIOUS"
        const val ACTION_STOP = "com.example.dsmusic.STOP"
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val songsJson = intent.getStringExtra("SONGS") ?: return START_NOT_STICKY
                songs = Gson().fromJson(songsJson, object : TypeToken<MutableList<Song>>() {}.type)
                currentIndex = intent.getIntExtra("INDEX", 0)
                playSong(songs[currentIndex])
            }
            ACTION_TOGGLE_PLAY -> togglePlay()
            ACTION_NEXT -> nextSong()
            ACTION_PREVIOUS -> previousSong()
            ACTION_STOP -> {
                stopForeground(true)
                mediaPlayer?.release()
                stopSelf()
            }
        }
        return START_STICKY
    }

    fun seekTo(pos: Int) {
        mediaPlayer?.seekTo(pos)
    }

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getCurrentSong(): Song? = songs.getOrNull(currentIndex)

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    fun toggleShuffle() {
        isShuffling = !isShuffling
    }

    fun cycleRepeatMode() {
        repeatMode = (repeatMode + 1) % 3
    }

    private fun togglePlay() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.start()
            }
            songs.getOrNull(currentIndex)?.let { song ->
                showNotification(song)
            }
        }
    }

    fun nextSong() {
        mediaPlayer?.stop()
        mediaPlayer?.release()

        currentIndex = if (isShuffling) {
            (songs.indices - currentIndex).random()
        } else {
            (currentIndex + 1) % songs.size
        }

        if (repeatMode == 0 && currentIndex == 0 && !isShuffling) {
            return
        }

        playSong(songs[currentIndex])
    }

    fun previousSong() {
        mediaPlayer?.stop()
        mediaPlayer?.release()

        val atFirst = currentIndex == 0
        currentIndex = if (isShuffling) {
            (songs.indices - currentIndex).random()
        } else {
            if (currentIndex - 1 < 0) songs.size - 1 else currentIndex - 1
        }

        if (repeatMode == 0 && atFirst && !isShuffling) {
            return
        }

        playSong(songs[currentIndex])
    }

    private fun playSong(song: Song) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@MusicService, Uri.parse(song.uri))
            prepare()
            start()

            setOnCompletionListener {
                when (repeatMode) {
                    1 -> playSong(songs[currentIndex])
                    else -> nextSong()
                }
            }
        }
        showNotification(song)
    }

    private fun showNotification(song: Song) {
        val toggleIntent = Intent(this, MusicService::class.java).apply { action = ACTION_TOGGLE_PLAY }
        val nextIntent = Intent(this, MusicService::class.java).apply { action = ACTION_NEXT }
        val previousIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PREVIOUS }
        val stopIntent = Intent(this, MusicService::class.java).apply { action = ACTION_STOP }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val togglePending = PendingIntent.getService(this, 0, toggleIntent, flags)
        val nextPending = PendingIntent.getService(this, 1, nextIntent, flags)
        val previousPending = PendingIntent.getService(this, 2, previousIntent, flags)
        val stopPending = PendingIntent.getService(this, 3, stopIntent, flags)

        val playIcon = if (mediaPlayer?.isPlaying == true) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(playIcon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_delete, "❌", stopPending)
            .addAction(android.R.drawable.ic_media_previous, "⏪", previousPending)
            .addAction(playIcon, if (mediaPlayer?.isPlaying == true) "⏸️" else "▶️", togglePending)
            .addAction(android.R.drawable.ic_media_next, "⏩", nextPending)

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}

