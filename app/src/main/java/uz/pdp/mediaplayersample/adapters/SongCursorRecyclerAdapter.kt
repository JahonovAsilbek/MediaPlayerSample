package uz.pdp.mediaplayersample.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import uz.pdp.mediaplayersample.R
import uz.pdp.mediaplayersample.databinding.SongListRowBinding
import uz.pdp.mediaplayersample.models.Song

class SongCursorRecyclerAdapter(
    cursor: Cursor?,
    private val context: Context,
    private val listenerSong: SongClickListener
) : CursorRecyclerAdapter<SongCursorRecyclerAdapter.SongViewHolder>(cursor) {

    override fun onBindViewHolder(holder: SongViewHolder, cursor: Cursor) {
        holder.apply {
            val song = getSongFromCurrentCursorPos(cursor)
            if (song.isUnknownArtist) {
                artist.visibility = View.GONE
            } else {
                artist.visibility = View.VISIBLE
            }
            artist.text = song.artist
            title.text = song.title
            duration.text = song.getDurationStr()

            Glide.with(context)
                .load(song.albumArtPath)
                .override(512)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .placeholder(R.drawable.ic_launcher_background)
                .into(albumArt)
        }
    }

    inner class SongViewHolder(
        binding: SongListRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val title: TextView = binding.textTitle
        val artist: TextView = binding.textArtist
        val duration: TextView = binding.textDuration
        val albumArt: ImageView = binding.albumArt

        init {
            binding.root.setOnClickListener {
                check(cursor?.moveToPosition(adapterPosition) == true) { "couldn't move cursor to position $adapterPosition" }
                listenerSong.onSongSelected(getSongFromCurrentCursorPos(cursor!!))
            }
        }
    }

    @SuppressLint("Range")
    private fun getSongFromCurrentCursorPos(cursor: Cursor): Song =
        with(cursor) {
            val id = getLong(getColumnIndex(MediaStore.Audio.Media._ID))
            val artist = getString(getColumnIndex(MediaStore.Audio.Media.ARTIST))
            val title = getString(getColumnIndex(MediaStore.Audio.Media.TITLE))
            val duration = getLong(getColumnIndex(MediaStore.Audio.Media.DURATION))
            val albumId = getLong(getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
            return Song(id, title, artist, duration, albumId)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = SongListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    interface SongClickListener {
        fun onSongSelected(song: Song)
    }
}