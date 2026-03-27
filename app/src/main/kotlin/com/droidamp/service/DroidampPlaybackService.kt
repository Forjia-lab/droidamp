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
import kotlinx.coroutines.flow.MutableStateFlow

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
        /**
         * Observable audio session ID — updated when ExoPlayer creates/recreates its AudioTrack.
         * The ViewModel collects this to know when it's safe to attach a Visualizer.
         * Value is 0 until ExoPlayer's first AudioTrack is created.
         */
        val audioSessionId = MutableStateFlow(0)
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

        // Seed immediately — ExoPlayer pre-allocates a session ID during build().
        // onAudioSessionIdChanged only fires when the ID *changes*, so if ExoPlayer reuses
        // the pre-allocated ID for its AudioTrack (common case), we'd never get an update
        // without this initial seed.
        audioSessionId.value = player.audioSessionId
        Log.d("VizDebug", "Service onCreate: initial audioSessionId=${player.audioSessionId}")

        // Also update whenever ExoPlayer recreates its AudioTrack (e.g. after an error)
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int,
            ) {
                Log.d("VizDebug", "onAudioSessionIdChanged → $audioSessionId")
                DroidampPlaybackService.audioSessionId.value = audioSessionId
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
