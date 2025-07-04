package com.example.dsmusic.model

import android.net.Uri
import com.google.gson.annotations.SerializedName

/**
 * Represents an audio track in the library.
 *
 * @property title  Display name of the track
 * @property artist Name of the artist
 * @property uri    [Uri] string pointing to the media in {@link MediaStore}
 */
data class Song(
    val title: String,
    val artist: String,
    /**
     * String representation of the content {@link Uri} of the audio file.
     * Using a Uri instead of a raw file path ensures compatibility with
     * scoped storage on Android 10+.
     */
    @SerializedName(value = "uri", alternate = ["path"])
    val uri: String
)
