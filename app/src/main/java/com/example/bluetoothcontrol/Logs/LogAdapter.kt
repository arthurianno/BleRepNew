package com.example.bluetoothcontrol.Logs

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.Devices.DevicesAdapter
import com.example.bluetoothcontrol.R
import com.example.bluetoothcontrol.ReadingData.DataItem
import com.example.bluetoothcontrol.databinding.ItemLogBinding
import java.util.ArrayList
import kotlin.math.log

class LogAdapter: RecyclerView.Adapter<LogAdapter.LogAdapterHolder>() {

     val items = ArrayList<LogItem>()
     private var callBack: DevicesAdapter.CallBack? = null


    @SuppressLint("NotifyDataSetChanged")
    fun updateLog(items: ArrayList<LogItem>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }
     class LogAdapterHolder(private val binding: ItemLogBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(logItem: LogItem){
            binding.apply {
                tagLog.text = logItem.TAG
                descriptionLog.text = logItem.message
                when(logItem.type){
                    "e" -> {
                        tagLog.setTextColor(ContextCompat.getColor(itemView.context, R.color.red)) // Лог ошибки (e) - красный цвет текста
                        descriptionLog.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
                    }
                    "d" -> {
                        tagLog.setTextColor(ContextCompat.getColor(itemView.context, R.color.blue)) // Лог отладки (d) - синий цвет текста
                        descriptionLog.setTextColor(ContextCompat.getColor(itemView.context, R.color.blue))
                    }
                    else -> {
                        tagLog.setTextColor(ContextCompat.getColor(itemView.context, R.color.black)) // Другие типы логов - черный цвет текста (можно изменить на другой цвет по вашему выбору)
                        descriptionLog.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                    }
                }
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