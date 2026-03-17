package com.droidamp.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.droidamp.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

// ─────────────────────────────────────────────────────────────
//  DroidampPlaybackService
//
//  Runs as a foreground service — keeps playback alive when
//  the app is backgrounded, screen is off, or user swipes away.
//  Provides the MediaSession that the system notification
//  controls and Android Auto will connect to.
// ─────────────────────────────────────────────────────────────

@AndroidEntryPoint
class DroidampPlaybackService : MediaSessionService() {

    companion object {
        /** Updated by AnalyticsListener once ExoPlayer assigns its AudioTrack session. */
        @Volatile var audioSessionId: Int = 0
            private set
    }

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)   // pause on headphone unplug
            .build()

        // Seed the session ID immediately (ExoPlayer may pre-allocate it during build)
        audioSessionId = player.audioSessionId
        Log.d("Droidamp", "ExoPlayer initial audioSessionId = ${player.audioSessionId}")

        // Update whenever ExoPlayer (re-)creates its AudioTrack
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int,
            ) {
                DroidampPlaybackService.audioSessionId = audioSessionId
                Log.d("Droidamp", "ExoPlayer audioSessionId → $audioSessionId")
            }
        })

        val sessionActivityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    /**
     * Called when the user swipes the app away from the Recents screen.
     * Stop the service (and dismiss the notification) only when nothing is playing.
     * If music is actively playing, leave the foreground service running so playback continues.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
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
