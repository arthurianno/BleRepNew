package com.example.bluetoothcontrol.ReadingData

import com.example.bluetoothcontrol.Controls.DataType

data class DataItem(var name: String, var hexName:String, val attributeName: String, var isValueChanged: Boolean = false, var address : Int, var lengthOfItem: Int,var type: DataType) {

}