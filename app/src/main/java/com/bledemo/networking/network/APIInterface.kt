package com.bledemo.networking.network

import com.bledemo.database.model.SyncDataModel
import com.bledemo.model.BaseModel
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST

interface APIInterface {

    @POST("75153bed-0839-4e1f-9aa5-ed8eb82720a1")
    fun updateData(@Body body: SyncDataModel): Observable<BaseModel<Any?>>

}
