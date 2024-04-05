package com.example.bluetoothcontrol

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.bluetoothcontrol.Logs.LogItem

 object Logger {
    val _items = MutableLiveData<ArrayList<LogItem>>()
    private var isInitialized = false
    fun init(context: Context) {
        if (!isInitialized) {
            isInitialized = true
        }
    }

    fun e(tag: String, message: String) {
        Log.e(tag, message)
        addItem(LogItem(tag, message,"e"))
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addItem(LogItem(tag, message,"d"))
    }

    private fun addItem(logItem: LogItem) {
        val currentItems = _items.value ?: ArrayList()
        currentItems.add(logItem)
        _items.value = currentItems
    }
}