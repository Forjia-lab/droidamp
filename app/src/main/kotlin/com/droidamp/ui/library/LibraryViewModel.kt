package com.droidamp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidamp.data.api.ServerUrlProvider
import com.droidamp.data.repository.NavidromeRepository
import com.droidamp.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val selectedAlbumTracks: List<Track> = emptyList(),
    val selectedAlbum: Album?       = null,
    val isLoading: Boolean          = false,
    val error: String?              = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: NavidromeRepository,
    private val serverUrlProvider: ServerUrlProvider,
) : ViewModel() {

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
            LibraryTab.Tracks    -> { /* search-based */ }
        }
    }

    fun loadAlbumTracks(album: Album) {
        _uiState.update { it.copy(selectedAlbum = album, isLoading = true) }
        viewModelScope.launch {
            repo.getAlbumTracks(album.id).collect { result ->
                result.fold(
                    onSuccess = { tracks -> _uiState.update { it.copy(selectedAlbumTracks = tracks, isLoading = false) } },
                    onFailure = { e   -> _uiState.update { it.copy(error = e.message, isLoading = false) } },
                )
            }
        }
    }

    fun clearAlbumSelection() { _uiState.update { it.copy(selectedAlbum = null, selectedAlbumTracks = emptyList()) } }

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
