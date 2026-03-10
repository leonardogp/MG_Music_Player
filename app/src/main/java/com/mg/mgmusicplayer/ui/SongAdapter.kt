package com.mg.mgmusicplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mg.mgmusicplayer.R
import com.mg.mgmusicplayer.data.Song

class SongAdapter(
    private val songs: List<Song>,
    private val onClick: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val title: TextView = view.findViewById(R.id.songTitle)
        val artist: TextView = view.findViewById(R.id.songArtist)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)

        return SongViewHolder(view)
    }

    override fun getItemCount() = songs.size

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {

        val song = songs[position]

        holder.title.text = song.title
        holder.artist.text = song.artist

        holder.itemView.setOnClickListener {

            onClick(song)

        }
    }
}