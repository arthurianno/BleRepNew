package com.example.bluetoothcontrol.Devices

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.Logger
import com.example.bluetoothcontrol.R
import com.example.bluetoothcontrol.SharedViewModel
import com.example.bluetoothcontrol.databinding.ItemDeviceBinding

class DevicesAdapter(private val callback:CallBack,private val sharedViewModel: SharedViewModel): RecyclerView.Adapter<DevicesAdapter.DevicesViewHolder>() {

    private val items = ArrayList<BluetoothDevice>()
    private val filteredItems = ArrayList<BluetoothDevice>()
    private var callBack: CallBack? = null
    private var filter: String? = null
    private var selectedItemPosition: Int = RecyclerView.NO_POSITION
    private var connecting: Boolean = false



    @SuppressLint("NotifyDataSetChanged")
    fun update(newItems: List<BluetoothDevice>) {
        items.clear()
        items.addAll(newItems)
        applyFilter() // Применяем фильтр после обновления списка
    }
    @SuppressLint("NotifyDataSetChanged")
    fun setFilter(filter: String?) {
        this.filter = filter
        notifyDataSetChanged() // Перерисовываем список при изменении фильтра
    }
    @SuppressLint("NotifyDataSetChanged")
    fun setConnecting(isConnecting: Boolean) {
        this.connecting = isConnecting
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setSelected(position: Int) {
        if (selectedItemPosition != position) {
            val previousSelectedItem = selectedItemPosition
            selectedItemPosition = position
            notifyItemChanged(previousSelectedItem)
            notifyItemChanged(position)
            //notifyDataSetChanged()
        }
    }
    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    private fun applyFilter() {
        filteredItems.clear()
        filteredItems.addAll(items.filter { it.name?.contains(filter.orEmpty(), ignoreCase = true) == true })
        notifyDataSetChanged() // Уведомляем адаптер об изменениях
    }
    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        items.clear()
        filteredItems.clear()
        notifyDataSetChanged()
        Log.e("DeviceAdapter", "Devices list is cleared")
        Logger.e("DeviceAdapter", "Devices list is cleared")
    }

    fun addCallBack(callBack: CallBack){
        this.callBack = callBack
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevicesViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context),parent,false)

        return DevicesViewHolder(binding)
    }

    @SuppressLint("MissingPermission")
    override fun getItemCount(): Int {
        return filteredItems.size
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: DevicesViewHolder, position: Int) {
        holder.bind(filteredItems[position],position == selectedItemPosition, connecting)

        val defaultColor = ContextCompat.getColor(holder.itemView.context, android.R.color.transparent)
        holder.itemView.setBackgroundColor(defaultColor)
        ViewCompat.setElevation(holder.itemView, 0f)
    }

    @SuppressLint("ResourceType")
    inner class DevicesViewHolder(private val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {


        @SuppressLint("MissingPermission")
        fun bind(item: BluetoothDevice, isSelected: Boolean, connecting: Boolean) {
            itemView.setOnClickListener {
                if (!connecting) {
                    setSelected(adapterPosition)
                    callback.onItemClick(item)
                }
            }
            binding.apply {
                textName.text = item.name ?: textName.context.getString(R.string.unnamed_device)
                textAddress.text = item.address
                itemView.isSelected = isSelected
            }
        }
    }


        interface CallBack{
        fun onItemClick(device: BluetoothDevice)
    }
}