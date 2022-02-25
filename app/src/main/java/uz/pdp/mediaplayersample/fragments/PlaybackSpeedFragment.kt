package uz.pdp.mediaplayersample.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.pdp.mediaplayersample.databinding.FragmentPlaybackSpeedBinding
import uz.pdp.mediaplayersample.helpers.onSeekBarChangeListener
import uz.pdp.mediaplayersample.musicplayer.PlaybackSpeedListener
import uz.pdp.mediaplayersample.storage.Config
import kotlin.math.roundToInt

class PlaybackSpeedFragment : BottomSheetDialogFragment() {
    private val MIN_PLAYBACK_SPEED = 0.25f
    private val MAX_PLAYBACK_SPEED = 3f
    private val MAX_PROGRESS = (MAX_PLAYBACK_SPEED * 100 + MIN_PLAYBACK_SPEED * 100).toInt()
    private val HALF_PROGRESS = MAX_PROGRESS / 2
    private val STEP = 0.05f

    private var seekBar: SeekBar? = null
    private var listener: PlaybackSpeedListener? = null

    lateinit var binding: FragmentPlaybackSpeedBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaybackSpeedBinding.inflate(layoutInflater)

        view.apply {
            seekBar = binding.playbackSpeedSeekbar
            binding.playbackSpeedSlow.setOnClickListener { reduceSpeed() }
            binding.playbackSpeedFast.setOnClickListener { increaseSpeed() }
            initSeekbar(binding.playbackSpeedSeekbar, binding.playbackSpeedLabel)
        }

        return binding.root
    }

    private fun initSeekbar(seekbar: SeekBar, speedLabel: TextView) {
        val formattedValue = formatPlaybackSpeed(Config.playbackSpeed)
        speedLabel.text = "${formattedValue}x"
        seekbar.max = MAX_PROGRESS

        val playbackSpeedProgress = Config.playbackSpeedProgress
        if (playbackSpeedProgress == -1) {
            Config.playbackSpeedProgress = HALF_PROGRESS
        }
        seekbar.progress = Config.playbackSpeedProgress

        var lastUpdatedProgress = Config.playbackSpeedProgress
        var lastUpdatedFormattedValue = formattedValue

        seekbar.onSeekBarChangeListener { progress ->
            val playbackSpeed = getPlaybackSpeed(progress)
            if (playbackSpeed.toString() != lastUpdatedFormattedValue) {
                lastUpdatedProgress = progress
                lastUpdatedFormattedValue = playbackSpeed.toString()
                Config.playbackSpeed = playbackSpeed
                Config.playbackSpeedProgress = progress

                speedLabel.text = "${formatPlaybackSpeed(playbackSpeed)}x"
                listener?.updatePlaybackSpeed(playbackSpeed)
            } else {
                seekbar.progress = lastUpdatedProgress
            }
        }
    }

    private fun getPlaybackSpeed(progress: Int): Float {
        var playbackSpeed = when {
            progress < HALF_PROGRESS -> {
                val lowerProgressPercent = progress / HALF_PROGRESS.toFloat()
                val lowerProgress =
                    (1 - MIN_PLAYBACK_SPEED) * lowerProgressPercent + MIN_PLAYBACK_SPEED
                lowerProgress
            }
            progress > HALF_PROGRESS -> {
                val upperProgressPercent = progress / HALF_PROGRESS.toFloat() - 1
                val upperDiff = MAX_PLAYBACK_SPEED - 1
                upperDiff * upperProgressPercent + 1
            }
            else -> 1f
        }
        playbackSpeed =
            playbackSpeed.coerceAtLeast(MIN_PLAYBACK_SPEED).coerceAtMost(MAX_PLAYBACK_SPEED)
        val stepMultiplier = 1 / STEP
        return (playbackSpeed * stepMultiplier).roundToInt() / stepMultiplier
    }

    private fun reduceSpeed() {
        var currentProgress = seekBar?.progress ?: return
        val currentSpeed = Config.playbackSpeed
        while (currentProgress > 0) {
            val newSpeed = getPlaybackSpeed(--currentProgress)
            if (newSpeed != currentSpeed) {
                seekBar!!.progress = currentProgress
                break
            }
        }
    }

    private fun increaseSpeed() {
        var currentProgress = seekBar?.progress ?: return
        val currentSpeed = Config.playbackSpeed
        while (currentProgress < MAX_PROGRESS) {
            val newSpeed = getPlaybackSpeed(++currentProgress)
            if (newSpeed != currentSpeed) {
                seekBar!!.progress = currentProgress
                break
            }
        }
    }

    private fun formatPlaybackSpeed(value: Float) = String.format("%.2f", value)

    fun setListener(playbackSpeedListener: PlaybackSpeedListener) {
        listener = playbackSpeedListener
    }
}
