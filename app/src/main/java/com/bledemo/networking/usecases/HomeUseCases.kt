package com.bledemo.networking.usecases

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.bledemo.database.model.SyncDataModel
import com.bledemo.model.BaseErrorModel
import com.bledemo.model.BaseModel
import com.bledemo.networking.network.CallbackObserver
import com.bledemo.networking.network.Networking
import com.bledemo.utils.Session
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class HomeUseCases(
    val mContext: Context,
    private var errorLiveData: MutableLiveData<BaseErrorModel>,
    private var responseUpdateData: MutableLiveData<String>? = null,
) {
    private val session by lazy { Session(mContext) }

    fun updateData(request: SyncDataModel?) {
        if (request == null) return
        Networking
            .with(mContext)
            .getServices()
            .updateData(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CallbackObserver<BaseModel<Any?>>() {
                override fun onSuccess(response: BaseModel<Any?>) {
                    responseUpdateData?.value = response.message
                }

                override fun onFailed(code: Int, message: String) {
                    errorLiveData.value = BaseErrorModel(message, code)
                }

            })
    }

}