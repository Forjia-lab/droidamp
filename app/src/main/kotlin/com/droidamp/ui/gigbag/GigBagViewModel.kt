package com.droidamp.ui.gigbag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidamp.data.local.GigBagRepository
import com.droidamp.data.local.db.GigBagEntity
import com.droidamp.data.local.db.GigBagTrackEntity
import com.droidamp.data.local.db.GigBagWithCount
import com.droidamp.domain.model.Track   // used in addTrackToBag, createBagAndAddTrack, toTrack()
import com.droidamp.domain.model.TrackSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GigBagViewModel @Inject constructor(
    private val repository: GigBagRepository,
) : ViewModel() {

    val bagsWithCount: StateFlow<List<GigBagWithCount>> =
        repository.getAllWithCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedBagId = MutableStateFlow<String?>(null)

    // Emits raw entities so the UI has access to entity.id for removal
    val selectedBagTracks: StateFlow<List<GigBagTrackEntity>> = _selectedBagId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getTracksForBag(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    fun selectBag(id: String?) { _selectedBagId.value = id }

    fun addTrackToBag(bagId: String, bagName: String, track: Track) {
        viewModelScope.launch {
            if (repository.containsTrack(bagId, track.id)) {
                _snackbarMessage.value = "Already in $bagName"
            } else {
                repository.addTrack(bagId, track)
                _snackbarMessage.value = "Added to $bagName"
            }
        }
    }

    fun removeTrackEntry(entityId: String) {
        viewModelScope.launch { repository.removeTrackEntry(entityId) }
    }

    fun createBagAndAddTrack(name: String, track: Track) {
        viewModelScope.launch {
            val bag = repository.createBag(name)
            repository.addTrack(bag.id, track)
            _snackbarMessage.value = "Added to ${bag.name}"
        }
    }

    fun createBag(name: String) {
        viewModelScope.launch { repository.createBag(name) }
    }

    fun renameBag(bag: GigBagEntity, newName: String) {
        viewModelScope.launch { repository.renameBag(bag, newName) }
    }

    fun deleteBag(id: String) {
        viewModelScope.launch { repository.deleteBag(id) }
    }

    fun clearSnackbar() { _snackbarMessage.value = null }
}

internal fun GigBagTrackEntity.toTrack() = Track(
    id          = trackId,
    title       = title,
    artist      = artist,
    album       = album,
    albumId     = albumId,
    duration    = duration,
    trackNumber = trackNumber,
    year        = year,
    suffix      = suffix,
    streamUrl   = streamUrl,
    coverArtId  = coverArtId,
    source      = runCatching { TrackSource.valueOf(source) }.getOrDefault(TrackSource.LOCAL),
)
