package com.droidamp.data.local

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.droidamp.domain.model.Album
import com.droidamp.domain.model.Artist
import com.droidamp.domain.model.Track
import com.droidamp.domain.model.TrackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataScanner: TrackMetadataScanner,
) {
    companion object {
        val REQUIRED_PERMISSION: String get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_AUDIO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE

        private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, REQUIRED_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED

    fun scanTracks(): List<Track> {
        if (!hasPermission()) return emptyList()

        val tracks  = mutableListOf<Track>()
        val uri     = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cols    = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            @Suppress("DEPRECATION") MediaStore.Audio.Media.DATA,
        )
        val sel     = "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} > 30000"
        val sort    = "${MediaStore.Audio.Media.ARTIST} ASC, ${MediaStore.Audio.Media.ALBUM} ASC, ${MediaStore.Audio.Media.TRACK} ASC"

        context.contentResolver.query(uri, cols, sel, null, sort)?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol      = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val sizeCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            @Suppress("DEPRECATION")
            val dataCol     = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id      = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val streamUri = ContentUris
                    .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    .toString()
                val coverUri  = ContentUris
                    .withAppendedId(ALBUM_ART_URI, albumId)
                    .toString()
                val suffix   = cursor.getString(mimeCol)?.substringAfterLast('/') ?: "mp3"
                // Derive file extension: prefer the actual filename (DATA column) over
                // the MIME subtype, which can be "mpeg" instead of "mp3" or "x-wav".
                val filePath  = if (dataCol >= 0) cursor.getString(dataCol) else null
                val extension = filePath?.substringAfterLast('.')?.lowercase()
                    ?.takeIf { it.isNotBlank() && it.length <= 5 }
                    ?: suffix.removePrefix("x-").lowercase()
                val scanResult = metadataScanner.scan(streamUri, extension)

                tracks.add(Track(
                    id          = "local:$id",
                    title       = cursor.getString(titleCol) ?: "Unknown",
                    artist      = cursor.getString(artistCol) ?: "Unknown Artist",
                    album       = cursor.getString(albumCol) ?: "Unknown Album",
                    albumId     = "local_album:$albumId",
                    duration    = cursor.getLong(durCol),
                    trackNumber = cursor.getInt(trackCol),
                    year        = cursor.getInt(yearCol),
                    size        = cursor.getLong(sizeCol),
                    suffix      = suffix,
                    coverArtId  = coverUri,
                    streamUrl   = streamUri,
                    source      = TrackSource.LOCAL,
                    bpm         = scanResult.bpm,
                    camelotKey  = scanResult.camelotKey,
                    replayGain  = scanResult.replayGain,
                ))
            }
        }
        return tracks
    }

    fun albumsFromTracks(tracks: List<Track>): List<Album> =
        tracks
            .groupBy { it.albumId }
            .map { (albumId, albumTracks) ->
                val first = albumTracks.first()
                Album(
                    id         = albumId,
                    name       = first.album,
                    artist     = first.artist,
                    artistId   = "local_artist:${first.artist}",
                    year       = first.year,
                    trackCount = albumTracks.size,
                    coverArtId = first.coverArtId,
                )
            }
            .sortedBy { it.name }

    fun artistsFromTracks(tracks: List<Track>): List<Artist> =
        tracks
            .groupBy { it.artist }
            .map { (name, artistTracks) ->
                Artist(
                    id         = "local_artist:$name",
                    name       = name,
                    albumCount = artistTracks.map { it.albumId }.distinct().size,
                )
            }
            .sortedBy { it.name }

    fun tracksForAlbum(albumId: String, allTracks: List<Track>): List<Track> =
        allTracks.filter { it.albumId == albumId }.sortedBy { it.trackNumber }

    fun albumsForArtist(artistId: String, allTracks: List<Track>): List<Album> =
        albumsFromTracks(allTracks.filter { "local_artist:${it.artist}" == artistId })
}
