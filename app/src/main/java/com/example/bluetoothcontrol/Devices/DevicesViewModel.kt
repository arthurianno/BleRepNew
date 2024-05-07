package com.example.bluetoothcontrol.Devices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bluetoothcontrol.BluetoothAdapterProvider
import com.example.bluetoothcontrol.Logger


class DevicesViewModel(adapterProvider: BluetoothAdapterProvider): ViewModel() {

    private val _devices : MutableLiveData<List<BluetoothDevice>?> = MutableLiveData()
    val devices : MutableLiveData<List<BluetoothDevice>?> get() = _devices

    private val adapter = adapterProvider.getAdapter()
    private var scanner: BluetoothLeScanner? = null
    private var callback: BleScanCallBack? = null
    private val settings: ScanSettings
    private val filters: List<ScanFilter>
    private var isFilterApplied: Boolean = true

    private val foundDevices = LinkedHashMap<String, BluetoothDevice>()

    init {
        settings = buildSettings()
        filters = buildFilter()
    }

    fun clearData(){
        devices.value = null
        _devices.value = null
        foundDevices.clear()
    }
    private fun buildSettings() =
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

    private fun buildFilter() =
        listOf(
            ScanFilter.Builder()
                .build()
        )

    @SuppressLint("MissingPermission")
    fun startScan(){
        if(callback == null) {
            callback = BleScanCallBack()
            scanner = adapter.bluetoothLeScanner
            scanner?.startScan(filters, settings, callback)
            Log.e("DevicesViewModel","StartScanning")
            Logger.e("DevicesViewModel","StartScanning")
        }
    }
    override fun onCleared() {
        super.onCleared()
        stopScan()
        clearScanCache()
    }

    @SuppressLint("MissingPermission")
    fun stopScan(){
        if(callback != null){
            scanner?.stopScan(callback)
            scanner = null
            callback = null
            Log.e("DevicesViewModel","StopScanning")
            Logger.e("DevicesViewModel","StopScanning")
        }
    }
    @SuppressLint("MissingPermission")
    fun clearScanCache() {
        scanner?.flushPendingScanResults(callback)
        Log.e("DevicesViewModel","CASH IS CLEARED")
        Logger.e("DevicesViewModel","CASH IS CLEARED")
    }


    inner class BleScanCallBack: ScanCallback(){


        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result ->
                if(result.device.name?.contains("satellite", ignoreCase = true) == true) {
                    foundDevices[result.device.address] = result.device
                }
            }
            _devices.postValue(foundDevices.values.toList())
            super.onBatchScanResults(results)
        }

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null && result.device.name?.contains("satellite", ignoreCase = true) == true) {
                foundDevices[result.device.address] = result.device
            }
            _devices.postValue(foundDevices.values.toList()) // reverse the list
            super.onScanResult(callbackType, result)
        }


        override fun onScanFailed(errorCode: Int) {
            foundDevices.clear()
            Log.e("BluetoothScanner", "OnScanFailed")
            Logger.e("BluetoothScanner", "OnScanFailed")
        }
    }
    companion object{
        var FILTER_NAME = "SatelliteOnline0005"
    }
}


class DeviceViewModelFactory(private val adapterProvider: BluetoothAdapterProvider): ViewModelProvider.Factory{

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(DevicesViewModel::class.java)){
            return DevicesViewModel(adapterProvider) as T
        }
        throw IllegalArgumentException("View Model not Found")
    }

}
