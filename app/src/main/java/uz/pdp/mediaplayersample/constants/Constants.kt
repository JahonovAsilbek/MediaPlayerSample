package uz.pdp.mediaplayersample.constants

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

const val PATH = "uz.pdp.music.action"

const val ACTION_PLAY_PAUSE = PATH + "play_pause"
const val ACTION_STOP = PATH + "stop"
const val ACTION_NEXT = PATH + "next"
const val ACTION_PREVIOUS = PATH + "previous"
const val ACTION_SHUFFLE = PATH + "shuffle"
const val ACTION_JUMP_TO = PATH + "jump_to"
const val PROGRESS = "progress"
const val ACTION_SEEK_TO = PATH + "seek_to"
const val FINISH = PATH + "finish"
const val START_SLEEP_TIMER = PATH + "start_sleep_timer"
const val STOP_SLEEP_TIMER = PATH + "stop_sleep_timer"
const val PREFS_KEY = "Prefs"
const val SONG_LAST_POSITION = "song_last_position"
const val PLAYBACK_SPEED = "playback_speed"
const val PLAYBACK_SPEED_PROGRESS = "playback_speed_progress"
const val SET_PLAYBACK_SPEED = PATH + "SET_PLAYBACK_SPEED"
const val SLEEP_TIME = PATH + "sleep_time"



@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
fun isOreoPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
fun isMarshmallowPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M