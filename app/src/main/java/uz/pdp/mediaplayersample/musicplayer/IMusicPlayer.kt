package uz.pdp.mediaplayersample.musicplayer

import uz.pdp.mediaplayersample.models.Song
import java.io.IOException

interface IMusicPlayer {

    @Throws(
        IllegalArgumentException::class,
        SecurityException::class,
        IllegalStateException::class,
        IOException::class
    )
    fun setSong(song: Song, forcePlay: Boolean)
    fun isPlaying(): Boolean
    fun isPaused(): Boolean
    fun isStopped(): Boolean
    fun play()
    fun pause()
    fun stop()
    fun getPosition(): Long
    fun seekTo(position: Int)
    fun getDuration():Int
    fun setPlaybackSpeed(speed:Float)

    /**
     * Register a callback to be invoked when the end of a song has been reached during playback
     */
    fun setOnCompletionListener(listener: MusicPlayerCompletionListener)
}