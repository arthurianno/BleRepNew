package com.example.bluetoothcontrol

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _selectedDeviceAddress = MutableLiveData<String>()
    val selectedDeviceAddress: LiveData<String>
        get() = _selectedDeviceAddress
    val devName = MutableLiveData<String>()
    val mtuValue = MutableLiveData<String>()
    val intervalValue = MutableLiveData<String>()
    val timerActiveFragment = MutableLiveData<Boolean>()



    fun updateDeviceAddress(address: String) {
        _selectedDeviceAddress.postValue(address)
    }

    fun updateDevName(name: String) {
        devName.value = name
    }
}

