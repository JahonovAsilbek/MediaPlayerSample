package uz.pdp.mediaplayersample.load

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import uz.pdp.mediaplayersample.models.Song

const val ALBUM_ART_SIZE = 512

class AlbumArtLoader(val context: Context) {

    fun getAlbumArt(song: Song): Bitmap? =
        try {
            Glide.with(context)
                .asBitmap()
                .override(ALBUM_ART_SIZE)
                .load(song.albumArtPath)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .submit()
                .get()
        } catch (e: Exception) {
//                e.printStackTrace()
            null
        }
}