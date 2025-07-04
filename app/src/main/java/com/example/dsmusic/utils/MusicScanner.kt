package com.example.dsmusic.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.provider.MediaStore.Audio.Media._ID
import android.provider.MediaStore.Audio.Media.TITLE
import android.provider.MediaStore.Audio.Media.ARTIST
import com.example.dsmusic.model.Song

/**
 * Utility object used to scan the device for audio files using [MediaStore].
 */
object MusicScanner {
    fun getAllAudioFiles(context: Context): List<Song> {
        val songs = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media._ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (cursor.moveToNext()) {
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val id = cursor.getLong(idColumn)
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                songs.add(Song(title, artist, uri.toString()))
            }
        }
        return songs
    }
}
