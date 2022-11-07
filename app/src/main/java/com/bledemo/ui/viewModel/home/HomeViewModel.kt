package com.bledemo.ui.viewModel.home

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.MutableLiveData
import com.bledemo.database.model.SyncDataModel
import com.bledemo.networking.usecases.HomeUseCases
import com.bledemo.ui.viewModel.BaseViewModel
import okhttp3.internal.notify


class HomeViewModel(context: Context) : BaseViewModel(context) {
    val responseUpdateData: MutableLiveData<String> = MutableLiveData()
    private val homeUseCase =
        HomeUseCases(context, errorLiveData, responseUpdateData = responseUpdateData)

    fun updateData(request: SyncDataModel) = homeUseCase.updateData(request)

}