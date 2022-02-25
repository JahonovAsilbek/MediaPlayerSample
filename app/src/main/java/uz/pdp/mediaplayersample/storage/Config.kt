package uz.pdp.mediaplayersample.storage

import android.content.Context
import android.content.SharedPreferences
import uz.pdp.mediaplayersample.constants.PLAYBACK_SPEED
import uz.pdp.mediaplayersample.constants.PLAYBACK_SPEED_PROGRESS
import uz.pdp.mediaplayersample.constants.SLEEP_TIME

object Config {
    private const val NAME = "token"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(NAME, MODE)
    }

    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.apply()
    }

    fun setSongLastPosition(SONG_NAME: String, position: Int) {
        sharedPreferences.edit().putInt(SONG_NAME, position).apply()
    }

    fun getSongLastPosition(SONG_NAME: String): Int = sharedPreferences.getInt(SONG_NAME, 0)

    var playbackSpeed: Float
        get() = sharedPreferences.getFloat(PLAYBACK_SPEED, 1f)
        set(playbackSpeed) = sharedPreferences.edit().putFloat(PLAYBACK_SPEED, playbackSpeed)
            .apply()

    var playbackSpeedProgress: Int
        get() = sharedPreferences.getInt(PLAYBACK_SPEED_PROGRESS, -1)
        set(playbackSpeedProgress) = sharedPreferences.edit()
            .putInt(PLAYBACK_SPEED_PROGRESS, playbackSpeedProgress).apply()

    var sleepTime: Int
        get() = sharedPreferences.getInt(SLEEP_TIME, 60 * 30)
        set(playbackSpeedProgress) = sharedPreferences.edit()
            .putInt(SLEEP_TIME, playbackSpeedProgress).apply()
}
