package uz.pdp.mediaplayersample.helpers

import uz.pdp.mediaplayersample.models.Song

class Events {
    class SongChanged(val track: Song?)
    class NextSongChanged(val track: Song?)
    class SongStateChanged(val isPlaying: Boolean)
    class ProgressUpdated(val progress: Int,val current: Song)
    class SleepTimerChanged(val milliSeconds: Long)
    class PlaylistsUpdated
}