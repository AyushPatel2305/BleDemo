package com.bledemo.ui.viewModel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bledemo.model.BaseErrorModel
import com.bledemo.ui.view.BaseActivity

open class BaseViewModel(private val mContext: Context) : ViewModel() {
    var isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val isRefreshing: MutableLiveData<Boolean> = MutableLiveData()

    val errorLiveData: MutableLiveData<BaseErrorModel> = MutableLiveData()
    val refreshLiveData: MutableLiveData<Boolean> = MutableLiveData()
    var isUpdating = false

    init {
        errorLiveData.observeForever {
            isUpdating = false
            if (it.code == 1) {
                refreshLiveData.value = true
            }
            if (it.message != "IllegalStateException") {
                it.message?.let { it1 -> Toast.makeText(mContext, it1, Toast.LENGTH_SHORT).show() }
//                errorDialog.setMessage("Something went wrong. Please try again later.").show()
            }
            isLoading.value = false
            isRefreshing.value = false
        }

        isLoading.observe(mContext as BaseActivity) {
            if (it) mContext.showProgressbar()
            else mContext.hideProgressbar()
        }
    }

    override fun onCleared() {
        super.onCleared()
    }


}