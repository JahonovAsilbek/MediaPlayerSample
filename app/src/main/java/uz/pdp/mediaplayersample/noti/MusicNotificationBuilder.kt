package uz.pdp.mediaplayersample.noti

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import uz.pdp.mediaplayersample.MainActivity
import uz.pdp.mediaplayersample.service.MusicWidget
import uz.pdp.mediaplayersample.R
import uz.pdp.mediaplayersample.constants.*
import uz.pdp.mediaplayersample.models.Song

class MusicNotificationBuilder(private val context: Context,
                               private val channelId: String) {

    private val notificationIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    private val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

    fun build(song: Song,
              albumArt: Bitmap,
              isPlaying: Boolean,
              isShuffleOn: Boolean,
              mediaSession: MediaSessionCompat.Token): Notification {
        val shuffleIcon = if (isShuffleOn) R.drawable.shuffle_on else R.drawable.shuffle_off
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause_white_36dp else R.drawable.ic_play_arrow_white_36dp

        return NotificationCompat.Builder(context, channelId) // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Add media control buttons that invoke intents in your media service
                .addAction(shuffleIcon, null, MusicWidget.getPendingIntent(context, ACTION_SHUFFLE))
                .addAction(R.drawable.ic_skip_previous_white_36dp, null, MusicWidget.getPendingIntent(context, ACTION_PREVIOUS))
                .addAction(playPauseIcon, null, MusicWidget.getPendingIntent(context, ACTION_PLAY_PAUSE))
                .addAction(R.drawable.ic_skip_next_white_36dp, null, MusicWidget.getPendingIntent(context, ACTION_NEXT))
                .addAction(R.drawable.ic_stop_white_36dp, null, MusicWidget.getPendingIntent(context, ACTION_STOP))
                .setLargeIcon(albumArt)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setTicker(song.title)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession)
                        .setShowActionsInCompactView(1, 2, 3))
                .build()
    }
}