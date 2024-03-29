package com.example.bluetoothcontrol.Devices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.R
import com.example.bluetoothcontrol.SharedViewModel
import com.example.bluetoothcontrol.databinding.ItemDeviceBinding

class DevicesAdapter(private val callback:CallBack,private val sharedViewModel: SharedViewModel): RecyclerView.Adapter<DevicesAdapter.DevicesViewHolder>() {

    private val items = ArrayList<BluetoothDevice>()
    private var callBack: CallBack? = null


    @SuppressLint("NotifyDataSetChanged")
    fun update(items: List<BluetoothDevice>){
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }
    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        items.clear()
        notifyDataSetChanged()
        Log.e("DeviceAdapter", "Devices list is cleared")
    }

    fun addCallBack(callBack: CallBack){
        this.callBack = callBack
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevicesViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context),parent,false)

        return DevicesViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: DevicesViewHolder, position: Int) {
       holder.bind(items[position])
    }

     inner class DevicesViewHolder(private val binding: ItemDeviceBinding): RecyclerView.ViewHolder(binding.root){

         @SuppressLint("MissingPermission")
         fun bind(item: BluetoothDevice) {
             itemView.setOnClickListener {
                 callback.onItemClick(item)
             }
             binding.apply {
                 textName.text = item.name ?: textName.context.getString(R.string.unnamed_device)
                 textAddress.text = item.address
             }
         }
    }

    interface CallBack{
        fun onItemClick(device: BluetoothDevice)
    }
}