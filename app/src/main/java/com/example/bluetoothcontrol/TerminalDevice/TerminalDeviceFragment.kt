package com.example.bluetoothcontrol.TerminalDevice

//noinspection SuspiciousImport
import android.R
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.Controls.BleControlManager
import com.example.bluetoothcontrol.Controls.ControlViewModel
import com.example.bluetoothcontrol.Logger
import com.example.bluetoothcontrol.MainActivity
import com.example.bluetoothcontrol.ReadingData.ReadingDataFragment
import com.example.bluetoothcontrol.SharedViewModel
import com.example.bluetoothcontrol.databinding.FragmentTerminalDeviceBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone


class TerminalDeviceFragment(): Fragment(){

    private var _binding: FragmentTerminalDeviceBinding? = null
    private val binding: FragmentTerminalDeviceBinding get() = _binding!!
    private val terminalAdapter = TerminalAdapter()
    private lateinit var bleControlManager: BleControlManager
    private lateinit var controlViewModel: ControlViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminalDeviceBinding.inflate(inflater, container, false)
        controlViewModel = (requireActivity() as MainActivity).getControlViewModelFromMain()
        bleControlManager = MainActivity.controlManager
        BleControlManager.requestDataTermItem.observe(viewLifecycleOwner) { data ->
            terminalAdapter.update(data)
        }
        binding.spinner.visibility = View.INVISIBLE
        return (binding.root)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.showBottomNavigationView()
        binding.recyclerView.apply {
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
            layoutManager = LinearLayoutManager(requireContext())
            adapter = terminalAdapter
        }
        controlViewModel.setConnectionCallback(object : ControlViewModel.ConnectionCallback{
            override fun onDeviceFailedToConnect() {
                showToast("Устройство отключено!")
            }

        })
        controlViewModel.setDisconnectionCallBack(object : ControlViewModel.DisconnectionCallback{
            override fun onDeviceDisconnected() {
                showToast("Устройство отключено!")
            }

        })

        sharedViewModel.selectedDeviceAddress.observe(viewLifecycleOwner){deviceAddress ->
            if(deviceAddress != null){
                binding.terminalWrite.setOnClickListener{
                    BleControlManager.requestDataTermItem.value?.clear()
                    controlViewModel.connect(deviceAddress,"TERMINAL")
                    Logger.e(ReadingDataFragment.TAG,"Toggle connection to device with address $deviceAddress")
                }
            }else{
                Logger.e(ReadingDataFragment.TAG,"address $deviceAddress is null ")
            }
        }
        val spinnerItems = listOf("Режим команды не выбран","TIME", "VERSION", "BATTERY", "SERIAL", "MAC","FIND","ERASE","RD","SETTIME")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, spinnerItems)
        spinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = spinnerAdapter
        binding.spinner.setSelection(0, false)

        binding.inputSwitch.setOnCheckedChangeListener{ _, isChecked ->
            if(isChecked){
                binding.spinner.visibility = View.VISIBLE

            }else{
                if(controlViewModel.isConnected.value == true){
                    controlViewModel.disconnect()
                    if(terminalAdapter.itemCount > 0){
                        terminalAdapter.clear()
                    }
                }
                binding.spinner.visibility = View.INVISIBLE
            }
        }
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = spinnerItems[position]
                if (selectedItem.isNotEmpty()) {
                    when(selectedItem){
                        "TIME" -> connectToDevice(selectedItem,"")
                        "VERSION" -> connectToDevice(selectedItem,"")
                        "BATTERY" -> connectToDevice(selectedItem,"")
                        "MAC" -> connectToDevice(selectedItem,"")
                        "SERIAL" -> connectToDevice(selectedItem,"")
                        "ERASE" -> connectToDevice(selectedItem,"")
                        "FIND" -> connectToDevice(selectedItem,"")
                    }
                    if(selectedItem == "RD"){
                        showInputDialog(selectedItem,"RD")
                    }else if (selectedItem == "SETTIME"){
                        showInputDialog(selectedItem,"SETTIME")
                    }
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        super.onViewCreated(view, savedInstanceState)

    }
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun connectToDevice(selectedItem : String,size:String){
        val deviceAddress = sharedViewModel.selectedDeviceAddress.value
        if(deviceAddress != null){
            when (bleControlManager.isConnected) {
                false -> {
                    BleControlManager.requestDataTermItem.value?.clear()
                    controlViewModel.connect(deviceAddress,"TERMINALspinner")
                    bleControlManager.setPinCallback {
                        if(it.equals("CORRECT")){
                            if(terminalAdapter.itemCount > 0){
                                terminalAdapter.clear()
                            }
                            ControlViewModel.readTerminalCommandSpinner(selectedItem,size)
                            Logger.d(TAG," connected and Sending terminal command spinner type $selectedItem")
                        }
                    }
                }
                true -> {

                    ControlViewModel.readTerminalCommandSpinner(selectedItem,size)
                    Logger.d(TAG,"Sending terminal command spinner type $selectedItem")
                }

            }
            binding.spinner.setSelection(0, false)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun showInputDialog(selectedItem: String, commandType: String) {
        binding.spinner.setSelection(0, false)
        val builder = AlertDialog.Builder(requireContext())
        val title = if (commandType == "SETTIME") {
            "Время будет установлено автоматически"
        } else {
            "Введите номер для считывания измерений из буфера от 000 до 999"
        }
        builder.setTitle(title)

        if (commandType != "SETTIME") {
            val input = EditText(requireContext())
            builder.setView(input)
            builder.setPositiveButton("OK") { _, _ ->
                val inputValue = input.text.toString()
                if (commandType == "RD" && isValidBufferNumber(inputValue)) {
                    connectToDevice(selectedItem, inputValue)
                } else {
                    showToast("Данные неверные!")
                }
            }
        } else {
            builder.setPositiveButton("OK") { _, _ ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
                calendar.timeInMillis = System.currentTimeMillis()
                // Отнимаем 3 часа
                calendar.add(Calendar.HOUR_OF_DAY, -3)
                val dateFormat = SimpleDateFormat("yyMMddHHmmss")
                val formattedTime = dateFormat.format(calendar.time)
                connectToDevice(selectedItem, formattedTime)
            }
        }
        // Устанавливаем кнопку "Отмена"
        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel() // Закрываем диалог без сохранения введенных данных
        }

        builder.show()
    }


    private fun isValidTime(time: String): Boolean {
        return time.length == 14 && time.matches("[0-9]+".toRegex())
    }

    private fun isValidBufferNumber(number: String): Boolean {
        return number.length in 1..3 && number.matches("[0-9]+".toRegex())
    }



    companion object {
        const val TAG = "AboutDeviceFragment"

        @JvmStatic
        fun newInstance() = TerminalDeviceFragment()
    }




}