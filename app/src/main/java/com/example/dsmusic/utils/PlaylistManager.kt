package com.example.dsmusic.utils

import android.content.Context
import com.example.dsmusic.model.Playlist
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object PlaylistManager {
    private const val FILE_NAME = "playlists.json"

    fun getAllPlaylists(context: Context): MutableList<Playlist> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return mutableListOf()
        val json = file.readText()
        return Gson().fromJson(json, object : TypeToken<MutableList<Playlist>>() {}.type)
    }

    fun savePlaylists(context: Context, playlists: List<Playlist>) {
        val json = Gson().toJson(playlists)
        File(context.filesDir, FILE_NAME).writeText(json)
    }

    fun addPlaylist(context: Context, playlist: Playlist) {
        val all = getAllPlaylists(context)
        all.add(playlist)
        savePlaylists(context, all)
    }

    fun updatePlaylist(context: Context, updated: Playlist) {
        val all = getAllPlaylists(context).map {
            if (it.name == updated.name) updated else it
        }
        savePlaylists(context, all)
    }

    fun renamePlaylist(context: Context, oldName: String, newName: String) {
        val all = getAllPlaylists(context).map {
            if (it.name == oldName) Playlist(newName, it.songs) else it
        }
        savePlaylists(context, all)
    }

    fun removePlaylist(context: Context, name: String) {
        val all = getAllPlaylists(context).filter { it.name != name }
        savePlaylists(context, all)
    }
}
