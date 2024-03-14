package com.example.bluetoothcontrol

import android.app.Application
import android.view.View

// Объект App создается один раз во время запуска приложения и может быть использован
// для предоставления глобального доступа к ресурсам или сервисам,
// которые должны быть доступными в любой части приложения

// Предоставление объекта BluetoothAdapterProvider в App может быть полезным, если вы хотите,
// чтобы ваши компоненты приложения (например, активити, фрагменты и другие классы)
// имели удобный доступ к адаптеру Bluetooth и контексту.

class App : Application() {

    val adapterProvider: BluetoothAdapterProvider by lazy {
        BluetoothAdapterProvider.Base(applicationContext)
    }

}