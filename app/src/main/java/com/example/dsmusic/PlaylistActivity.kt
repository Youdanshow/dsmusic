package com.example.dsmusic

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dsmusic.adapter.PlaylistAdapter
import com.example.dsmusic.model.Playlist
import com.example.dsmusic.utils.PlaylistManager
import com.google.gson.Gson

class PlaylistActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var playlists: MutableList<Playlist>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

        recycler = findViewById(R.id.recyclerPlaylists)
        playlists = PlaylistManager.getAllPlaylists(this)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = PlaylistAdapter(playlists) { playlist ->
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("PLAYLIST", Gson().toJson(playlist))
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnNewPlaylist).setOnClickListener {
            val input = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Nom de la playlist")
                .setView(input)
                .setPositiveButton("Créer") { _, _ ->
                    val newPlaylist = Playlist(input.text.toString(), mutableListOf())
                    PlaylistManager.addPlaylist(this, newPlaylist)
                    playlists.add(newPlaylist)
                    recycler.adapter?.notifyDataSetChanged()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }
}
