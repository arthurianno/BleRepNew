package com.example.bluetoothcontrol.ReadingData

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.R
import com.example.bluetoothcontrol.databinding.ItemReadingDataBinding

class ReadingDataAdapter : RecyclerView.Adapter<ReadingDataAdapter.ReadingDataViewHolder>() {

     val items = ArrayList<DataItem>()
    private var callBack : CallBackOnReadingItem? = null

    @SuppressLint("NotifyDataSetChanged")
    fun update(items: ArrayList<DataItem>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    fun addCallBack(callBackOnReadingItem: CallBackOnReadingItem){
        this.callBack = callBackOnReadingItem
    }

    fun updateAttributeColor(position: Int, isValueChanged: Boolean) {
        items[position].isValueChanged = isValueChanged
        notifyItemChanged(position)
    }



    inner class ReadingDataViewHolder(val binding: ItemReadingDataBinding):RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(dataItem: DataItem) {
            itemView.setOnClickListener {
                callBack?.onItemClickReadingData(dataItem)
            }
            binding.hexData.text = dataItem.hexName
            binding.nameOfData.text = dataItem.name
            binding.nameOfAddressData.text = dataItem.address.toString()
            binding.nameOfAtributeData.apply {
                text = dataItem.attributeName
                if (dataItem.isValueChanged) {
                    setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
                } else {
                    setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReadingDataViewHolder {
        val binding = ItemReadingDataBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReadingDataViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ReadingDataViewHolder, position: Int) {
        holder.bind(items[position])
    }
    interface CallBackOnReadingItem{
        fun onItemClickReadingData(item:DataItem)
    }
}