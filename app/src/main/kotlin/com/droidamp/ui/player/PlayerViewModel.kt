package com.droidamp.ui.player

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.droidamp.domain.model.PlayerState
import com.droidamp.domain.model.RepeatMode
import com.droidamp.domain.model.Track
import com.droidamp.service.DroidampPlaybackService
import com.droidamp.ui.visualizer.VisualizerMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

data class EqBand(
    val index: Int,
    val centerFreqHz: Int,  // Hz (already divided from millihertz)
    val level: Short,       // millibels
    val minLevel: Short,
    val maxLevel: Short,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // ── Player state ──────────────────────────────────────────
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // ── Visualizer ────────────────────────────────────────────
    private val _fftData = MutableStateFlow(FloatArray(20) { 0f })
    val fftData: StateFlow<FloatArray> = _fftData.asStateFlow()

    private val _vizMode = MutableStateFlow(VisualizerMode.DEFAULT)
    val vizMode: StateFlow<VisualizerMode> = _vizMode.asStateFlow()

    private val _isVizFullScreen = MutableStateFlow(false)
    val isVizFullScreen: StateFlow<Boolean> = _isVizFullScreen.asStateFlow()

    // ── EQ ────────────────────────────────────────────────────
    private val _eqBands = MutableStateFlow<List<EqBand>>(emptyList())
    val eqBands: StateFlow<List<EqBand>> = _eqBands.asStateFlow()

    // ── Favorites (in-memory) ─────────────────────────────────
    private val _starredIds = MutableStateFlow<Set<String>>(emptySet())
    val starredIds: StateFlow<Set<String>> = _starredIds.asStateFlow()

    // ── MediaController / effects ─────────────────────────────
    private var controller: MediaController? = null
    private var visualizer: Visualizer? = null
    private var equalizer: Equalizer? = null

    init {
        connectToService()
        startPositionPolling()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, DroidampPlaybackService::class.java)
        )
        viewModelScope.launch {
            val future = MediaController.Builder(context, sessionToken).buildAsync()
            controller = future.await()
            controller?.addListener(playerListener)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) attachAudioEffects() else detachAudioEffects()
        }
        override fun onMediaMetadataChanged(metadata: MediaMetadata) {}
        override fun onRepeatModeChanged(repeatMode: Int) {
            _playerState.update {
                it.copy(repeatMode = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                })
            }
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playerState.update { it.copy(isShuffled = shuffleModeEnabled) }
        }
    }

    // ── Position polling ──────────────────────────────────────
    private fun startPositionPolling() {
        viewModelScope.launch {
            while (true) {
                delay(500)
                controller?.let { c ->
                    _playerState.update { it.copy(
                        positionMs = c.currentPosition,
                        durationMs = c.duration.coerceAtLeast(0L),
                    ) }
                }
            }
        }
    }

    // ── Public controls ───────────────────────────────────────

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.streamUrl)
                .setMediaId(track.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
        }
        controller?.run {
            setMediaItems(items, startIndex, 0L)
            prepare()
            play()
        }
        _playerState.update { it.copy(queue = tracks, queueIndex = startIndex, currentTrack = tracks.getOrNull(startIndex)) }
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun prev() { controller?.seekToPreviousMediaItem() }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    fun seekToQueueItem(index: Int) {
        controller?.seekTo(index, 0L)
        _playerState.update { it.copy(
            queueIndex = index,
            currentTrack = it.queue.getOrNull(index),
        ) }
    }

    fun setVolume(volume: Float) {
        controller?.volume = volume
        _playerState.update { it.copy(volume = volume) }
    }

    fun toggleRepeat() {
        val next = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playerState.update { it.copy(repeatMode = next) }
        controller?.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_playerState.value.isShuffled
        controller?.shuffleModeEnabled = newShuffle
    }

    fun toggleStar(trackId: String) {
        _starredIds.update { if (it.contains(trackId)) it - trackId else it + trackId }
    }

    // ── Visualizer mode ───────────────────────────────────────

    fun nextVizMode() { _vizMode.update { it.next() } }
    fun prevVizMode() { _vizMode.update { it.prev() } }
    fun setVizMode(mode: VisualizerMode) { _vizMode.value = mode }
    fun toggleVizFullScreen() { _isVizFullScreen.update { !it } }

    // ── EQ ────────────────────────────────────────────────────

    fun setEqBand(bandIndex: Int, level: Short) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), level)
            _eqBands.update { bands ->
                bands.map { if (it.index == bandIndex) it.copy(level = level) else it }
            }
        } catch (_: Exception) {}
    }

    // ── Audio effects ─────────────────────────────────────────

    private fun attachAudioEffects() {
        attachVisualizer()
        attachEqualizer()
    }

    private fun detachAudioEffects() {
        detachVisualizer()
        detachEqualizer()
    }

    private fun attachVisualizer() {
        detachVisualizer()
        val audioSessionId = DroidampPlaybackService.audioSessionId.takeIf { it != 0 } ?: return
        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer, waveform: ByteArray, sampleRate: Int) {}
                    override fun onFftDataCapture(v: Visualizer, fft: ByteArray, sampleRate: Int) {
                        processFft(fft)
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (_: Exception) {}
    }

    private fun detachVisualizer() {
        visualizer?.run { enabled = false; release() }
        visualizer = null
        _fftData.value = FloatArray(20) { 0f }
    }

    private fun attachEqualizer() {
        detachEqualizer()
        val audioSessionId = DroidampPlaybackService.audioSessionId.takeIf { it != 0 } ?: return
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
                val range = bandLevelRange
                val bands = (0 until numberOfBands.toInt()).map { b ->
                    EqBand(
                        index        = b,
                        centerFreqHz = getCenterFreq(b.toShort()) / 1000,
                        level        = getBandLevel(b.toShort()),
                        minLevel     = range[0],
                        maxLevel     = range[1],
                    )
                }
                _eqBands.value = bands
            }
        } catch (_: Exception) {}
    }

    private fun detachEqualizer() {
        equalizer?.run { enabled = false; release() }
        equalizer = null
    }

    private fun processFft(fft: ByteArray) {
        val bands = 20
        // Drop fft[0] (DC) and fft[1] (Nyquist) — only audio frequency data from index 2 onward
        val usableFft  = fft.drop(2).toByteArray()
        val bucketSize = (usableFft.size / bands).coerceAtLeast(1)
        val result = FloatArray(bands) { b ->
            val start = b * bucketSize
            val end   = (start + bucketSize).coerceAtMost(usableFft.size)
            val rms   = sqrt(usableFft.slice(start until end).map { (it.toFloat() / 128f) * (it.toFloat() / 128f) }.average().toFloat())
            (rms * 5f).coerceIn(0f, 1f)
        }
        val prev = _fftData.value
        _fftData.value = FloatArray(bands) { i -> prev[i] * 0.3f + result[i] * 0.7f }
    }

    override fun onCleared() {
        detachAudioEffects()
        controller?.run { removeListener(playerListener); release() }
        super.onCleared()
    }
}
