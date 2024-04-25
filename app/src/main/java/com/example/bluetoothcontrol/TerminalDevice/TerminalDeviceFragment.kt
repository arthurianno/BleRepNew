package com.example.bluetoothcontrol.TerminalDevice

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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


class TerminalDeviceFragment(): Fragment() {

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
        val spinnerItems = listOf("","TIME", "VERSION", "BATTERY", "SERIAL", "MAC","FIND","ERASE","RD")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, spinnerItems)
        spinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = spinnerAdapter
        binding.spinner.setSelection(0, false)

        binding.inputSwitch.setOnCheckedChangeListener{ _, isChecked ->
            if(isChecked){
                binding.spinner.visibility = View.VISIBLE

            }else{
                binding.spinner.visibility = View.INVISIBLE
            }
        }
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = spinnerItems[position]
                if (selectedItem.isNotEmpty()) {
                    when(selectedItem){
                        "TIME" -> connectToDevice(selectedItem)
                        "VERSION" -> connectToDevice(selectedItem)
                        "BATTERY" -> connectToDevice(selectedItem)
                        "MAC" -> connectToDevice(selectedItem)
                        "SERIAL" -> connectToDevice(selectedItem)
                    }
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        super.onViewCreated(view, savedInstanceState)

    }

    private fun connectToDevice(selectedItem : String){
        val deviceAddress = sharedViewModel.selectedDeviceAddress.value
        if(deviceAddress != null){
            when (bleControlManager.isConnected) {
                false -> {
                    controlViewModel.connect(deviceAddress,"TERMINALspinner")
                    bleControlManager.setPinCallback {
                        if(it.equals("CORRECT")){
                            ControlViewModel.readTerminalCommandSpinner(selectedItem)
                            Logger.d(TAG," connected and Sending terminal command spinner type $selectedItem")
                        }
                    }
                }
                true -> {
                    ControlViewModel.readTerminalCommandSpinner(selectedItem)
                    Logger.d(TAG,"Sending terminal command spinner type $selectedItem")
                }
                else -> {
                    Toast.makeText(requireContext(),"Ошибка отправки",Toast.LENGTH_SHORT).show()
                }
            }
        }else{
            Toast.makeText(requireContext(),"Устройство не подключено",Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "AboutDeviceFragment"

        @JvmStatic
        fun newInstance() = TerminalDeviceFragment()
    }


}