package com.droidamp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidamp.data.api.ServerUrlProvider
import com.droidamp.data.local.LocalMediaRepository
import com.droidamp.data.repository.NavidromeRepository
import com.droidamp.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
//  LibraryViewModel
// ─────────────────────────────────────────────────────────────

sealed class LibraryTab { object Artists : LibraryTab(); object Albums : LibraryTab(); object Tracks : LibraryTab(); object Playlists : LibraryTab() }

data class LibraryUiState(
    val tab: LibraryTab             = LibraryTab.Albums,
    val artists: List<Artist>       = emptyList(),
    val albums: List<Album>         = emptyList(),
    val playlists: List<Playlist>   = emptyList(),
    val tracks: List<Track>         = emptyList(),
    val selectedAlbumTracks: List<Track>    = emptyList(),
    val selectedAlbum: Album?               = null,
    val selectedArtist: Artist?             = null,
    val selectedArtistAlbums: List<Album>   = emptyList(),
    val selectedPlaylist: Playlist?         = null,
    val selectedPlaylistTracks: List<Track> = emptyList(),
    val isLoading: Boolean          = false,
    val error: String?              = null,
    val localTrackCount: Int        = 0,
    val localHasPermission: Boolean = false,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: NavidromeRepository,
    private val serverUrlProvider: ServerUrlProvider,
    private val localRepo: LocalMediaRepository,
) : ViewModel() {

    // Cache of locally-scanned tracks for ID-based lookups
    private var localTracks: List<Track> = emptyList()

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadAlbums()
        viewModelScope.launch {
            serverUrlProvider.settingsUpdated.collect { reload() }
        }
    }

    fun reload() = selectTab(_uiState.value.tab)

    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(tab = tab) }
        when (tab) {
            LibraryTab.Albums    -> loadAlbums()
            LibraryTab.Artists   -> loadArtists()
            LibraryTab.Playlists -> loadPlaylists()
            LibraryTab.Tracks    -> loadTracks()
        }
    }

    fun loadAlbumTracks(album: Album) {
        _uiState.update { it.copy(selectedAlbum = album, isLoading = true) }
        if (album.id.startsWith("local_album:")) {
            viewModelScope.launch(Dispatchers.IO) {
                val tracks = localRepo.tracksForAlbum(album.id, localTracks)
                _uiState.update { it.copy(selectedAlbumTracks = tracks, isLoading = false) }
            }
        } else {
            viewModelScope.launch {
                repo.getAlbumTracks(album.id).collect { result ->
                    result.fold(
                        onSuccess = { tracks -> _uiState.update { it.copy(selectedAlbumTracks = tracks, isLoading = false) } },
                        onFailure = { e   -> _uiState.update { it.copy(error = e.message, isLoading = false) } },
                    )
                }
            }
        }
    }

    fun clearAlbumSelection() { _uiState.update { it.copy(selectedAlbum = null, selectedAlbumTracks = emptyList()) } }

    fun loadArtistAlbums(artist: Artist) {
        _uiState.update { it.copy(selectedArtist = artist, isLoading = true) }
        if (artist.id.startsWith("local_artist:")) {
            viewModelScope.launch(Dispatchers.IO) {
                val albums = localRepo.albumsForArtist(artist.id, localTracks)
                _uiState.update { it.copy(selectedArtistAlbums = albums, isLoading = false) }
            }
        } else {
            viewModelScope.launch {
                repo.getArtistAlbums(artist.id).collect { result ->
                    result.fold(
                        onSuccess = { albums -> _uiState.update { it.copy(selectedArtistAlbums = albums, isLoading = false) } },
                        onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } },
                    )
                }
            }
        }
    }

    fun clearArtistSelection() { _uiState.update { it.copy(selectedArtist = null, selectedArtistAlbums = emptyList()) } }

    fun scanLocalMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasPermission = localRepo.hasPermission()
            if (hasPermission) {
                val tracks = localRepo.scanTracks()
                localTracks = tracks
                _uiState.update { it.copy(localTrackCount = tracks.size, localHasPermission = true) }
            } else {
                _uiState.update { it.copy(localHasPermission = false) }
            }
        }
    }

    fun refreshLocalPermission() {
        _uiState.update { it.copy(localHasPermission = localRepo.hasPermission()) }
        if (localRepo.hasPermission()) scanLocalMedia()
    }

    fun loadPlaylistTracks(playlist: Playlist) {
        _uiState.update { it.copy(selectedPlaylist = playlist, isLoading = true) }
        viewModelScope.launch {
            repo.getPlaylistTracks(playlist.id).collect { result ->
                result.fold(
                    onSuccess = { tracks -> _uiState.update { it.copy(selectedPlaylistTracks = tracks, isLoading = false) } },
                    onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } },
                )
            }
        }
    }

    fun clearPlaylistSelection() { _uiState.update { it.copy(selectedPlaylist = null, selectedPlaylistTracks = emptyList()) } }

    private fun loadTracks() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repo.getRandomTracks().collect { result ->
                result.fold(
                    onSuccess = { tracks -> _uiState.update { it.copy(tracks = tracks, isLoading = false) } },
                    onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } },
                )
            }
        }
    }

    private fun loadAlbums() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repo.getAllAlbums().collect { result ->
                result.fold(
                    onSuccess = { albums -> _uiState.update { it.copy(albums = albums, isLoading = false) } },
                    onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } },
                )
            }
        }
    }

    private fun loadArtists() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repo.getArtists().collect { result ->
                result.fold(
                    onSuccess = { artists -> _uiState.update { it.copy(artists = artists, isLoading = false) } },
                    onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } },
                )
            }
        }
    }

    private fun loadPlaylists() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repo.getPlaylists().collect { result ->
                result.fold(
                    onSuccess = { pl -> _uiState.update { it.copy(playlists = pl, isLoading = false) } },
                    onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } },
                )
            }
        }
    }
}
