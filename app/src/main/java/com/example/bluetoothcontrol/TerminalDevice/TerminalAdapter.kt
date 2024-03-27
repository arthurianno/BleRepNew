package com.example.bluetoothcontrol.TerminalDevice

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.databinding.ItemTermBinding

class TerminalAdapter : RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TerminalAdapter.TerminalViewHolder {
        TODO("Not yet implemented")
    }

     inner class TerminalViewHolder(private val binding: ItemTermBinding): RecyclerView.ViewHolder(binding.root) {
         fun bind(){

         }
    }

    override fun onBindViewHolder(holder: TerminalAdapter.TerminalViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }
}