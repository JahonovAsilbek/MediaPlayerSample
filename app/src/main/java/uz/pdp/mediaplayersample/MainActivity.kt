package uz.pdp.mediaplayersample

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pub.devrel.easypermissions.EasyPermissions
import uz.pdp.mediaplayersample.adapters.SongCursorRecyclerAdapter
import uz.pdp.mediaplayersample.constants.*
import uz.pdp.mediaplayersample.databinding.ActivityMainBinding
import uz.pdp.mediaplayersample.databinding.DialogSleepBinding
import uz.pdp.mediaplayersample.fragments.PlaybackSpeedFragment
import uz.pdp.mediaplayersample.helpers.Events
import uz.pdp.mediaplayersample.load.MusicLoader
import uz.pdp.mediaplayersample.load.SongListLoader
import uz.pdp.mediaplayersample.models.Song
import uz.pdp.mediaplayersample.musicplayer.PlaybackSpeedListener
import uz.pdp.mediaplayersample.service.MusicService
import uz.pdp.mediaplayersample.storage.Config
import uz.pdp.mediaplayersample.viewmodel.SongListVM
import uz.pdp.mediaplayersample.viewmodel.ViewModelFactory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), PlaybackSpeedListener {

    private lateinit var adapter: SongCursorRecyclerAdapter
    private var viewModel: SongListVM? = null
    private var songList: ArrayList<Song>? = null
    lateinit var serviceIntent: Intent
    private var bus: EventBus? = null

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Config.init(this)
        setupVM()
        bus = EventBus.getDefault()
        bus!!.register(this)

        songClick()

        binding.rv.also {
            val dividerItemDecoration = DividerItemDecoration(it.context, LinearLayout.VERTICAL)
            it.addItemDecoration(dividerItemDecoration)
            it.adapter = adapter
        }

        viewModel!!.cursorLD.observe(this) {
            adapter.changeCursor(it)
        }
        viewModel!!.currentPosLD.observe(this, Observer(binding.rv::scrollToPosition))
        viewModel!!.getCursor()

        //playbackspeed
        binding.playbackSpeed.text = "${DecimalFormat("#.##").format(Config.playbackSpeed)}x"
    }

    private fun songClick() {
        adapter = SongCursorRecyclerAdapter(
            null,
            this,
            object : SongCursorRecyclerAdapter.SongClickListener {
                override fun onSongSelected(song: Song) {
                    updateUI(song)
                    serviceIntent = Intent(this@MainActivity, MusicService::class.java)
                    serviceIntent.action = ACTION_JUMP_TO
                    serviceIntent.putExtra("song", song)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    btnClick()
                }
            })
    }

    private fun btnClick() {
        binding.next.setOnClickListener {
            serviceIntent.action = ACTION_NEXT
            startService(serviceIntent)
        }
        binding.play.setOnClickListener {
            serviceIntent.action = ACTION_PLAY_PAUSE
            startService(serviceIntent)
        }
        binding.prev.setOnClickListener {
            serviceIntent.action = ACTION_PREVIOUS
            startService(serviceIntent)
        }

        binding.playbackSpeed.setOnClickListener {
            showPlaybackSpeedPicker()
        }

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Intent(this@MainActivity, MusicService::class.java).apply {
                    putExtra(PROGRESS, seekBar?.progress)
                    action = ACTION_SEEK_TO
                    startService(this)
                    Log.d("AAAA", "onStopTrackingTouch: ${seekBar?.progress}")
                }
            }

        })

        binding.sleepTimer.setOnClickListener {
            showSleepDialog()
        }
    }

    private fun showSleepDialog() {
        val dialog = AlertDialog.Builder(this)
        val alert = dialog.create()
        val view = DialogSleepBinding.inflate(layoutInflater)
        view.btn.setOnClickListener {
            val sleepTime = view.et.text.toString().trim()
            // sned intent to service
            if (sleepTime.isNotEmpty()) {
                Config.sleepTime = sleepTime.toInt()
                Intent(this, MusicService::class.java).apply {
                    this.action = START_SLEEP_TIMER
                    try {
                        if (isOreoPlus()) {
                            startForegroundService(this)
                        } else {
                            startService(this)
                        }
                    } catch (ignored: Exception) {
                    }
                }
            } else {
                Intent(this, MusicService::class.java).apply {
                    this.action = STOP_SLEEP_TIMER
                    try {
                        if (isOreoPlus()) {
                            startForegroundService(this)
                        } else {
                            startService(this)
                        }
                    } catch (ignored: Exception) {
                    }
                }
            }
            alert.cancel()
        }
        alert.setView(view.root)
        alert.show()
    }

    private fun showPlaybackSpeedPicker() {
        val fragment = PlaybackSpeedFragment()
        fragment.show(supportFragmentManager, PlaybackSpeedFragment::class.java.simpleName)
        fragment.setListener(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun progressUpdated(event: Events.ProgressUpdated) {
        binding.seekbar.progress = event.progress * 1000
        val date = Date(event.progress * 1000L)
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        val format = simpleDateFormat.format(date)
        binding.start.text = format

        // save song last state
        Config.setSongLastPosition(event.current.title, event.progress)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updateNextSong(event: Events.NextSongChanged) {
        event.track?.let { updateUI(it) }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updatePlayPause(event: Events.SongStateChanged) {
        binding.play.text = if (event.isPlaying) "Pause" else "Play"
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updateSleepTime(event: Events.SleepTimerChanged) {
        val date = Date(event.milliSeconds)
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        val format = simpleDateFormat.format(date)
        if (event.milliSeconds != 0L)
            binding.sleepTimer.text = format
        else binding.sleepTimer.text = "sleep"
    }

    private fun updateUI(song: Song) {
        binding.image.setImageURI(Uri.parse(song.albumArtPath))
        binding.seekbar.max = song.duration.toInt()
        binding.title.text = song.title
        binding.author.text = song.artist

        val date = Date(song.duration)
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        val format = simpleDateFormat.format(date)
        binding.end.text = format
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    private fun setupVM() {
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                SongListLoader(this),
                MusicLoader(this)
            )
        )[SongListVM::class.java]
    }

    override fun updatePlaybackSpeed(speed: Float) {
        val isSlow = speed < 1f
        if (isSlow != binding.playbackSpeed.tag as? Boolean) {
            binding.playbackSpeed.tag = isSlow
        }

        binding.playbackSpeed.text = "${DecimalFormat("#.##").format(speed)}x"
        Intent(this, MusicService::class.java).apply {
            this.action = SET_PLAYBACK_SPEED
            try {
                if (isOreoPlus()) {
                    startForegroundService(this)
                } else {
                    startService(this)
                }
            } catch (ignored: Exception) {
            }
        }
    }
}
