package com.droidamp.data.local

import com.droidamp.data.local.db.GigBagDao
import com.droidamp.data.local.db.GigBagEntity
import com.droidamp.data.local.db.GigBagTrackEntity
import com.droidamp.data.local.db.GigBagWithCount
import com.droidamp.domain.model.Track
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GigBagRepository @Inject constructor(private val dao: GigBagDao) {

    fun getAllWithCount(): Flow<List<GigBagWithCount>> = dao.getAllWithCount()

    fun getTracksForBag(bagId: String): Flow<List<GigBagTrackEntity>> =
        dao.getTracksForBag(bagId)

    suspend fun createBag(name: String): GigBagEntity {
        val bag = GigBagEntity(name = name.trim())
        dao.insert(bag)
        return bag
    }

    suspend fun renameBag(bag: GigBagEntity, newName: String) {
        dao.update(bag.copy(name = newName.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteBag(id: String) = dao.deleteById(id)

    suspend fun containsTrack(bagId: String, trackId: String): Boolean =
        dao.containsTrack(bagId, trackId)

    suspend fun removeTrackEntry(entityId: String) = dao.deleteTrack(entityId)

    suspend fun addTrack(bagId: String, track: Track) {
        val nextPos = dao.maxPosition(bagId) + 1
        dao.insertTrack(
            GigBagTrackEntity(
                gigBagId    = bagId,
                trackId     = track.id,
                position    = nextPos,
                title       = track.title,
                artist      = track.artist,
                album       = track.album,
                albumId     = track.albumId,
                duration    = track.duration,
                trackNumber = track.trackNumber,
                year        = track.year,
                suffix      = track.suffix,
                streamUrl   = track.streamUrl,
                coverArtId  = track.coverArtId,
                source      = track.source.name,
            )
        )
    }
}
