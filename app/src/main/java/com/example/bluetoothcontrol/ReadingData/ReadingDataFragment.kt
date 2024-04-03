package com.example.bluetoothcontrol.ReadingData

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.BluetoothAdapterProvider
import com.example.bluetoothcontrol.Controls.BleControlManager
import com.example.bluetoothcontrol.Controls.ControlViewModel
import com.example.bluetoothcontrol.Controls.DataType
import com.example.bluetoothcontrol.Controls.EntireCheck
import com.example.bluetoothcontrol.MainActivity
import com.example.bluetoothcontrol.SharedViewModel
import com.example.bluetoothcontrol.databinding.FragmentReadingDataBinding
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import kotlin.math.min


class ReadingDataFragment : Fragment(),ReadingDataAdapter.CallBackOnReadingItem,BleControlManager.AcceptedCommandCallback {

    private var _binding: FragmentReadingDataBinding? = null
    private val binding: FragmentReadingDataBinding get() = _binding!!
    private val adapterReading = ReadingDataAdapter()
    lateinit var adapter : BluetoothAdapterProvider
    private lateinit var bleControlManager: BleControlManager
    private lateinit var controlViewModel: ControlViewModel
    private var checkItem = false
    private var dialogAgree = false
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReadingDataBinding.inflate(inflater, container, false)
        BleControlManager.requestData.observe(viewLifecycleOwner) { data ->
            Log.d(TAG,"recView UPDATE")
            adapterReading.update(data)
        }
        controlViewModel = (requireActivity() as MainActivity).getControlViewModelFromMain()
        bleControlManager = MainActivity.controlManager
        controlViewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            Log.d("ReadingDataFragment", "Is Connected: $isConnected")
        }
        return (binding.root)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.showBottomNavigationView()
        binding.dataRecView.apply {
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapterReading
        }
       sharedViewModel.selectedDeviceAddress.observe(viewLifecycleOwner){deviceAddress ->
           if(deviceAddress != null){
               binding.buttonRDData.setOnClickListener{
                   controlViewModel.connect(deviceAddress,"RAW")
                   Log.e(TAG,"Toggle connection to device with address $deviceAddress")
               }
           }else{
               Log.e(TAG,"address $deviceAddress is null ")
           }
       }
        adapterReading.addCallBack(this)
        bleControlManager.setAcceptedCommandCallback(this)
        binding.buttonWRData.setOnClickListener {
            if(controlViewModel.isConnected.value == true){
                showBluetoothReloadNotification()
            }else{
                Toast.makeText(requireContext(),"Connection lost",Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val TAG = "RDataFragment"
        private const val KEY_DEVICE_ADDRESS = "key_device_address"
        @JvmStatic
        fun newInstance(deviceAddress: String?) =
            ReadingDataFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_DEVICE_ADDRESS,deviceAddress)
                }
            }
    }

    override fun onItemClickReadingData(item: DataItem) {
        Toast.makeText(requireContext(), "You clicked on item", Toast.LENGTH_SHORT).show()
        val position = adapterReading.items.indexOf(item) // Получаем позицию элемента в списке
        showInputDialog(item, position)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showInputDialog(dataItem: DataItem, position: Int) {
        val oldValue = dataItem.name
        val input = EditText(requireContext())
        input.setText(dataItem.name)

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Изменение параметра")
            .setMessage("Старые данные: $oldValue")
            .setView(input)
            .setPositiveButton("Ок") { dialog, _ ->
                val newValue = input.text.toString()
                if (oldValue != newValue) {
                    dataItem.name = newValue
                    adapterReading.updateAttributeColor(position, true)
                    checkItem = true
                }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showWriteDialog() {
        adapter = BluetoothAdapterProvider.Base(requireContext())
        val changedItemsCount = adapterReading.items.count { it.isValueChanged }
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Запись данных")
            .setMessage("Вы уверены, что хотите записать данные? Количество измененных элементов: $changedItemsCount")
            .setPositiveButton("Записать") { dialog, _ ->
                // Логика для записи данных
                if (changedItemsCount > 0 && bleControlManager.isConnected && checkItem) {
                    if(dialogAgree){
                        val changedDataItems = adapterReading.items.filter { it.isValueChanged }
                        changedDataItems.forEach { dataItem ->
                            // Подготовка данных для отправки
                            val newData = prepareData(dataItem)
                            bleControlManager.writeData(newData, EntireCheck.WRITE, dataItem)
                        }
                        Toast.makeText(requireContext(), "Данные успешно записаны", Toast.LENGTH_SHORT).show()
                        // Очистка списка данных и обновление RecyclerView
                        adapterReading.items.clear()
                        BleControlManager.requestData.value?.clear()
                        adapterReading.notifyDataSetChanged()
                        Log.d(TAG,"recView CLEAR")
                        dialog.dismiss()
                    }else{
                        Toast.makeText(requireContext(), "Вы отказались от перезагрузки Bluetooth!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Данные не записаны, вы не поменяли данные", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showBluetoothReloadNotification() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Перезагрузка Bluetooth")
            .setMessage("Bluetooth будет перезагружен после записи данных")
            .setPositiveButton("Продолжить") { dialog, _ ->
                // Вызовите метод для перезагрузки Bluetooth
                dialogAgree = true
                showWriteDialog()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }
    private fun prepareData(dataItem: DataItem): ByteArray {
        return when (dataItem.type) {
            DataType.FLOAT -> {
                val value = dataItem.name.toFloatOrNull() ?: 0.0f
                val byteBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                byteBuffer.putFloat(value)
                val newData = byteBuffer.array()
                dataItem.isValueChanged = false
                newData
            }
            DataType.UINT32 -> {
                val value = dataItem.name.toIntOrNull() ?: 0
                val byteBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                byteBuffer.putInt(value)
                val newData = byteBuffer.array()
                dataItem.isValueChanged = false
                newData
            }
            DataType.CHAR_ARRAY -> {
                val maxLength = 16
                val valueBytes = dataItem.name.toByteArray(Charsets.UTF_8)
                val newData = ByteArray(maxLength)
                val lengthOfData = min(valueBytes.size, maxLength)
                System.arraycopy(valueBytes, 0, newData, 0, lengthOfData)
                if (lengthOfData < maxLength) {
                    Arrays.fill(newData, lengthOfData, maxLength, 0.toByte())
                }
                dataItem.isValueChanged = false
                newData
            }
        }
    }

    override fun onAcc(acc: Boolean) {
        if(acc){
            adapter.reloadBluetooth()
        }else{
            Toast.makeText(requireContext(), "Ошибка загрузки данных!", Toast.LENGTH_SHORT).show()
        }
    }

}



