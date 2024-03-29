package com.example.bluetoothcontrol.TerminalDevice

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.ReadingData.DataItem
import com.example.bluetoothcontrol.databinding.ItemTermBinding

class TerminalAdapter : RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder>() {

    private val items = ArrayList<DataItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun update(items:ArrayList<DataItem>){
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TerminalViewHolder {
        val binding = ItemTermBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TerminalViewHolder(binding)
    }

     inner class TerminalViewHolder(private var binding: ItemTermBinding): RecyclerView.ViewHolder(binding.root) {
         fun bind(termItem: DataItem){
             binding.nameOfAtributeData.text = termItem.attributeName
             binding.nameOfData.text = termItem.name
         }
    }

    override fun onBindViewHolder(holder: TerminalAdapter.TerminalViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }
}