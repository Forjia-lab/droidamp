package com.droidamp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities    = [GigBagEntity::class, GigBagTrackEntity::class],
    version     = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gigBagDao(): GigBagDao

    companion object {
        const val NAME = "droidamp.db"
    }
}
