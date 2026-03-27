package com.droidamp.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GigBagDao {

    // ── Bags ─────────────────────────────────────────────────

    @Query("""
        SELECT gb.*, COUNT(gbt.id) AS trackCount
        FROM gig_bags gb
        LEFT JOIN gig_bag_tracks gbt ON gb.id = gbt.gigBagId
        GROUP BY gb.id
        ORDER BY gb.name ASC
    """)
    fun getAllWithCount(): Flow<List<GigBagWithCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bag: GigBagEntity)

    @Update
    suspend fun update(bag: GigBagEntity)

    @Query("DELETE FROM gig_bags WHERE id = :id")
    suspend fun deleteById(id: String)

    // ── Tracks ────────────────────────────────────────────────

    @Query("SELECT * FROM gig_bag_tracks WHERE gigBagId = :bagId ORDER BY position ASC")
    fun getTracksForBag(bagId: String): Flow<List<GigBagTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: GigBagTrackEntity)

    @Query("DELETE FROM gig_bag_tracks WHERE id = :id")
    suspend fun deleteTrack(id: String)

    @Query("SELECT COALESCE(MAX(position), -1) FROM gig_bag_tracks WHERE gigBagId = :bagId")
    suspend fun maxPosition(bagId: String): Int

    @Query("SELECT COUNT(*) > 0 FROM gig_bag_tracks WHERE gigBagId = :bagId AND trackId = :trackId")
    suspend fun containsTrack(bagId: String, trackId: String): Boolean
}

data class GigBagWithCount(
    @Embedded val bag: GigBagEntity,
    @ColumnInfo(name = "trackCount") val trackCount: Int,
)
