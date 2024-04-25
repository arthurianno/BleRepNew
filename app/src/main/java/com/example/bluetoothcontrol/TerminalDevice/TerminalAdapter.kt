package com.example.bluetoothcontrol.TerminalDevice

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.databinding.ItemTermBinding

class TerminalAdapter : RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder>() {

     private val items = ArrayList<TermItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun update(items:ArrayList<TermItem>){
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }


    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TerminalViewHolder {
        val binding = ItemTermBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TerminalViewHolder(binding)
    }

     inner class TerminalViewHolder(private var binding: ItemTermBinding): RecyclerView.ViewHolder(binding.root) {
         fun bind(termItem: TermItem) {
             binding.nameOfData.text = termItem.nameOfTerm
             binding.nameOfAtributeData.text = termItem.name
             binding.textViewAttribute.text = termItem.command
         }
    }

    override fun onBindViewHolder(holder: TerminalAdapter.TerminalViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }
}