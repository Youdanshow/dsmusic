package com.example.dsmusic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.dsmusic.MainActivity
import com.example.dsmusic.R
import com.example.dsmusic.model.Song
import com.google.gson.Gson

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var songs: MutableList<Song> = mutableListOf()
    private var currentIndex: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val json = intent.getStringExtra(EXTRA_SONGS)
                if (json != null) {
                    val list = Gson().fromJson(json, Array<Song>::class.java)
                    songs = list.toMutableList()
                    currentIndex = intent.getIntExtra(EXTRA_INDEX, 0)
                    playSong(songs[currentIndex])
                }
            }
            ACTION_PLAY -> mediaPlayer?.start()
            ACTION_PAUSE -> mediaPlayer?.pause()
            ACTION_NEXT -> nextSong()
            ACTION_PREV -> previousSong()
            ACTION_STOP -> stopSelf()
        }
        if (mediaPlayer != null) updateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        stopForeground(true)
    }

    private fun playSong(song: Song) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.path)
            prepare()
            start()
            setOnCompletionListener { nextSong() }
        }
        startForeground(NOTIF_ID, buildNotification(song))
    }

    private fun nextSong() {
        if (songs.isEmpty()) return
        currentIndex = (currentIndex + 1) % songs.size
        playSong(songs[currentIndex])
    }

    private fun previousSong() {
        if (songs.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) songs.size - 1 else currentIndex - 1
        playSong(songs[currentIndex])
    }

    private fun buildNotification(song: Song): Notification {
        val channelId = createChannel()
        val playPauseAction = if (mediaPlayer?.isPlaying == true) ACTION_PAUSE else ACTION_PLAY
        val playPauseIcon = if (mediaPlayer?.isPlaying == true) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseIntent = PendingIntent.getService(this, 0, Intent(this, MusicService::class.java).apply {
            action = playPauseAction
        }, PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable())

        val nextIntent = PendingIntent.getService(this, 1, Intent(this, MusicService::class.java).apply {
            action = ACTION_NEXT
        }, PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable())

        val prevIntent = PendingIntent.getService(this, 2, Intent(this, MusicService::class.java).apply {
            action = ACTION_PREV
        }, PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable())

        val stopIntent = PendingIntent.getService(this, 3, Intent(this, MusicService::class.java).apply {
            action = ACTION_STOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable())

        val contentIntent = PendingIntent.getActivity(this, 4, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable())

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_media_previous, "Prev", prevIntent)
            .addAction(playPauseIcon, "PlayPause", playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", stopIntent)
            .setOngoing(mediaPlayer?.isPlaying == true)
            .build()
    }

    private fun updateNotification() {
        val song = songs.getOrNull(currentIndex) ?: return
        val notification = buildNotification(song)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, notification)
    }

    private fun createChannel(): String {
        val channelId = "music_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Music", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        return channelId
    }

    private fun flagImmutable(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }

    companion object {
        const val ACTION_START = "action_start"
        const val ACTION_PLAY = "action_play"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_PREV = "action_prev"
        const val ACTION_STOP = "action_stop"

        const val EXTRA_SONGS = "extra_songs"
        const val EXTRA_INDEX = "extra_index"

        private const val NOTIF_ID = 1
    }
}

