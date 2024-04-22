package com.example.bluetoothcontrol.TerminalDevice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        super.onViewCreated(view, savedInstanceState)
        
    }

    companion object {
        const val TAG = "AboutDeviceFragment"

        @JvmStatic
        fun newInstance() = TerminalDeviceFragment()
    }


}