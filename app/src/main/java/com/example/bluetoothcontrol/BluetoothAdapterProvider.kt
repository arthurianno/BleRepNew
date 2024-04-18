package com.example.bluetoothcontrol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper


// Этот интерфейс и его реализация предоставляют уровень абстракции для работы с Bluetooth-адаптером в приложении.
// Вместо того чтобы напрямую взаимодействовать с Android API для работы с Bluetooth,
//  можно использовать этот интерфейс и его реализацию для получения адаптера Bluetooth и контекста.

// Base -  в данном случае это , использование класса Base внутри интерфейса предоставляет реализацию методов по умолчанию,
// что является альтернативой использованию default-методов

interface BluetoothAdapterProvider {

    fun getAdapter(): BluetoothAdapter


    fun getContext(): Context

    fun reloadBluetooth()


    class Base(private val context: Context) : BluetoothAdapterProvider {
        override fun getAdapter(): BluetoothAdapter {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            return manager.adapter
        }


        override fun getContext(): Context {
            return context
        }
        @SuppressLint("MissingPermission")
        override fun reloadBluetooth() {
            // Получите экземпляр BluetoothAdapter
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            // Отключите Bluetooth
            bluetoothAdapter.disable()
            // Включите Bluetooth через короткую задержку после отключения
            Handler(Looper.getMainLooper()).postDelayed({
                bluetoothAdapter.enable()
            }, 1000) // Задержка в миллисекундах (в данном случае 1 секунда)
        }
    }
}