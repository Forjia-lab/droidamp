package com.droidamp.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.id3.ID3v24Tag
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────
//  TrackMetadataScanner
//
//  Reads TBPM / TKEY tags from local media files.
//  Must be called from a background thread (IO dispatcher).
//
//  • WAV  — streams via ContentResolver, parses RIFF chunks in
//            memory to find the "id3 " chunk; no file path needed.
//  • Other (MP3, FLAC, …) — copies to a temp file in cacheDir
//            via ContentResolver, scans with jaudiotagger, deletes.
//
//  This avoids raw file-path access which is blocked by Android
//  10+ scoped storage even when READ_MEDIA_AUDIO is granted.
// ─────────────────────────────────────────────────────────────

private const val TAG = "TrackMetaScan"

@Singleton
class TrackMetadataScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private val KEY_TO_CAMELOT: Map<String, String> = mapOf(
            "C"   to "8B",  "Am"  to "8A",
            "G"   to "9B",  "Em"  to "9A",
            "D"   to "10B", "Bm"  to "10A",
            "A"   to "11B", "F#m" to "11A",
            "E"   to "12B", "C#m" to "12A",
            "B"   to "1B",  "G#m" to "1A",
            "F#"  to "2B",  "D#m" to "2A",
            "Db"  to "3B",  "Bbm" to "3A",
            "Ab"  to "4B",  "Fm"  to "4A",
            "Eb"  to "5B",  "Cm"  to "5A",
            "Bb"  to "6B",  "Gm"  to "6A",
            "F"   to "7B",  "Dm"  to "7A",
            "C#"  to "3B",  "A#m" to "3A",
            "D#"  to "5B",  "A#"  to "6B",
            "Gb"  to "2B",  "Ebm" to "2A",
        )

        init {
            Logger.getLogger("org.jaudiotagger").level = Level.SEVERE
        }
    }

    /**
     * Reads BPM and musical key from a MediaStore content URI.
     * [extension] is the file extension ("wav", "mp3", "flac", …) used
     * to select the right parse strategy.
     *
     * Blocking — call only from an IO-dispatched coroutine or thread.
     * Returns Pair(bpm, camelotKey) e.g. ("72", "9A · Em"), both nullable.
     */
    fun scan(contentUriString: String, extension: String): Pair<String?, String?> {
        val uri = Uri.parse(contentUriString)
        Log.d(TAG, "scan uri=$contentUriString  ext=$extension")
        return when (extension.lowercase()) {
            "wav"  -> scanWavViaStream(uri)
            else   -> scanViaJAudioTagger(uri, extension)
        }
    }

    // ── WAV: stream RIFF chunks directly, no temp file ────────────

    private fun scanWavViaStream(uri: Uri): Pair<String?, String?> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                readWavId3FromStream(stream)
            } ?: run {
                Log.w(TAG, "scanWavViaStream: ContentResolver returned null stream for $uri")
                null to null
            }
        } catch (e: Exception) {
            Log.e(TAG, "scanWavViaStream: ${e.javaClass.simpleName}: ${e.message}")
            null to null
        }
    }

    private fun readWavId3FromStream(stream: InputStream): Pair<String?, String?> {
        // Verify RIFF/WAVE header (12 bytes)
        val header = ByteArray(12)
        if (stream.readFully(header) != 12) return null to null
        if (header.sliceStr(0, 4) != "RIFF" || header.sliceStr(8, 4) != "WAVE") {
            Log.w(TAG, "readWavId3FromStream: not a RIFF/WAVE file")
            return null to null
        }

        val chunkId   = ByteArray(4)
        val chunkSize = ByteArray(4)

        while (true) {
            if (stream.readFully(chunkId)   != 4) break
            if (stream.readFully(chunkSize) != 4) break

            val id   = chunkId.sliceStr(0, 4)
            val size = (ByteBuffer.wrap(chunkSize).order(ByteOrder.LITTLE_ENDIAN).int.toLong()) and 0xFFFFFFFFL
            Log.d(TAG, "readWavId3FromStream: chunk='$id'  size=$size")

            if (id.trim().lowercase() == "id3") {
                val data = ByteArray(size.toInt())
                val read = stream.readFully(data)
                Log.d(TAG, "readWavId3FromStream: id3 chunk found, read $read/${data.size} bytes")
                val tag = parseId3Bytes(data) ?: return null to null
                return extractBpmKey(tag, "wav-stream")
            }

            // Skip chunk; WAV chunks are padded to even offsets
            val aligned = size + if (size % 2L != 0L) 1L else 0L
            stream.skipFully(aligned)
        }
        Log.d(TAG, "readWavId3FromStream: no id3 chunk found")
        return null to null
    }

    // ── Non-WAV: copy to temp file, scan with jaudiotagger ────────

    private fun scanViaJAudioTagger(uri: Uri, extension: String): Pair<String?, String?> {
        // Use thread ID in name so parallel scans don't collide
        val tempFile = File(context.cacheDir, "droidamp_meta_${Thread.currentThread().id}.$extension")
        return try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (copied == null) {
                Log.w(TAG, "scanViaJAudioTagger: null stream for $uri")
                return null to null
            }
            Log.d(TAG, "scanViaJAudioTagger: temp=${tempFile.path}  size=${tempFile.length()}")
            val tag = runCatching { AudioFileIO.read(tempFile).tag }.getOrElse {
                Log.e(TAG, "scanViaJAudioTagger: jaudiotagger exception: ${it.message}")
                null
            }
            if (tag == null) null to null else extractBpmKey(tag, extension)
        } catch (e: Exception) {
            Log.e(TAG, "scanViaJAudioTagger: ${e.javaClass.simpleName}: ${e.message}")
            null to null
        } finally {
            tempFile.delete()
        }
    }

    // ── ID3 byte-buffer parsing (for WAV id3 chunk) ───────────────

    /** Detects ID3v2.3 vs v2.4 from the version byte and parses. */
    private fun parseId3Bytes(data: ByteArray): Tag? {
        if (data.size < 10) return null
        if (data[0] != 'I'.code.toByte() || data[1] != 'D'.code.toByte() || data[2] != '3'.code.toByte()) {
            Log.w(TAG, "parseId3Bytes: data does not start with ID3 header")
            return null
        }
        val majorVersion = data[3].toInt() and 0xFF
        Log.d(TAG, "parseId3Bytes: ID3v2.$majorVersion")
        val bb = ByteBuffer.wrap(data)
        return try {
            when (majorVersion) {
                4    -> ID3v24Tag(bb)
                else -> ID3v23Tag(bb)
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseId3Bytes: v2.$majorVersion parse failed (${e.message}), retrying as v2.3")
            try { ID3v23Tag(ByteBuffer.wrap(data)) } catch (_: Exception) { null }
        }
    }

    // ── Field reading + key formatting ────────────────────────────

    private fun extractBpmKey(tag: Tag, label: String): Pair<String?, String?> {
        val rawBpm = readField(tag, FieldKey.BPM, label)
        val bpm    = rawBpm?.let { it.toFloatOrNull()?.toInt()?.toString() ?: it }
        val rawKey = readField(tag, FieldKey.KEY, label)
        Log.d(TAG, "extractBpmKey: bpm=$bpm  key=$rawKey  [$label]")
        return bpm to rawKey?.let { buildKeyDisplay(it) }
    }

    private fun readField(tag: Tag, key: FieldKey, label: String): String? = try {
        tag.getFirst(key).takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        Log.w(TAG, "readField $key failed [$label]: ${e.javaClass.simpleName}: ${e.message}")
        null
    }

    /** "Em" → "9A · Em"; already-Camelot "9A" passes through unchanged. */
    private fun buildKeyDisplay(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.matches(Regex("\\d{1,2}[AB]"))) return trimmed
        val camelot = resolveKey(trimmed)
        return if (camelot != null) "$camelot · $trimmed" else trimmed
    }

    private fun resolveKey(raw: String): String? {
        KEY_TO_CAMELOT[raw]?.let { return it }
        val lower = raw.lowercase()
        val normalized = when {
            lower.endsWith(" major") -> raw.substringBefore(" ").trim().replaceFirstChar { it.uppercaseChar() }
            lower.endsWith("maj")   -> raw.dropLast(3).trim().replaceFirstChar { it.uppercaseChar() }
            lower.endsWith(" minor") -> raw.substringBefore(" ").trim().replaceFirstChar { it.uppercaseChar() } + "m"
            lower.endsWith("min")   -> raw.dropLast(3).trim().replaceFirstChar { it.uppercaseChar() } + "m"
            else -> null
        }
        return normalized?.let { KEY_TO_CAMELOT[it] }
    }

    // ── InputStream helpers ───────────────────────────────────────

    /** Reads exactly [buf.size] bytes, returns how many were actually read. */
    private fun InputStream.readFully(buf: ByteArray): Int {
        var offset = 0
        while (offset < buf.size) {
            val n = read(buf, offset, buf.size - offset)
            if (n < 0) break
            offset += n
        }
        return offset
    }

    /** Skips [n] bytes, looping until all are skipped or EOF. */
    private fun InputStream.skipFully(n: Long) {
        var remaining = n
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped <= 0) break
            remaining -= skipped
        }
    }

    private fun ByteArray.sliceStr(offset: Int, len: Int) =
        String(this, offset, len, Charsets.US_ASCII)
}
