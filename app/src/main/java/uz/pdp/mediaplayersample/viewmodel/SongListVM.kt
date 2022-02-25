package uz.pdp.mediaplayersample.viewmodel

import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import uz.pdp.mediaplayersample.load.MusicLoader
import uz.pdp.mediaplayersample.load.SongListLoader
import uz.pdp.mediaplayersample.models.Song

class SongListVM(
    private val songLoader: SongListLoader,
    private val musicLoader: MusicLoader
) : ViewModel() {

    private val tag = javaClass.simpleName
    private var currentSearch: Job? = null
    val cursorLD = MutableLiveData<Cursor?>()
    val currentPosLD = MutableLiveData<Int>()

    fun getCursor(query: String? = null) {
        currentSearch?.cancel("Cancelling previous search")
        currentSearch = viewModelScope.launch(Dispatchers.IO) {
            Log.i(tag, "Getting song list cursor for query: $query")
//            delay(5000)
            val isNotSearching = query.isNullOrBlank()
            val cursor =
                if (isNotSearching) songLoader.getCursor()
                else songLoader.getFilteredCursor(query ?: "")

            cursorLD.postValue(cursor)
            if (isNotSearching && (cursor != null)) {
                val currentIndex = getCurrentSongIndex(cursor, musicLoader.getCurrent())
                currentPosLD.postValue(currentIndex)
            }
            Log.i(tag, "Song list cursor retrieved")
        }
    }

    private fun getCurrentSongIndex(cur: Cursor?, current: Song): Int {
        cur?.let {
            while (cur.moveToNext()) {
                val id = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media._ID))
                if (id == current.id) {
                    return cur.position
                }
            }
        }
        return 0
    }
}