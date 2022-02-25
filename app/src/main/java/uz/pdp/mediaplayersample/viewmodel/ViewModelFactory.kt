package uz.pdp.mediaplayersample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import uz.pdp.mediaplayersample.load.MusicLoader
import uz.pdp.mediaplayersample.load.SongListLoader

class ViewModelFactory(
    private val songListLoader: SongListLoader,
    private val musicLoader: MusicLoader
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongListVM::class.java)) {
            return SongListVM(songListLoader, musicLoader) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}