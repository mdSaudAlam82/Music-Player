package com.example.musicplayer.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class MusicPlayerService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // 1. CPU aur Network ko zinda rakhne ke liye WakeMode
        player.setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)

        // 2. Notification par click karne se app khulne ka rasta (PendingIntent)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. MediaSession build karna (PendingIntent ke sath)
        mediaSession = MediaSession.Builder(this, player)
            .setId("MusicPlayerServiceSession")
            .setSessionActivity(pendingIntent) // Notification block hone se bachayega
            .build()

        // 4. Notification Provider (Icon set karna)
        val notificationProvider = DefaultMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(R.drawable.ic_launcher_foreground)
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // 👇 THE IMMORTAL HACK 👇
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Ise poora khali rakha hai.
        // Ab minimize screen se app hatane par bhi gaana aur notification chalte rahenge!
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}