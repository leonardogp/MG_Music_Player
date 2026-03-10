package com.mg.mgmusicplayer.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mg.mgmusicplayer.R
import com.mg.mgmusicplayer.player.MusicPlayerManager
import com.mg.mgmusicplayer.util.MusicScanner

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var player: MusicPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerSongs)

        player = MusicPlayerManager(this)

        requestPermission()

        val songs = MusicScanner.getSongs(this)

        val adapter = SongAdapter(songs) {

            player.play(it)

        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun requestPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 1)

        } else {

            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        }
    }
}