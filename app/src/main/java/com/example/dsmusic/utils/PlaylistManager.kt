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
        return try {
            Gson().fromJson<MutableList<Playlist>?>(
                json,
                object : TypeToken<MutableList<Playlist>>() {}.type
            ) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
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
        val playlists = getAllPlaylists(context)
        val index = playlists.indexOfFirst { it.name == oldName }
        if (index != -1) {
            val songs = playlists[index].songs
            playlists[index] = Playlist(newName, songs)
            savePlaylists(context, playlists)
        }
    }

    fun deletePlaylist(context: Context, name: String) {
        val playlists = getAllPlaylists(context).filter { it.name != name }
        savePlaylists(context, playlists)
    }
}
