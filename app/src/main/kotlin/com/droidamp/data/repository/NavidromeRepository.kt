package com.droidamp.data.repository

import com.droidamp.data.api.ApiAlbum
import com.droidamp.data.api.ApiArtist
import com.droidamp.data.api.ApiPlaylist
import com.droidamp.data.api.ApiSong
import com.droidamp.data.api.ServerUrlProvider
import com.droidamp.data.api.SubsonicApiService
import com.droidamp.domain.model.Album
import com.droidamp.domain.model.Artist
import com.droidamp.domain.model.Playlist
import com.droidamp.domain.model.Track
import com.droidamp.domain.model.TrackSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavidromeRepository @Inject constructor(
    private val api: SubsonicApiService,
    private val serverUrlProvider: ServerUrlProvider,
) {
    fun buildStreamUrl(songId: String): String =
        "${serverUrlProvider.baseUrl()}/rest/stream.view?id=$songId&" +
        serverUrlProvider.authParams() + "&format=raw"

    fun buildCoverArtUrl(id: String, size: Int = 300): String =
        "${serverUrlProvider.baseUrl()}/rest/getCoverArt.view?id=$id&" +
        serverUrlProvider.authParams() + "&size=$size"

    suspend fun ping(): Result<Boolean> = runCatching {
        api.ping().subsonic_response?.status == "ok"
    }

    fun getArtists(): Flow<Result<List<Artist>>> = flow {
        emit(runCatching {
            api.getArtists().subsonic_response?.artists?.index
                ?.flatMap { it.artist }
                ?.map { it.toArtist() } ?: emptyList()
        })
    }

    fun getAllAlbums(): Flow<Result<List<Album>>> = flow {
        emit(runCatching {
            api.getAlbumList().subsonic_response?.albumList2?.album
                ?.map { it.toAlbum() } ?: emptyList()
        })
    }

    fun getAlbumTracks(albumId: String): Flow<Result<List<Track>>> = flow {
        emit(runCatching {
            val albumResult = api.getAlbum(albumId).subsonic_response?.album
            val coverArtUrl = albumResult?.coverArt?.let { buildCoverArtUrl(it) }
            albumResult?.song?.map { it.toTrack(coverArtUrl, serverUrlProvider.baseUrl()) }
                ?: emptyList()
        })
    }

    fun getPlaylists(): Flow<Result<List<Playlist>>> = flow {
        emit(runCatching {
            api.getPlaylists().subsonic_response?.playlists?.playlist
                ?.map { it.toPlaylist() } ?: emptyList()
        })
    }

    fun getPlaylistTracks(playlistId: String): Flow<Result<List<Track>>> = flow {
        emit(runCatching {
            api.getPlaylist(playlistId).subsonic_response?.playlist?.entry
                ?.map { it.toTrack(null, serverUrlProvider.baseUrl()) } ?: emptyList()
        })
    }
}

private fun ApiArtist.toArtist() = Artist(
    id = id,
    name = name,
    albumCount = albumCount,
    coverArtId = coverArt,
)

private fun ApiAlbum.toAlbum() = Album(
    id = id,
    name = name,
    artist = artist,
    artistId = artistId,
    year = year,
    trackCount = songCount,
    coverArtId = coverArt,
)

private fun ApiSong.toTrack(coverArtUrl: String?, baseUrl: String): Track {
    val artUrl = coverArt?.let { "$baseUrl/rest/getCoverArt.view?id=$it&v=1.16.1&c=droidamp&size=300" }
        ?: coverArtUrl
    return Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration.toLong() * 1000L,
        trackNumber = track,
        year = year,
        size = size,
        suffix = suffix,
        coverArtId = artUrl,
        streamUrl = "$baseUrl/rest/stream.view?id=$id&v=1.16.1&c=droidamp",
        source = TrackSource.NAVIDROME,
    )
}

private fun ApiPlaylist.toPlaylist() = Playlist(
    id = id,
    name = name,
    trackCount = songCount,
    duration = duration,
    owner = owner,
)
