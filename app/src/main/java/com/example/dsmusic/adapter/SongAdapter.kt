package com.example.dsmusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.dsmusic.R
import com.example.dsmusic.model.Song
import com.example.dsmusic.utils.PlaylistManager

class SongAdapter(
    private val songs: List<Song>,
    private val onClick: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtTitle)
        val artist: TextView = view.findViewById(R.id.txtArtist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        holder.artist.text = song.artist
        holder.itemView.setOnClickListener { onClick(song) }
        holder.itemView.setOnLongClickListener {
            val context = holder.itemView.context
            val playlists = PlaylistManager.getAllPlaylists(context)
            if (playlists.isEmpty()) return@setOnLongClickListener true
            val names = playlists.map { it.name }.toTypedArray()
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.add_to_playlist))
                .setItems(names) { _, which ->
                    val playlist = playlists[which]
                    if (!playlist.songs.contains(song)) {
                        playlist.songs.add(song)
                        PlaylistManager.updatePlaylist(context, playlist)
                        Toast.makeText(context, "Ajouté à ${playlist.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.song_already_in_playlist), Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
            true
        }
    }

    override fun getItemCount(): Int = songs.size
}
