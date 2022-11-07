package com.bledemo.database.dao

import androidx.room.*
import com.bledemo.database.model.SyncDataModel

@Dao
interface SyncDataDao {
    @Query("Select * from SyncData")
    fun fetchDeviceData(): List<SyncDataModel>

    @Query("delete from SyncData")
    fun deleteAllData()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(syncDataList: SyncDataModel?)

    @Update
    fun update(syncDataList: SyncDataModel?)

    @Delete
    fun delete(syncDataList: SyncDataModel?)

//    @Query("Select * from SyncData " + "WHERE MAC_Address LIKE '% :macAddress %' AND UUID LIKE '% :uuid %'")
//    fun getPreviousData(macAddress: String, uuid: String): SyncDataModel
}