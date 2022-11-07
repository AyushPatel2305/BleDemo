package com.bledemo.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bledemo.database.dao.SyncDataDao
import com.bledemo.database.model.SyncDataModel

@Database(entities = [SyncDataModel::class], version = 1)
abstract class ActivityDatabase : RoomDatabase() {

    abstract fun syncDataDao(): SyncDataDao

}