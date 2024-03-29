package com.example.bluetoothcontrol.Controls
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bluetoothcontrol.BluetoothAdapterProvider
import com.example.bluetoothcontrol.Devices.DevicesAdapter
import com.example.bluetoothcontrol.Devices.DevicesFragment
import com.example.bluetoothcontrol.MainActivity
import com.example.bluetoothcontrol.MainActivity.Companion.controlManager
import com.example.bluetoothcontrol.SharedViewModel
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.LinkedList
import java.util.Queue

class ControlViewModel(private val adapterProvider: BluetoothAdapterProvider, private val context: Context): ViewModel() {
    private val controlManager = MainActivity.controlManager
    private val _isConnected = MutableLiveData<Boolean>().apply { value = false }
    private val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    var sliceSize = 0;
    val isConnected: LiveData<Boolean> get() = _isConnected
    var pinCode: String? = null
    private var lastConnectedDevice: BluetoothDevice? = null


    init {
        //  есть ли сохраненный пин-код для устройства
        val savedPinCode = getSavedPinCode()
        if (savedPinCode != null) {
            pinCode = savedPinCode
        }else{
            Log.e("ControlViewModel","PinCode is $pinCode")
        }
    }
    fun connect(deviceAddress: String,mode: String) {
        if (_isConnected.value == false) {
            val device = adapterProvider.getAdapter().getRemoteDevice(deviceAddress)
            lastConnectedDevice = device
            // Проверяем, есть ли сохраненный пин-код для этого устройства
            val savedPinCode = getSavedPinCodeForDevice(device.address)
            if (savedPinCode != null) {
                pinCode = savedPinCode
            }
            controlManager.connect(device)
                .useAutoConnect(false)
                .done {
                    showToast("Соединение с устройством успешно!")
                    _isConnected.postValue(true)
                    Log.d("ControlViewModel", "Connection success ${_isConnected.value}")
                    if (savedPinCode != null) {
                        controlManager.sendPinCommand(pinCode ?: "", EntireCheck.PIN_CODE_RESULT,mode)
                        Log.d("ControlViewModel", "Saving PIN-Code for device: $deviceAddress, PIN: $pinCode")
                        savePinCodeForDevice(deviceAddress,pinCode ?: "")
                    }
                }
                .fail { _, status ->
                    _isConnected.postValue(false)
                    Log.d("ControlViewModel", "Connection failed ${_isConnected.value}")
                }
                .enqueue()
            controlManager.setConnectionObserver(connectionObserver)
        }else{
            Log.e("ControlViewModel","PinCode is $pinCode")
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun disconnect() {
        if (_isConnected.value == true) {
            controlManager.disconnect().enqueue()
            showToast("DeviceDisconnected")
            _isConnected.postValue(false)
            lastConnectedDevice = null
        }
    }

    private val connectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {
            Log.d("ControlViewModel", "onDeviceConnecting: $device")
            _isConnected.postValue(false)
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            Log.d("ControlViewModel", "onDeviceConnected: $device")
            _isConnected.postValue(true)
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            Log.e("ControlViewModel", "onDeviceFailedToConnect: $device, reason: $reason")
            _isConnected.postValue(false)

        }

        override fun onDeviceReady(device: BluetoothDevice) {
            Log.e("ControlViewModel,", "OnDeviceReady() devices is ready")
            _isConnected.postValue(true)
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            Log.d("ControlViewModel", "onDeviceDisconnecting: $device")
            _isConnected.postValue(false)

        }

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            Log.d("ControlViewModel", "onDeviceDisconnected: $device, reason: $reason")
            _isConnected.postValue(false)
        }
    }

//    fun sendCommand(command: String) {
//        if (isConnected.value == true) {
//            controlManager.sendCommand(command,EntireCheck.default_command)
//        } else {
//            Log.e("ControlViewModel", "Device is not connected")
//        }
//    }


    private fun getSavedPinCode(): String? {
        return sharedPreferences.getString(lastConnectedDevice?.address, null)
    }

    fun savePinCodeForDevice(deviceAddress: String, pinCode: String) {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(deviceAddress, pinCode)
        editor.apply()
    }

    fun getSavedPinCodeForDevice(deviceAddress: String): String? {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString(deviceAddress, null)
    }



    companion object {
        val entireCheckQueue: Queue<EntireCheck> = LinkedList()
        fun readDeviceProfile(sliseSize: Int) {
            if (controlManager.isConnected && sliseSize == 128) {
                // Чтение каждого поля из структуры UIC_slope_t
                controlManager.readData(0x00, 0x04, EntireCheck.I_0UA); // i_0uA
                controlManager.readData(0x04, 0x04, EntireCheck.I_2UA); // i_2uA
                controlManager.readData(0x08, 0x04, EntireCheck.I_10UA); // i_10uA
                controlManager.readData(0x0C, 0x04, EntireCheck.I_20UA); // i_20uA
                controlManager.readData(0x10, 0x04, EntireCheck.I_30UA); // i_30uA
                controlManager.readData(0x14, 0x04, EntireCheck.I_40UA); // i_40uA
                controlManager.readData(0x18, 0x04, EntireCheck.I_60UA); // i_60uA
                // Чтение каждого поля из структуры
                controlManager.readData(0x1C, 0x04, EntireCheck.Tref_mV) // Tref_mV
                controlManager.readData(0x20, 0x04, EntireCheck.R1_Ohm) // R1_Ohm
                controlManager.readData(0x24, 0x04, EntireCheck.Uref) // Uref
                controlManager.readData(0x28, 0x04, EntireCheck.Uw) // Uw
                controlManager.readData(0x2C, 0x04, EntireCheck.T10ref_C) // T10ref_C
                // Чтение каждого поля из структуры DeviceProfile_t
                controlManager.readData(0x30, 0x04, EntireCheck.SETUP_TIME) // setupTime
                controlManager.readData(0x34, 0x04, EntireCheck.POW_VOLT) // powVoltK
                controlManager.readData(0x38, 0x04, EntireCheck.CONFIG_WORD) // configWord
                controlManager.readData(0x3C, 0x10, EntireCheck.SETUP_OPERATOR) // setupOperator
                controlManager.readData(0x4C, 0x10, EntireCheck.HW_VER) // hw_ver
                controlManager.readData(0x5C, 0x10, EntireCheck.SER_NUM) // serialNumber
                controlManager.readData(0x70, 0x0C, EntireCheck.RESERV) // reserv
                controlManager.readData(0x6C, 0x04, EntireCheck.LOCAL_TIME_SH) // localTimeShift
                controlManager.readData(0x7C, 0x04, EntireCheck.CRC32) // crc32
            }else{
                // Чтение каждого поля из структуры UIC_slope_t
                controlManager.readData(0x00, 0x04, EntireCheck.I_0UA); // i_0uA
                controlManager.readData(0x04, 0x04, EntireCheck.I_2UA); // i_2uA
                controlManager.readData(0x08, 0x04, EntireCheck.I_10UA); // i_10uA
                controlManager.readData(0x0C, 0x04, EntireCheck.I_20UA); // i_20uA
                controlManager.readData(0x10, 0x04, EntireCheck.I_30UA); // i_30uA
                controlManager.readData(0x14, 0x04, EntireCheck.I_40UA); // i_40uA
                controlManager.readData(0x18, 0x04, EntireCheck.I_60UA); // i_60uA
                // Чтение каждого поля из структуры
                controlManager.readData(0x1C, 0x04, EntireCheck.Tref_mV); // Tref_mV
                controlManager.readData(0x20, 0x04, EntireCheck.R1_Ohm); // R1_Ohm
                controlManager.readData(0x24, 0x04, EntireCheck.Uref); // Uref
                controlManager.readData(0x28, 0x04, EntireCheck.Uw); // Uw
                controlManager.readData(0x2C, 0x04, EntireCheck.T10ref_C); // T10ref_C
                // Чтение каждого поля из структуры DeviceProfile_t
                controlManager.readData(0x30, 0x04, EntireCheck.SETUP_TIME); // setupTime
                controlManager.readData(0x34, 0x04, EntireCheck.POW_VOLT); // powVoltK
                controlManager.readData(0x38, 0x04, EntireCheck.CONFIG_WORD); // configWord
                controlManager.readData(0x3C, 0x10, EntireCheck.SETUP_OPERATOR); // setupOperator
                controlManager.readData(0x4C, 0x10, EntireCheck.HW_VER); // hw_ver
                controlManager.readData(0x5C, 0x10, EntireCheck.SER_NUM); // serialNumber
            }
        }
    }

}
class ControlViewModelFactory(private val adapterProvider: BluetoothAdapterProvider, private val context: Context): ViewModelProvider.NewInstanceFactory(){
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ControlViewModel::class.java)){
            return ControlViewModel(adapterProvider, context) as T
        }
        throw IllegalArgumentException("View model not Found")
    }
}