package com.bledemo.ui.view.adapters.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bledemo.database.model.SyncDataModel
import com.bledemo.databinding.ItemTestConnectionBinding
import com.bledemo.utils.DateTimeUtil
import java.util.*

class TestConnectionAdapter(val list: List<SyncDataModel>) :
    RecyclerView.Adapter<TestConnectionAdapter.TestConnectionViewHolder>() {
    class TestConnectionViewHolder(private val binding: ItemTestConnectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(syncDataModel: SyncDataModel) {
            binding.tvTime.text = syncDataModel.timeInMillis?.let {
                DateTimeUtil.millisToFormat(
                    it, DateTimeUtil.FULL_DATE_FORMAT,
                    TimeZone.getDefault()
                )
            }
            binding.tvMessage.text = "Data Obtained from ${syncDataModel.deviceName}"

            binding.root.setOnClickListener {
//                Snackbar.make(
//                    it,
//                    "SleepData : ${syncDataModel.sleepQuantity}, HRV : ${syncDataModel.hrvData}, Stress : ${syncDataModel.stressData}",
//                    Snackbar.LENGTH_SHORT
//                ).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestConnectionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemTestConnectionBinding.inflate(layoutInflater, parent, false)
        return TestConnectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TestConnectionViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}