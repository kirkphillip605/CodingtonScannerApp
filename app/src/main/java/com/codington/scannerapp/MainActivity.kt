package com.codington.scannerapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player

class MainActivity : AppCompatActivity() {

    private lateinit var playPauseButton: FrameLayout
    private lateinit var playPauseIcon: ImageView
    private lateinit var exitButton: FrameLayout
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var streamStatusText: TextView
    private lateinit var refreshButton: FrameLayout
    private lateinit var speakerButton: ImageButton
    private lateinit var tenCodesButton: LinearLayout
    private lateinit var liveDot: View
    private lateinit var liveIndicator: LinearLayout
    private lateinit var liveBadge: TextView

    private var audioService: AudioStreamService? = null
    private var serviceBound = false
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 1000)
        }
    }
    
    // Live dot blinking animation
    private val blinkRunnable = object : Runnable {
        override fun run() {
            if (audioService?.isPlaying() == true) {
                liveDot.visibility = if (liveDot.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
            } else {
                liveDot.visibility = View.VISIBLE
            }
            handler.postDelayed(this, 800)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioStreamService.LocalBinder
            audioService = binder.getService()
            serviceBound = true
            
            audioService?.setPlayerListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    runOnUiThread {
                        updatePlaybackState(playbackState)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    runOnUiThread {
                        updatePlayPauseButton(isPlaying)
                    }
                }
            })
            
            // Auto-play on launch
            audioService?.play()
            
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        
        // Make status bar icons light (for dark background)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        
        setContentView(R.layout.activity_main)

        // Initialize views
        playPauseButton = findViewById(R.id.playPauseButton)
        playPauseIcon = findViewById(R.id.playPauseIcon)
        exitButton = findViewById(R.id.exitButton)
        seekBar = findViewById(R.id.seekBar)
        currentTimeText = findViewById(R.id.currentTime)
        streamStatusText = findViewById(R.id.streamStatusText)
        refreshButton = findViewById(R.id.refreshButton)
        speakerButton = findViewById(R.id.speakerButton)
        tenCodesButton = findViewById(R.id.tenCodesButton)
        liveDot = findViewById(R.id.liveDot)
        liveIndicator = findViewById(R.id.liveIndicator)
        liveBadge = findViewById(R.id.liveBadge)

        // Set up click listeners
        playPauseButton.setOnClickListener {
            audioService?.let { service ->
                if (service.isPlaying()) {
                    service.pause()
                } else {
                    service.play()
                }
            }
        }

        exitButton.setOnClickListener {
            exitApp()
        }
        
        refreshButton.setOnClickListener {
            // Restart the stream
            audioService?.restart()
        }
        
        speakerButton.setOnClickListener {
            // Volume control could be added here
        }
        
        tenCodesButton.setOnClickListener {
            // 10-Codes functionality could be added here
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioService?.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Start and bind to service
        val intent = Intent(this, AudioStreamService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()
        handler.post(updateRunnable)
        handler.post(blinkRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(blinkRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun updateUI() {
        audioService?.let { service ->
            val currentPosition = service.getCurrentPosition()
            val duration = service.getDuration()

            currentTimeText.text = formatTime(currentPosition)
            
            if (duration > 0) {
                seekBar.max = duration.toInt()
                seekBar.progress = currentPosition.toInt()
                seekBar.isEnabled = true
            } else {
                seekBar.isEnabled = false
            }

            updatePlayPauseButton(service.isPlaying())
        }
    }

    private fun updatePlaybackState(state: Int) {
        when (state) {
            Player.STATE_IDLE -> streamStatusText.text = "Idle"
            Player.STATE_BUFFERING -> streamStatusText.text = "Buffering..."
            Player.STATE_READY -> streamStatusText.text = "Ready"
            Player.STATE_ENDED -> streamStatusText.text = "Ended"
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        playPauseIcon.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun exitApp() {
        // Stop the service (this will handle cleanup)
        audioService?.stop()
        
        // Unbind from service
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        
        // Finish the activity and exit the app completely
        finish()
        finishAffinity()
    }
}
