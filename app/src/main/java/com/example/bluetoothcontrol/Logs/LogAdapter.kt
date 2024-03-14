package com.example.bluetoothcontrol.Logs

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.Devices.DevicesAdapter
import com.example.bluetoothcontrol.ReadingData.DataItem
import com.example.bluetoothcontrol.databinding.ItemLogBinding
import java.util.ArrayList

class LogAdapter: RecyclerView.Adapter<LogAdapter.LogAdapterHolder>() {

    private val items = ArrayList<DataItem>()
     private var callBack: DevicesAdapter.CallBack? = null

    @SuppressLint("NotifyDataSetChanged")
    fun updateLog(items: ArrayList<DataItem>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }
     class LogAdapterHolder(private val binding: ItemLogBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(logItem: DataItem){
            binding.apply {
                nameOfDataLog.text = logItem.name
                hexDataLog.text = logItem.hexName
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogAdapterHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return LogAdapterHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: LogAdapterHolder, position: Int) {
        holder.bind(items[position])
    }
}