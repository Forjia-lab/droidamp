package com.droidamp.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

// ─────────────────────────────────────────────────────────────
//  Subsonic REST API — Navidrome implements this fully
//  Docs: http://www.subsonic.org/pages/api.jsp
//  All calls include auth params (injected via OkHttp interceptor)
// ─────────────────────────────────────────────────────────────

interface SubsonicApiService {

    @GET("rest/ping")
    suspend fun ping(): SubsonicResponse<Unit>

    @GET("rest/getArtists")
    suspend fun getArtists(): SubsonicResponse<ArtistsResult>

    @GET("rest/getArtist")
    suspend fun getArtist(@Query("id") artistId: String): SubsonicResponse<ArtistResult>

    @GET("rest/getAlbum")
    suspend fun getAlbum(@Query("id") albumId: String): SubsonicResponse<AlbumResult>

    @GET("rest/getAlbumList2")
    suspend fun getAlbumList(
        @Query("type") type: String = "alphabeticalByName",
        @Query("size") size: Int = 500,
        @Query("offset") offset: Int = 0,
    ): SubsonicResponse<AlbumListResult>

    @GET("rest/search3")
    suspend fun search(
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int = 10,
        @Query("albumCount") albumCount: Int = 20,
        @Query("songCount") songCount: Int = 50,
    ): SubsonicResponse<SearchResult>

    @GET("rest/getPlaylist")
    suspend fun getPlaylist(@Query("id") playlistId: String): SubsonicResponse<PlaylistResult>

    @GET("rest/getPlaylists")
    suspend fun getPlaylists(): SubsonicResponse<PlaylistsResult>

    @GET("rest/getStarred2")
    suspend fun getStarred(): SubsonicResponse<StarredResult>

    @GET("rest/star")
    suspend fun star(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null,
    ): SubsonicResponse<Unit>

    @GET("rest/unstar")
    suspend fun unstar(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null,
    ): SubsonicResponse<Unit>

    @GET("rest/scrobble")
    suspend fun scrobble(
        @Query("id") trackId: String,
        @Query("time") time: Long,
        @Query("submission") submission: Boolean = true,
    ): SubsonicResponse<Unit>

    @GET("rest/getRandomSongs")
    suspend fun getRandomSongs(
        @Query("size") size: Int = 50,
        @Query("genre") genre: String? = null,
    ): SubsonicResponse<RandomSongsResult>

    @GET("rest/getSongsByGenre")
    suspend fun getSongsByGenre(
        @Query("genre") genre: String,
        @Query("count") count: Int = 500,
    ): SubsonicResponse<SongsByGenreResult>

    @GET("rest/getGenres")
    suspend fun getGenres(): SubsonicResponse<GenresResult>
}

// ─── Response wrappers ───────────────────────────────────────

data class SubsonicResponse<T>(
    @SerializedName("subsonic-response")
    val subsonic_response: SubsonicWrapper<T>? = null
)

data class SubsonicWrapper<T>(
    val status: String,
    val version: String,
    val type: String? = null,
    val serverVersion: String? = null,
    val artists: ArtistsResult? = null,
    val artist: ArtistResult? = null,
    val album: AlbumResult? = null,
    val albumList2: AlbumListResult? = null,
    val searchResult3: SearchResult? = null,
    val playlist: PlaylistResult? = null,
    val playlists: PlaylistsResult? = null,
    val starred2: StarredResult? = null,
    val randomSongs: RandomSongsResult? = null,
    val songsByGenre: SongsByGenreResult? = null,
    val genres: GenresResult? = null,
    val error: SubsonicError? = null,
)

data class SubsonicError(val code: Int, val message: String)

// ─── Result types ────────────────────────────────────────────

data class ArtistsResult(val index: List<ArtistIndex> = emptyList())
data class ArtistIndex(val name: String, val artist: List<ApiArtist> = emptyList())

data class ArtistResult(
    val id: String, val name: String,
    val album: List<ApiAlbum> = emptyList(),
    val coverArt: String? = null,
    val albumCount: Int = 0,
)

data class AlbumResult(
    val id: String, val name: String,
    val artist: String = "", val artistId: String = "",
    val year: Int = 0, val coverArt: String? = null,
    val song: List<ApiSong> = emptyList(),
)

data class AlbumListResult(val album: List<ApiAlbum> = emptyList())

data class SearchResult(
    val artist: List<ApiArtist> = emptyList(),
    val album: List<ApiAlbum> = emptyList(),
    val song: List<ApiSong> = emptyList(),
)

data class PlaylistResult(
    val id: String, val name: String,
    val owner: String = "", val songCount: Int = 0,
    val duration: Long = 0,
    val entry: List<ApiSong> = emptyList(),
)

data class PlaylistsResult(val playlist: List<ApiPlaylist> = emptyList())
data class ApiPlaylist(val id: String, val name: String, val songCount: Int = 0, val duration: Long = 0, val owner: String = "")

data class StarredResult(
    val artist: List<ApiArtist> = emptyList(),
    val album: List<ApiAlbum> = emptyList(),
    val song: List<ApiSong> = emptyList(),
)

data class RandomSongsResult(val song: List<ApiSong> = emptyList())
data class SongsByGenreResult(val song: List<ApiSong> = emptyList())
data class GenresResult(val genre: List<ApiGenre> = emptyList())
data class ApiGenre(val value: String, val songCount: Int = 0, val albumCount: Int = 0)

// ─── API models ──────────────────────────────────────────────

data class ApiArtist(
    val id: String, val name: String,
    val coverArt: String? = null, val albumCount: Int = 0,
)

data class ApiAlbum(
    val id: String, val name: String,
    val artist: String = "", val artistId: String = "",
    val year: Int = 0, val coverArt: String? = null,
    val songCount: Int = 0, val duration: Long = 0,
)

data class ApiSong(
    val id: String, val title: String,
    val album: String = "", val albumId: String = "",
    val artist: String = "", val artistId: String = "",
    val track: Int = 0, val year: Int = 0,
    val duration: Int = 0,           // seconds
    val size: Long = 0,
    val suffix: String = "",
    val coverArt: String? = null,
    val starred: String? = null,
    val replayGain: ApiReplayGain? = null,
)

data class ApiReplayGain(
    val trackGain: Float? = null,
    val albumGain: Float? = null,
    val trackPeak: Float? = null,
    val albumPeak: Float? = null,
)
