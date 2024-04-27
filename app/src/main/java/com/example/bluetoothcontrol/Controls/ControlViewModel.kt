package com.example.bluetoothcontrol.Controls

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bluetoothcontrol.BluetoothAdapterProvider
import com.example.bluetoothcontrol.MainActivity
import com.example.bluetoothcontrol.MainActivity.Companion.controlManager
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedList
import java.util.Queue
import java.util.TimeZone

class ControlViewModel(private val adapterProvider: BluetoothAdapterProvider, private val context: Context): ViewModel() {
    private val controlManager = MainActivity.controlManager
    private val _isConnected = MutableLiveData<Boolean>().apply { value = false }
    private val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    var sliceSize = 0;
    val isConnected: LiveData<Boolean> get() = _isConnected
    var pinCode: String? = null
    private var lastConnectedDevice: BluetoothDevice? = null
    private var connectionCallback: ConnectionCallback? = null
    private var disCallback: DisconnectionCallback? = null



    init {
        //  есть ли сохраненный пин-код для устройства
        val savedPinCode = getSavedPinCode()
        if (savedPinCode != null) {
            pinCode = savedPinCode
        }else{
            Log.e("ControlViewModel","PinCode is $pinCode")
        }
    }

    fun setConnectionCallback(callback: ConnectionCallback) {
        connectionCallback = callback
    }
    fun setDisconnectionCallBack(callback: DisconnectionCallback){
        disCallback = callback
    }

    fun removeConnectionCallback() {
        connectionCallback = null
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

        adapterProvider.getAdapter()
    }


    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun disconnect() {
        if (_isConnected.value == true) {
            controlManager.disconnect().enqueue()
            showToast("Устройство отключено")
            _isConnected.postValue(false)
            lastConnectedDevice = null
        }
    }

    private val connectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {
            Log.e("ControlViewModel", "onDeviceConnecting: $device")
            _isConnected.postValue(false)
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            Log.e("ControlViewModel", "onDeviceConnected: $device")
            _isConnected.postValue(true)
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            Log.e("ControlViewModel", "onDeviceFailedToConnect: $device, reason: $reason")
            _isConnected.postValue(false)
            connectionCallback?.onDeviceFailedToConnect()

        }

        override fun onDeviceReady(device: BluetoothDevice) {
            Log.e("ControlViewModel,", "OnDeviceReady() devices is ready")
            _isConnected.postValue(true)
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            Log.e("ControlViewModel", "onDeviceDisconnecting: $device")
            _isConnected.postValue(false)


        }

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            Log.e("ControlViewModel", "onDeviceDisconnected: $device, reason: $reason")
            _isConnected.postValue(false)
            disCallback?.onDeviceDisconnected()

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
        fun readDeviceProfile(sliceSize: Int) {
            if (controlManager.isConnected && sliceSize == 128) {
                val addresses = intArrayOf(
                    0x00, 0x04, 0x08, 0x0C, 0x10, 0x14, 0x18, // Адреса для UIC_slope_t
                    0x1C, 0x20, 0x24, 0x28, 0x2C, // Адреса для других полей
                    0x30, 0x34, 0x38, 0x3C, 0x4C, 0x5C, 0x70, 0x6C, 0x7C // Адреса для DeviceProfile_t
                )
                val sizes = intArrayOf(
                    0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, // Размеры для UIC_slope_t
                    0x04, 0x04, 0x04, 0x04, 0x04, // Размеры для других полей
                    0x04, 0x04, 0x04, 0x10, 0x10, 0x10, 0x0C, 0x04, 0x04 // Размеры для DeviceProfile_t
                )
                val checks = arrayOf(
                    EntireCheck.I_0UA, EntireCheck.I_2UA, EntireCheck.I_10UA, EntireCheck.I_20UA,
                    EntireCheck.I_30UA, EntireCheck.I_40UA, EntireCheck.I_60UA, // Проверки для UIC_slope_t
                    EntireCheck.Tref_mV, EntireCheck.R1_Ohm, EntireCheck.Uref, EntireCheck.Uw,
                    EntireCheck.T10ref_C, // Проверки для других полей
                    EntireCheck.SETUP_TIME, EntireCheck.POW_VOLT, EntireCheck.CONFIG_WORD,
                    EntireCheck.SETUP_OPERATOR, EntireCheck.HW_VER, EntireCheck.SER_NUM,
                    EntireCheck.RESERV, EntireCheck.LOCAL_TIME_SH, EntireCheck.CRC32 // Проверки для DeviceProfile_t
                )

                for (i in addresses.indices) {
                    controlManager.readData(addresses[i], sizes[i], checks[i])
                }
            } else {
                // Если не подключен или размер не равен 128, читаем только поля UIC_slope_t и другие поля
                val addresses = intArrayOf(
                    0x00, 0x04, 0x08, 0x0C, 0x10, 0x14, 0x18, // Адреса для UIC_slope_t
                    0x1C, 0x20, 0x24, 0x28, 0x2C // Адреса для других полей
                )
                val sizes = intArrayOf(
                    0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, // Размеры для UIC_slope_t
                    0x04, 0x04, 0x04, 0x04, 0x04 // Размеры для других полей
                )
                val checks = arrayOf(
                    EntireCheck.I_0UA, EntireCheck.I_2UA, EntireCheck.I_10UA, EntireCheck.I_20UA,
                    EntireCheck.I_30UA, EntireCheck.I_40UA, EntireCheck.I_60UA, // Проверки для UIC_slope_t
                    EntireCheck.Tref_mV, EntireCheck.R1_Ohm, EntireCheck.Uref, EntireCheck.Uw,
                    EntireCheck.T10ref_C // Проверки для других полей
                )

                for (i in addresses.indices) {
                    controlManager.readData(addresses[i], sizes[i], checks[i])
                }
            }
        }

        @SuppressLint("SimpleDateFormat")
        fun readTerminalCommands(){
            if(controlManager.isConnected){
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
                calendar.timeInMillis = System.currentTimeMillis()
                // Отнимаем 3 часа
                calendar.add(Calendar.HOUR_OF_DAY, -3)
                val dateFormat = SimpleDateFormat("yyMMddHHmmss")
                val formattedTime = dateFormat.format(calendar.time)
                BleControlManager.requestDataTermItem.value?.clear()
                controlManager.sendCommand("settime.$formattedTime",EntireCheck.default_command,"SETTIME")
                controlManager.sendCommand("gettime",EntireCheck.default_command,"Time")
                controlManager.sendCommand("version",EntireCheck.default_command,"Version")
                controlManager.sendCommand("battery",EntireCheck.default_command,"Battery")
                controlManager.sendCommand("serial",EntireCheck.default_command,"Serial")
                controlManager.sendCommand("mac",EntireCheck.default_command,"Mac address")
                controlManager.sendCommand("find",EntireCheck.default_command,"FIND")
                controlManager.sendCommand("rd.$000",EntireCheck.default_command,"RD")
                controlManager.sendCommand("rd.$001",EntireCheck.default_command,"RD")
                controlManager.sendCommand("rd.$002",EntireCheck.default_command,"RD")
                controlManager.sendCommand("rd.$003",EntireCheck.default_command,"RD")
                controlManager.sendCommand("rd.$004",EntireCheck.default_command,"RD")

            }
        }
        fun readTerminalCommandSpinner(command:String,number:String){
            if(controlManager.isConnected){
                when(command){
                    "TIME" -> controlManager.sendCommand("gettime",EntireCheck.default_command,"Time")
                    "VERSION" -> controlManager.sendCommand("version",EntireCheck.default_command,"Version")
                    "BATTERY" -> controlManager.sendCommand("battery",EntireCheck.default_command,"Battery")
                    "SERIAL" ->  controlManager.sendCommand("serial",EntireCheck.default_command,"Serial")
                    "MAC" -> controlManager.sendCommand("mac",EntireCheck.default_command,"Mac address")
                    "RD" -> controlManager.sendCommand("rd.$number",EntireCheck.default_command,"RD")
                    "ERASE" -> controlManager.sendCommand("erase",EntireCheck.default_command,"ERASE")
                    "SETTIME" -> controlManager.sendCommand("settime.$number",EntireCheck.default_command,"SETTIME")
                    "FIND" -> controlManager.sendCommand("find",EntireCheck.default_command,"FIND")
                }
                Log.e("ControlViewModel", "settime.$number")
            }
        }
    }
    interface ConnectionCallback {
        fun onDeviceFailedToConnect()
    }
    interface DisconnectionCallback{
        fun onDeviceDisconnected()
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