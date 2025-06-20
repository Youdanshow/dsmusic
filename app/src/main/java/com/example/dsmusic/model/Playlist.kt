package com.example.dsmusic.model

import com.example.dsmusic.model.Song

data class Playlist(
    val name: String,
    val songs: MutableList<Song>
)
