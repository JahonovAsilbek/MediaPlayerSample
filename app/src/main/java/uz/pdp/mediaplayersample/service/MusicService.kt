package uz.pdp.mediaplayersample.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.CursorIndexOutOfBoundsException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import uz.pdp.mediaplayersample.ConfigurationActivity
import uz.pdp.mediaplayersample.R
import uz.pdp.mediaplayersample.constants.*
import uz.pdp.mediaplayersample.helpers.Events
import uz.pdp.mediaplayersample.load.AlbumArtLoader
import uz.pdp.mediaplayersample.load.MusicLoader
import uz.pdp.mediaplayersample.models.Song
import uz.pdp.mediaplayersample.musicplayer.IMusicPlayer
import uz.pdp.mediaplayersample.musicplayer.MusicPlayerCompletionListener
import uz.pdp.mediaplayersample.musicplayer.MyExoPlayer
import uz.pdp.mediaplayersample.needsToRequestPermissions
import uz.pdp.mediaplayersample.noti.MusicNotificationBuilder
import uz.pdp.mediaplayersample.storage.Config
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class MusicService : MediaBrowserServiceCompat(), MusicPlayerCompletionListener {

    private lateinit var musicLoader: MusicLoader
    private lateinit var albumArtLoader: AlbumArtLoader
    private val job = Job()
    private var mProgressHandler = Handler()
    private var mPlaybackSpeed = 1f
    private var mSleepTimer: CountDownTimer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var mediaSessionCompat: MediaSessionCompat
    private var currFlipperState = ViewFlipperState.STOPPED
    private lateinit var player: IMusicPlayer
    private val notificationManager: NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val notificationBuilder: MusicNotificationBuilder by lazy {
        MusicNotificationBuilder(
            this,
            channelId
        )
    }
    private val defaultAlbumArt: Bitmap by lazy {
        BitmapFactory.decodeResource(
            resources,
            R.drawable.album_white
        )
    }
    private var mPlayBackSpeed = 0f

    private fun getIsPlaying() = player.isPlaying()

    override fun onCreate() {
        super.onCreate()
        musicLoader = MusicLoader(this)
        albumArtLoader = AlbumArtLoader(this)
        Config.init(this)
        startForeground(
            ONGOING_NOTIFICATION_ID,
            NotificationCompat.Builder(this, channelId).setContentTitle("").setContentText("")
                .build()
        )

        player = MyExoPlayer(this).also {
            it.setOnCompletionListener(this)
        }

        mediaSessionCompat = MediaSessionCompat(applicationContext, TAG).also {
            it.setPlaybackState(getPlaybackStateBuilder().build())
            it.setCallback(MySessionCallback(applicationContext))
            sessionToken = it.sessionToken
        }
        listenForPhoneCalls()


    }

    private fun listenForPhoneCalls() {
        (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.listen(object :
            PhoneStateListener() {
            private val wasPlayingBeforeCall = AtomicBoolean(false)
            override fun onCallStateChanged(state: Int, phoneNumber: String) {
                try {
                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK ->
                            if (player.isPlaying()) {
                                wasPlayingBeforeCall.set(true)
                                pauseMusic()
                            }
                        TelephonyManager.CALL_STATE_IDLE ->
                            if (wasPlayingBeforeCall.getAndSet(false)) {
                                playMusic()
                            }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun getPlaybackStateBuilder() = PlaybackStateCompat.Builder()
        .setActions(
            PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
        )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        if (intent == null || intent.action == null) {
            return Service.START_STICKY
        }
        if (needsToRequestPermissions()) {
            openConfigActivity()
        } else {
            processStartCommand(intent)
            MediaButtonReceiver.handleIntent(mediaSessionCompat, intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun openConfigActivity() =
        startActivity(
            Intent(this, ConfigurationActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

    private fun processStartCommand(intent: Intent) {
        try {
            when (intent.action) {
                ACTION_PLAY_PAUSE ->
                    if (player.isPlaying()) pauseMusic()
                    else playMusic()
                ACTION_STOP -> stopMusic()
                ACTION_NEXT -> nextSong()
                ACTION_PREVIOUS -> previousSong()
                ACTION_SHUFFLE -> toggleShuffle()
                ACTION_SEEK_TO -> handleProgress(intent)
                ACTION_JUMP_TO -> jumpTo(intent.extras!!["song"] as Song)
                FINISH -> handleFinish()
                STOP_SLEEP_TIMER -> stopSleepTimer()
                START_SLEEP_TIMER -> startSleepTimer()
                SET_PLAYBACK_SPEED -> setPlaybackSpeed()
            }
        } catch (e: CursorIndexOutOfBoundsException) {
            Toast.makeText(this, R.string.message_no_music_found, Toast.LENGTH_SHORT).show()
            stopMusic()
        } catch (e: Exception) {
            e.printStackTrace()
            stopMusic()
        }
    }

    private fun setPlaybackSpeed() {
        if (isMarshmallowPlus()) {
            mPlaybackSpeed = Config.playbackSpeed
            Log.d("AAAA", "setPlaybackSpeed:$mPlaybackSpeed ")
            if (player.isPlaying()) {
                try {
                    player.setPlaybackSpeed(mPlaybackSpeed)
                } catch (ignored: Exception) {
                }
            }
        }
    }

    private fun handleProgress(intent: Intent) {
        if (player != null) {
            val progress = intent.getIntExtra("progress", (player.getPosition() / 1000).toInt())
            Log.d("AAAA", "handleProgress:${progress / 1000} ")
            updateProgress(progress)
        }
    }

    private fun updateProgress(progress: Int) {
        player.seekTo(progress / 1000)
    }

    private fun toggleShuffle() {
        musicLoader.toggleShuffle()

        // update the shuffle icon
        if (player.isStopped()) {
            stopMusic()
        } else {
            val song = musicLoader.getCurrent()
            updateUI(song, player.isPlaying())
        }
        val msg = if (musicLoader.isShuffleOn) {
            R.string.toast_shuffle_on
        } else {
            R.string.toast_shuffle_off
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        Log.d(TAG, "DESTROY SERVICE")
        isRunning = false
        if (!player.isStopped()) stopMusic()
        mediaSessionCompat.release()
        super.onDestroy()
    }

    private fun setPlaybackState(position: Long, statePlaying: Int) =
        mediaSessionCompat.setPlaybackState(
            getPlaybackStateBuilder()
                .setState(statePlaying, position, 1.5f)
                .build()
        )

    private fun updateUI(song: Song?, isPlaying: Boolean) {
        // Update widget
        coroutineScope.launch {
            var loadedAlbumArt: Bitmap? = null
            withContext(Dispatchers.IO) {
                loadedAlbumArt = song?.let { albumArtLoader.getAlbumArt(song) }
            }
            val albumArt = loadedAlbumArt ?: defaultAlbumArt

            // We need to use the RemoteViews generated by the MusicWidget, class to make sure we preserve the pending intents for the buttons.
            // Otherwise the widget's buttons can stop responding to touch events.
            val remoteViews = MusicWidget.getRemoteViews(this@MusicService)
            currFlipperState = if (song != null) {
                setMetadata(song, albumArt)
                remoteViews.setTextViewText(R.id.textViewTitle, song.title)
                remoteViews.setTextViewText(R.id.textViewArtist, song.artist)
                remoteViews.setTextViewText(R.id.textViewDuration, song.getDurationStr())
                if (loadedAlbumArt != null)
                    remoteViews.setImageViewBitmap(R.id.ivAlbumArt, loadedAlbumArt)
                else
                    remoteViews.setImageViewResource(R.id.ivAlbumArt, R.drawable.ic_launcher)

                if (currFlipperState == ViewFlipperState.STOPPED) {
                    remoteViews.setDisplayedChild(
                        R.id.viewFlipper,
                        ViewFlipperState.PLAYING.ordinal
                    )
                }
                ViewFlipperState.PLAYING
            } else {
                if (currFlipperState == ViewFlipperState.PLAYING) {
                    remoteViews.setDisplayedChild(
                        R.id.viewFlipper,
                        ViewFlipperState.STOPPED.ordinal
                    )
                }
                ViewFlipperState.STOPPED
            }
            val playPauseIconRes =
                if (isPlaying) R.drawable.ic_pause_white_36dp else R.drawable.ic_play_arrow_white_36dp
            remoteViews.setImageViewResource(R.id.button_play_pause, playPauseIconRes)

            val isShuffleOn = musicLoader.isShuffleOn
            val shuffleIconRes = if (isShuffleOn) R.drawable.shuffle_on else R.drawable.shuffle_off
            remoteViews.setImageViewResource(R.id.button_shuffle, shuffleIconRes)

            val thisWidget = ComponentName(this@MusicService, MusicWidget::class.java)
            val manager = AppWidgetManager.getInstance(this@MusicService)
            manager.updateAppWidget(thisWidget, remoteViews)

            // Create/Update a notification, to run the service in foreground
            if (song != null) {
                val notification = notificationBuilder
                    .build(song, albumArt, isPlaying, isShuffleOn, mediaSessionCompat.sessionToken)
                notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
            }
        }
        trackStateChanged(getIsPlaying())
    }

    private fun setMetadata(song: Song, albumArt: Bitmap) {
        mediaSessionCompat.setMetadata(
            MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .build()
        )
    }

    private fun pauseMusic() {
        Log.d(TAG, "PAUSE")
        if (player.isPlaying()) {
            val song = musicLoader.getCurrent()
            updateUI(song, false)
            player.pause()
            Log.d(TAG, "Music paused")
            setPlaybackState(
                player.getPosition(),
                PlaybackStateCompat.STATE_PAUSED
            )
        }
        trackStateChanged(false)
    }

    @Throws(IOException::class)
    private fun playMusic() {
        Log.d(TAG, "PLAY")
        val song = musicLoader.getCurrent()
        var position: Long = Config.getSongLastPosition(song.title).toLong()
        if (player.isPaused()) {
            player.play()
            position = player.getPosition()
        } else {
            player.setSong(song, true)
            player.seekTo(position.toInt())
            player.setPlaybackSpeed(Config.playbackSpeed)
        }
        Log.d("AAAA", "playMusic: $position")
        setPlaybackState(position * 1000, PlaybackStateCompat.STATE_PLAYING)
        updateUI(song, true)
        Log.i("Music Service", "Playing: " + song.title)
        trackStateChanged(true)
    }

    private fun stopMusic() {
        Log.d(TAG, "STOP MUSIC")
        isRunning = false
        setPlaybackState(
            player.getPosition(),
            PlaybackStateCompat.STATE_STOPPED
        )
        player.stop()
        updateUI(null, false)
        musicLoader.close()
        stopForeground(true)
        stopSelf()
    }

    @Throws(IOException::class)
    private fun nextSong(forcePlay: Boolean = false) {
        Log.d(TAG, "NEXT SONG")
        val willBePlaying = player.isPlaying() || forcePlay
        val nextSong = musicLoader.getNext()
        val songLastPosition = Config.getSongLastPosition(nextSong.title)
        Log.d("AAAA", "nextsong pos: $songLastPosition")
        player.setSong(nextSong, forcePlay)
        setPlaybackState(
            songLastPosition * 1000L,
            if (willBePlaying) PlaybackStateCompat.STATE_PLAYING
            else PlaybackStateCompat.STATE_PAUSED
        )
        player.seekTo(songLastPosition)
        updateUI(nextSong, willBePlaying)
        trackStateChanged(true)
        handleSongNext(nextSong)
    }

    @Throws(IOException::class)
    private fun previousSong() {
        Log.d(TAG, "PREVIOUS SONG")
        val wasPlaying = player.isPlaying()
        val prevSong = musicLoader.getPrevious()
        val songLastPosition = Config.getSongLastPosition(prevSong.title)
        Log.d("AAAA", "prev pos: $songLastPosition")
        player.setSong(prevSong, false)
        setPlaybackState(
            songLastPosition * 1000L,
            if (wasPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        )
        updateUI(prevSong, wasPlaying)
        trackStateChanged(true)
        handleSongPrevious(prevSong)
        player.seekTo(songLastPosition)
    }

    @Throws(IOException::class)
    private fun jumpTo(song: Song) {
        musicLoader.jumpTo(song)
        playMusic()
    }

    private fun handleProgressHandler(isPlaying: Boolean) {
        if (isPlaying) {
            mProgressHandler.post(object : Runnable {
                override fun run() {
                    if (player.isPlaying()) {
                        val secs = player.getPosition() / 1000
                        broadcastTrackProgress(secs.toInt())
                    }
                    mProgressHandler.removeCallbacksAndMessages(null)
                    mProgressHandler.postDelayed(
                        this,
                        (PROGRESS_UPDATE_INTERVAL / mPlaybackSpeed).toLong()
                    )
                }
            })
        } else {
            mProgressHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun broadcastTrackProgress(progress: Int) {
        EventBus.getDefault().post(Events.ProgressUpdated(progress, musicLoader.getCurrent()))
        updateMediaSessionState()
    }

    private fun trackStateChanged(isPlaying: Boolean) {
        handleProgressHandler(isPlaying)
        handleSongStateChanged(isPlaying)
    }

    private fun handleSongNext(nextSong: Song) {
        EventBus.getDefault().post(Events.NextSongChanged(nextSong))
    }

    private fun handleSongPrevious(prevSong: Song) {
        EventBus.getDefault().post(Events.NextSongChanged(prevSong))
    }

    private fun handleSongStateChanged(isPlaying: Boolean) {
        EventBus.getDefault().post(Events.SongStateChanged(isPlaying))
    }

    private fun startSleepTimer() {
        val millisInFuture = Config.sleepTime * 1000L
        mSleepTimer?.cancel()
        mSleepTimer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val milliSeconds = (millisUntilFinished)
                EventBus.getDefault().post(Events.SleepTimerChanged(milliSeconds))
            }

            override fun onFinish() {
                EventBus.getDefault().post(Events.SleepTimerChanged(0))
                val intent = Intent(this@MusicService, MusicService::class.java)
                intent.action = FINISH
                startService(intent)
            }
        }
        mSleepTimer?.start()
    }

    private fun stopSleepTimer() {
        mSleepTimer?.cancel()
    }

    private fun handleFinish() {
        broadcastTrackProgress(0)
        stopSelf()
    }

    private fun updateMediaSessionState() {
        val builder = PlaybackStateCompat.Builder()
        val playbackState = if (getIsPlaying()) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        builder.setState(playbackState, player.getPosition(), mPlaybackSpeed)
        mediaSessionCompat.setPlaybackState(builder.build())
    }

    private val channelId: String by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                "music_widget_service",
                "Music playback controls"
            )
        } else {
            "Default Channel"
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    internal enum class ViewFlipperState {
        STOPPED, PLAYING
    }

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot {
        return BrowserRoot(javaClass.name, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(mutableListOf())
    }

    override fun onMusicCompletion() {
        Config.setSongLastPosition(musicLoader.getCurrent().title, 0)
        nextSong(true)// will update the UI and play when ready (async)
    }

    companion object {
        @JvmField
        var isRunning = false
        private const val TAG = "Music Service"
        private const val ONGOING_NOTIFICATION_ID = 1
        private const val PLAYBACK_SPEED = 1.0f
        private const val PROGRESS_UPDATE_INTERVAL = 1000L
    }
}