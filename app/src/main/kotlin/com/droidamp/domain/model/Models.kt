package com.droidamp.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String,
    val duration: Long,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val size: Long = 0,
    val suffix: String = "",
    val coverArtId: String? = null,
    val streamUrl: String = "",
    val source: TrackSource = TrackSource.NAVIDROME,
)

data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val artistId: String,
    val year: Int = 0,
    val trackCount: Int = 0,
    val coverArtId: String? = null,
)

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val coverArtId: String? = null,
)

data class Playlist(
    val id: String,
    val name: String,
    val trackCount: Int = 0,
    val duration: Long = 0,
    val owner: String = "",
)

enum class TrackSource { NAVIDROME, LOCAL, SOUNDCLOUD, BANDCAMP, RADIO }
enum class RepeatMode { OFF, ALL, ONE }

data class PlayerState(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffled: Boolean = false,
    val volume: Float = 1f,
)
