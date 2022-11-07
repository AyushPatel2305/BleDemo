package com.bledemo.database.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity(tableName = "SyncData")
class SyncDataModel : Serializable {
    @ColumnInfo(name = "mac_address")
    @SerializedName("mac_address")
    var macAddress: String? = null

    @ColumnInfo(name = "device_name")
    @SerializedName("device_name")
    var deviceName: String? = null

    @ColumnInfo(name = "uuid")
    @SerializedName("uuid")
    var uuid: String? = null

    @ColumnInfo(name = "data")
    @SerializedName("data")
    var data: String? = null

    @PrimaryKey
    @ColumnInfo(name = "time")
    @SerializedName("time")
    var timeInMillis: Long? = null

}
