package com.droidamp.ui.player

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

enum class RgMode { OFF, TRACK, ALBUM }

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
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    companion object {
        private const val TAG = "ReplayGain"
        private val KEY_EQ_PRESET       = stringPreferencesKey("eq_preset")
        private val KEY_RG_MODE         = stringPreferencesKey("rg_mode")
        private val KEY_RG_PREAMP       = floatPreferencesKey("rg_preamp")
        private val KEY_RG_PREVENT_CLIP = booleanPreferencesKey("rg_prevent_clipping")

        // dB values; applied as dB * 100 = millibels
        val EQ_PRESETS: Map<String, IntArray> = linkedMapOf(
            "Flat"       to intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0,  0,  0),
            "Bass Boost" to intArrayOf( 6,  5,  4,  2,  0,  0,  0,  0,  0,  0),
            "Treble"     to intArrayOf( 0,  0,  0,  0,  0,  2,  3,  4,  5,  6),
            "Rock"       to intArrayOf( 4,  3,  2,  0, -1,  0,  2,  3,  4,  4),
            "Electronic" to intArrayOf( 5,  4,  0, -2, -3,  0,  3,  4,  5,  5),
            "Classical"  to intArrayOf( 3,  2,  0,  0,  0,  0,  0,  2,  3,  3),
            "Hip Hop"    to intArrayOf( 5,  4,  2,  3, -1,  0,  1,  2,  3,  4),
            "Jazz"       to intArrayOf( 2,  1,  0,  2, -1, -1,  0,  1,  2,  3),
            "Pop"        to intArrayOf(-1,  0,  2,  3,  3,  2,  0, -1, -1, -1),
            "Dance"      to intArrayOf( 4,  3,  1,  0, -1,  2,  3,  4,  3,  2),
        )
    }

    // ── Player state ──────────────────────────────────────────
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // ── Visualizer ────────────────────────────────────────────
    private val _fftData = MutableStateFlow(FloatArray(20) { 0f })
    val fftData: StateFlow<FloatArray> = _fftData.asStateFlow()

    private val _waveformData = MutableStateFlow(FloatArray(128) { 0f })
    val waveformData: StateFlow<FloatArray> = _waveformData.asStateFlow()

    private val _vizMode = MutableStateFlow(VisualizerMode.DEFAULT)
    val vizMode: StateFlow<VisualizerMode> = _vizMode.asStateFlow()

    private val _isVizFullScreen = MutableStateFlow(false)
    val isVizFullScreen: StateFlow<Boolean> = _isVizFullScreen.asStateFlow()

    // ── EQ ────────────────────────────────────────────────────
    private val _eqBands = MutableStateFlow<List<EqBand>>(emptyList())
    val eqBands: StateFlow<List<EqBand>> = _eqBands.asStateFlow()

    private val _activePreset = MutableStateFlow("Flat")
    val activePreset: StateFlow<String> = _activePreset.asStateFlow()

    // ── ReplayGain settings ───────────────────────────────────
    private val _rgMode           = MutableStateFlow(RgMode.OFF)
    val rgMode: StateFlow<RgMode> = _rgMode.asStateFlow()

    private val _rgPreamp           = MutableStateFlow(0f)
    val rgPreamp: StateFlow<Float>  = _rgPreamp.asStateFlow()

    private val _rgPreventClipping          = MutableStateFlow(true)
    val rgPreventClipping: StateFlow<Boolean> = _rgPreventClipping.asStateFlow()

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
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            _activePreset.value = prefs[KEY_EQ_PRESET] ?: "Flat"
            _rgMode.value = prefs[KEY_RG_MODE]
                ?.let { runCatching { RgMode.valueOf(it) }.getOrDefault(RgMode.OFF) }
                ?: RgMode.OFF
            _rgPreamp.value = prefs[KEY_RG_PREAMP] ?: 0f
            _rgPreventClipping.value = prefs[KEY_RG_PREVENT_CLIP] ?: true
        }
        // Re-apply gain whenever any RG setting changes
        viewModelScope.launch {
            combine(_rgMode, _rgPreamp, _rgPreventClipping) { mode, preamp, clip ->
                Triple(mode, preamp, clip)
            }.collect { (mode, preamp, clip) ->
                Log.d(TAG, "settings changed → mode=$mode preamp=$preamp preventClip=$clip controller=${controller != null}")
                applyReplayGain()
            }
        }
        // React to audio session ID changes from the service.
        // The flow is seeded immediately from player.audioSessionId in Service.onCreate(), so we
        // get a non-zero value as soon as the service starts — no waiting for onAudioSessionIdChanged.
        viewModelScope.launch {
            DroidampPlaybackService.audioSessionId.collect { sessionId ->
                Log.d("VizDebug", "audioSessionId flow emitted → $sessionId (isPlaying=${_playerState.value.isPlaying})")
                if (sessionId != 0 && _playerState.value.isPlaying) {
                    attachVisualizer()
                    attachEqualizer()
                }
            }
        }
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, DroidampPlaybackService::class.java)
        )
        viewModelScope.launch {
            val future = MediaController.Builder(context, sessionToken).buildAsync()
            controller = future.await()
            Log.d(TAG, "connectToService: controller ready, isPlaying=${controller?.isPlaying}, volume=${controller?.volume}")
            controller?.addListener(playerListener)
            // Apply any RG settings that were loaded from DataStore before controller was ready
            applyReplayGain()
            // If the player is already playing when we connect (e.g. after screen rotation),
            // onIsPlayingChanged won't fire — attach effects immediately if session ID is ready.
            val c = controller ?: return@launch
            if (c.isPlaying) {
                _playerState.update { it.copy(isPlaying = true) }
                val sessionId = DroidampPlaybackService.audioSessionId.value
                if (sessionId != 0) {
                    attachAudioEffects()
                }
                // If session ID is 0 here, the audioSessionId flow collector (in init) will
                // attach when the service emits the real ID.
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d("VizDebug", "onIsPlayingChanged → $isPlaying (sessionId=${DroidampPlaybackService.audioSessionId.value})")
            _playerState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) attachAudioEffects() else detachAudioEffects()
        }
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            // Keep UI in sync when next/prev is triggered from the notification or lock screen
            val idx = controller?.currentMediaItemIndex ?: return
            _playerState.update { it.copy(
                queueIndex   = idx,
                currentTrack = it.queue.getOrNull(idx),
            ) }
            applyReplayGain()
        }
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
                        .setArtworkUri(track.coverArtId?.let { Uri.parse(it) })
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
        applyReplayGain()
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

    // ── ReplayGain controls ───────────────────────────────────

    fun setRgMode(mode: RgMode) {
        _rgMode.value = mode
        viewModelScope.launch { dataStore.edit { it[KEY_RG_MODE] = mode.name } }
    }

    fun setRgPreamp(db: Float) {
        _rgPreamp.value = db
        viewModelScope.launch { dataStore.edit { it[KEY_RG_PREAMP] = db } }
    }

    fun setRgPreventClipping(enabled: Boolean) {
        _rgPreventClipping.value = enabled
        viewModelScope.launch { dataStore.edit { it[KEY_RG_PREVENT_CLIP] = enabled } }
    }

    private fun applyReplayGain() {
        val mode   = _rgMode.value
        val preamp = _rgPreamp.value
        val track  = _playerState.value.currentTrack
        val c      = controller

        if (c == null) {
            Log.d(TAG, "applyReplayGain: controller not ready yet, skipping")
            return
        }

        if (mode == RgMode.OFF) {
            Log.d(TAG, "applyReplayGain: mode=OFF → volume=1.0")
            c.volume = 1f
            return
        }

        val rg = track?.replayGain
        val gainDb = when (mode) {
            RgMode.TRACK -> rg?.trackGain
            RgMode.ALBUM -> rg?.albumGain ?: rg?.trackGain
            RgMode.OFF   -> null
        }

        // No track-level gain available — apply preamp as a standalone volume offset
        if (gainDb == null) {
            val linearGain = 10f.pow(preamp / 20f).coerceIn(0f, 1f)
            Log.d(TAG, "applyReplayGain: mode=$mode no RG data → preamp-only linearGain=$linearGain")
            c.volume = linearGain
            return
        }

        val totalDb    = gainDb + preamp
        var linearGain = 10f.pow(totalDb / 20f)
        Log.d(TAG, "applyReplayGain: mode=$mode gainDb=$gainDb preamp=$preamp totalDb=$totalDb linearGain(pre-clip)=$linearGain")

        if (_rgPreventClipping.value) {
            val peak = when (mode) {
                RgMode.TRACK -> rg?.trackPeak
                RgMode.ALBUM -> rg?.albumPeak ?: rg?.trackPeak
                RgMode.OFF   -> null
            }
            if (peak != null && peak > 0f && peak * linearGain > 1f) {
                val clipped = 1f / peak
                Log.d(TAG, "applyReplayGain: clipping prevention: peak=$peak → clamping $linearGain → $clipped")
                linearGain = clipped
            }
        }

        val finalGain = linearGain.coerceIn(0f, 1f)
        Log.d(TAG, "applyReplayGain: setting controller.volume=$finalGain (track='${track?.title}')")
        c.volume = finalGain
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

    fun applyPreset(name: String) {
        val presetDb = EQ_PRESETS[name] ?: return
        val bands    = _eqBands.value
        if (bands.isEmpty()) return
        bands.forEach { band ->
            val displayIdx = (band.index.toFloat() / bands.size * 10).toInt().coerceIn(0, 9)
            val levelMb    = (presetDb[displayIdx] * 100)
                .coerceIn(band.minLevel.toInt(), band.maxLevel.toInt()).toShort()
            setEqBand(band.index, levelMb)
        }
        _activePreset.value = name
        viewModelScope.launch { dataStore.edit { it[KEY_EQ_PRESET] = name } }
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
        val sessionId = DroidampPlaybackService.audioSessionId.value
        Log.d("VizDebug", "attachVisualizer called, sessionId=$sessionId")
        if (sessionId == 0) {
            Log.d("VizDebug", "attachVisualizer: session ID is 0, deferring")
            return
        }
        try {
            val v = Visualizer(sessionId)
            // Clamp to 1024 — large capture sizes (max can be 16384+) don't improve our 20-band
            // output but would reduce FFT frequency resolution per-bin for no benefit.
            val targetSize = 1024.coerceIn(
                Visualizer.getCaptureSizeRange()[0],
                Visualizer.getCaptureSizeRange()[1],
            )
            v.captureSize = targetSize
            val err = v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(vis: Visualizer, waveform: ByteArray, sampleRate: Int) {
                    processWaveform(waveform)
                }
                override fun onFftDataCapture(vis: Visualizer, fft: ByteArray, sampleRate: Int) {
                    Log.d("VizDebug", "FFT data received, size=${fft.size}")
                    processFft(fft)
                }
            }, Visualizer.getMaxCaptureRate(), true, true)
            if (err != Visualizer.SUCCESS) {
                Log.e(TAG, "attachVisualizer: setDataCaptureListener failed, err=$err")
                v.release()
                return
            }
            val enableErr = v.setEnabled(true)
            if (enableErr != Visualizer.SUCCESS) {
                Log.e(TAG, "attachVisualizer: setEnabled(true) failed, err=$enableErr")
                v.release()
                return
            }
            visualizer = v
            Log.d(TAG, "attachVisualizer: attached to sessionId=$sessionId captureSize=$targetSize")
        } catch (e: Exception) {
            Log.e(TAG, "attachVisualizer: exception — ${e.message}")
        }
    }

    private fun detachVisualizer() {
        visualizer?.run { enabled = false; release() }
        visualizer = null
        _fftData.value    = FloatArray(20)  { 0f }
        _waveformData.value = FloatArray(128) { 0f }
    }

    private fun attachEqualizer() {
        detachEqualizer()
        val audioSessionId = DroidampPlaybackService.audioSessionId.value.takeIf { it != 0 } ?: return
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
            // Re-apply last-used preset after equalizer is attached
            applyPreset(_activePreset.value)
        } catch (_: Exception) {}
    }

    private fun detachEqualizer() {
        equalizer?.run { enabled = false; release() }
        equalizer = null
    }

    private fun processFft(fft: ByteArray) {
        val bands = 20

        // Android Visualizer FFT layout for N=captureSize bytes:
        //   fft[0]          = real[0]   (DC component)
        //   fft[1]          = real[N/2] (Nyquist component)
        //   fft[2k], fft[2k+1] = real[k], imag[k]  for k = 1 .. N/2-1
        //
        // Number of usable frequency bins (excluding DC and Nyquist):
        val numBins = (fft.size / 2) - 1   // e.g. 511 for captureSize=1024
        if (numBins < 1) return

        // Compute per-bin magnitudes using correct real+imag pairs.
        // Range: 0 .. sqrt(128² + 128²) ≈ 181; normalise to 0..1 by dividing by 128.
        val mags = FloatArray(numBins) { k ->
            val ri = 2 + k * 2
            val ii = ri + 1
            if (ii >= fft.size) return@FloatArray 0f
            val r = fft[ri].toFloat()
            val i = fft[ii].toFloat()
            sqrt(r * r + i * i) / 128f   // 0..~1.41, clamped below
        }

        // Map bins → bands with logarithmic spacing (perceptually natural for music).
        // startBin(b) = floor(numBins ^ (b / bands)), so low bands cover fewer bins
        // (individual bass frequencies) and high bands cover many bins (treble spread).
        val result = FloatArray(bands) { b ->
            val startBin = numBins.toFloat().pow(b.toFloat() / bands).toInt()
                .coerceIn(0, numBins - 1)
            val endBin   = numBins.toFloat().pow((b + 1f) / bands).toInt()
                .coerceIn(startBin + 1, numBins)
            // RMS magnitude across the bin range for this band
            val rms = sqrt(
                mags.slice(startBin until endBin)
                    .map { it * it }
                    .average()
                    .toFloat()
                    .coerceAtLeast(0f)
            )
            // Perceptual log scaling: ln(1 + v*25)/ln(26) maps 0..1 to 0..1 with good
            // sensitivity — quiet passages still move the bars, loud hits reach near 1.
            (ln(1f + rms * 25f) / ln(26f)).coerceIn(0f, 1f)
        }

        // Asymmetric smoothing: rise instantly on peaks, fall slowly
        val prev = _fftData.value
        _fftData.value = FloatArray(bands) { i ->
            val p = prev[i]; val r = result[i]
            if (r >= p) r else p * 0.82f + r * 0.18f
        }
    }

    private fun processWaveform(waveform: ByteArray) {
        val target = 128
        val step   = (waveform.size / target).coerceAtLeast(1)
        _waveformData.value = FloatArray(target) { i ->
            val idx = (i * step).coerceAtMost(waveform.size - 1)
            waveform[idx].toFloat() / 128f  // normalise to -1..1
        }
    }

    override fun onCleared() {
        detachAudioEffects()
        controller?.run { removeListener(playerListener); release() }
        super.onCleared()
    }
}
