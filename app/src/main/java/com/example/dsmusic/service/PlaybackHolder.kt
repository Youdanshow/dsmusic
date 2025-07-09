package com.example.dsmusic.service

import com.example.dsmusic.model.Song

/**
 * Holds the current playlist in memory so we don't send large extras
 * via intents which can exceed binder transaction limits.
 */
object PlaybackHolder {
    var songs: List<Song> = emptyList()
}
