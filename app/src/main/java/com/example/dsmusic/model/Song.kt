package com.example.dsmusic.model

data class Song(
    val title: String,
    val artist: String,
    val album: String = "",
    val uri: String,
    val duration: Long = 0L,
    val size: Long = 0L
)
