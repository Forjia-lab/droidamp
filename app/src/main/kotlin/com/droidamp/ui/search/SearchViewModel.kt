package com.droidamp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidamp.data.repository.NavidromeRepository
import com.droidamp.data.repository.SearchResults
import com.droidamp.domain.model.Album
import com.droidamp.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: SearchResults = SearchResults(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAlbum: Album? = null,
    val selectedAlbumTracks: List<Track> = emptyList(),
    val isLoadingTracks: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: NavidromeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _query
                .debounce(300L)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collect { q ->
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    repo.search(q).collect { result ->
                        result.fold(
                            onSuccess = { r -> _uiState.update { it.copy(results = r, isLoading = false) } },
                            onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } },
                        )
                    }
                }
        }
    }

    fun setQuery(q: String) {
        _uiState.update { it.copy(query = q) }
        _query.value = q
        if (q.isBlank()) _uiState.update { it.copy(results = SearchResults()) }
    }

    fun loadAlbumTracks(album: Album) {
        _uiState.update { it.copy(selectedAlbum = album, selectedAlbumTracks = emptyList(), isLoadingTracks = true) }
        viewModelScope.launch {
            repo.getAlbumTracks(album.id).collect { result ->
                result.fold(
                    onSuccess = { tracks -> _uiState.update { it.copy(selectedAlbumTracks = tracks, isLoadingTracks = false) } },
                    onFailure = { _uiState.update { it.copy(isLoadingTracks = false) } },
                )
            }
        }
    }

    fun clearAlbumSelection() {
        _uiState.update { it.copy(selectedAlbum = null, selectedAlbumTracks = emptyList()) }
    }
}
