package com.codington.scannerapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class AudioStreamService : Service() {

    private val binder = LocalBinder()
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private val streamUrl = "https://uranus.kevys.net:8034/scanner"
    
    companion object {
        private const val CHANNEL_ID = "AudioStreamChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.codington.scannerapp.ACTION_PLAY"
        const val ACTION_PAUSE = "com.codington.scannerapp.ACTION_PAUSE"
        const val ACTION_STOP = "com.codington.scannerapp.ACTION_STOP"
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioStreamService = this@AudioStreamService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeMediaSession()
        initializePlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_STOP -> stop()
        }
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "CodintonScannerSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    play()
                }
                
                override fun onPause() {
                    pause()
                }
                
                override fun onStop() {
                    stop()
                }
            })
            isActive = true
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            val mediaItem = MediaItem.fromUri(streamUrl)
            setMediaItem(mediaItem)
            prepare()
            
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateNotification()
                }
            })
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Stream",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audio streaming service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Play/Pause action
        val isPlaying = player?.isPlaying ?: false
        val playPauseAction = if (isPlaying) {
            val pauseIntent = Intent(this, AudioStreamService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            NotificationCompat.Action.Builder(
                R.drawable.ic_pause,
                "Pause",
                pausePendingIntent
            ).build()
        } else {
            val playIntent = Intent(this, AudioStreamService::class.java).apply {
                action = ACTION_PLAY
            }
            val playPendingIntent = PendingIntent.getService(
                this, 1, playIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            NotificationCompat.Action.Builder(
                R.drawable.ic_play,
                "Play",
                playPendingIntent
            ).build()
        }
        
        // Stop action
        val stopIntent = Intent(this, AudioStreamService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopAction = NotificationCompat.Action.Builder(
            R.drawable.ic_exit,
            "Stop",
            stopPendingIntent
        ).build()

        val statusText = if (isPlaying) "Live • Streaming" else "Paused"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Codington County Police Scanner")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_scanner)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession?.sessionToken)
                .setShowActionsInCompactView(0, 1))
            .build()
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    fun play() {
        player?.play()
        updateNotification()
    }

    fun pause() {
        player?.pause()
        updateNotification()
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
        mediaSession?.release()
        mediaSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    fun restart() {
        player?.let { p ->
            p.stop()
            val mediaItem = MediaItem.fromUri(streamUrl)
            p.setMediaItem(mediaItem)
            p.prepare()
            p.play()
        }
    }

    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0
    }

    fun getDuration(): Long {
        return player?.duration ?: 0
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun setPlayerListener(listener: Player.Listener) {
        player?.addListener(listener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        mediaSession = null
        if (player != null) {
            player?.release()
            player = null
        }
    }
}
