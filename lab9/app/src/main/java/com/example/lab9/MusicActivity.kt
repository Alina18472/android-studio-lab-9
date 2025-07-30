
package com.example.lab9

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast

class MusicActivity : BaseActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private var currentTrack: Int = -1
    private var isPlaying = false
    private lateinit var tracks: List<Track>

    override fun onCreate(savedInstanceState: Bundle?) {
        currentNavItemId = R.id.nav_music

        super.onCreate(savedInstanceState)

        layoutInflater.inflate(R.layout.activity_music, findViewById(R.id.content_frame))


        tracks = RawResourceScanner.getRawTrackList(this)
        mediaPlayer = MediaPlayer()

        val trackListView = findViewById<ListView>(R.id.trackListView)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            tracks.map { it.name }
        )
        trackListView.adapter = adapter

        trackListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            playTrack(position)
        }

        findViewById<Button>(R.id.btnPlay).setOnClickListener { playCurrentTrack() }
        findViewById<Button>(R.id.btnPause).setOnClickListener { pauseTrack() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopTrack() }
    }

    private fun playTrack(position: Int) {
        if (currentTrack == position && isPlaying) return

        stopTrack()

        currentTrack = position
        mediaPlayer = MediaPlayer.create(this, tracks[position].resourceId)
        mediaPlayer.start()
        isPlaying = true

        mediaPlayer.setOnCompletionListener {
            isPlaying = false
            currentTrack = -1
        }
    }

    private fun playCurrentTrack() {
        if (currentTrack == -1) {
            if (tracks.isNotEmpty()) {
                playTrack(0)
            }
            return
        }

        if (!isPlaying) {
            mediaPlayer.start()
            isPlaying = true
        }
    }

    private fun pauseTrack() {
        if (isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
        }
    }

    private fun stopTrack() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()
        isPlaying = false
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }


    data class Track(val name: String, val resourceId: Int)
}
object RawResourceScanner {
    fun getRawTrackList(context: Context): List<MusicActivity.Track> {
        val tracks = mutableListOf<MusicActivity.Track>()
        val resources = context.resources


        try {
            // Получаем все файлы из папки raw
            val fields = R.raw::class.java.fields

            for (field in fields) {
                try {
                    val resId = field.getInt(null)
                    val resName = resources.getResourceEntryName(resId)
                    tracks.add(MusicActivity.Track(resName.replace("_", " ").capitalize(), resId))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tracks
    }
}