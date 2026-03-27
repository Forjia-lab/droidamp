package com.droidamp.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "gig_bag_tracks",
    foreignKeys = [
        ForeignKey(
            entity      = GigBagEntity::class,
            parentColumns = ["id"],
            childColumns  = ["gigBagId"],
            onDelete    = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("gigBagId")],
)
data class GigBagTrackEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val gigBagId:    String,
    val trackId:     String,
    val position:    Int,
    val title:       String,
    val artist:      String,
    val album:       String,
    val albumId:     String = "",
    val duration:    Long,
    val trackNumber: Int = 0,
    val year:        Int = 0,
    val suffix:      String = "",
    val streamUrl:   String,
    val coverArtId:  String? = null,
    val source:      String,          // TrackSource.name
)
