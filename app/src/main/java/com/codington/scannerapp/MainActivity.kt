package com.codington.scannerapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.Player

class MainActivity : AppCompatActivity() {

    private lateinit var playPauseButton: Button
    private lateinit var exitButton: Button
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var streamStatusText: TextView

    private var audioService: AudioStreamService? = null
    private var serviceBound = false
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 1000)
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
            
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        playPauseButton = findViewById(R.id.playPauseButton)
        exitButton = findViewById(R.id.exitButton)
        seekBar = findViewById(R.id.seekBar)
        currentTimeText = findViewById(R.id.currentTime)
        totalTimeText = findViewById(R.id.totalTime)
        streamStatusText = findViewById(R.id.streamStatusText)

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
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(updateRunnable)
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
                totalTimeText.text = formatTime(duration)
                seekBar.max = duration.toInt()
                seekBar.progress = currentPosition.toInt()
                seekBar.isEnabled = true
            } else {
                totalTimeText.text = "--:--"
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
        playPauseButton.text = if (isPlaying) getString(R.string.pause) else getString(R.string.play)
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun exitApp() {
        // Stop the service
        audioService?.stop()
        
        // Stop the service completely
        val intent = Intent(this, AudioStreamService::class.java)
        stopService(intent)
        
        // Unbind from service
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        
        // Finish the activity
        finish()
        
        // Exit the app completely
        finishAffinity()
    }
}
