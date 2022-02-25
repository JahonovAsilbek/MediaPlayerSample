package uz.pdp.mediaplayersample.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import uz.pdp.mediaplayersample.constants.*

class MySessionCallback(val context: Context) : MediaSessionCompat.Callback() {

    override fun onPlay() = callService(ACTION_PLAY_PAUSE)

    override fun onPause() = callService(ACTION_PLAY_PAUSE)

    override fun onSetShuffleMode(shuffleMode: Int) = callService(ACTION_SHUFFLE)

    override fun onStop() = callService(ACTION_STOP)

    override fun onSkipToNext() = callService(ACTION_NEXT)

    override fun onSkipToPrevious() = callService(ACTION_PREVIOUS)

    private fun callService(action: String) {
        Intent(context, MusicService::class.java).also {
            it.action = action
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(it)
            } else {
                context.startService(it)
            }
        }
    }
}