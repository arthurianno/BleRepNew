package com.example.bluetoothcontrol.Devices

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.App
import com.example.bluetoothcontrol.Controls.BleControlManager
import com.example.bluetoothcontrol.Controls.ControlViewModel
import com.example.bluetoothcontrol.MainActivity
import com.example.bluetoothcontrol.R
import com.example.bluetoothcontrol.ReadingData.ReadingDataFragment
import com.example.bluetoothcontrol.SharedViewModel
import com.example.bluetoothcontrol.databinding.FragmentDevicesBinding

@Suppress("DEPRECATION")
class DevicesFragment : Fragment(), DevicesAdapter.CallBack {

    private var _binding: FragmentDevicesBinding? = null
    private val binding: FragmentDevicesBinding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var controlViewModel: ControlViewModel
    private lateinit var bleControlManager: BleControlManager
    private val viewModel: DevicesViewModel by viewModels {
        DeviceViewModelFactory((requireActivity().application as App).adapterProvider)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        devicesAdapter = DevicesAdapter(this@DevicesFragment, sharedViewModel)
        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        (activity as? MainActivity)?.hideBottomNavigationView()
        return binding.root
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        controlViewModel = (requireActivity() as MainActivity).getControlViewModelFromMain()
        bleControlManager = (requireActivity() as MainActivity).getControlManagerFromMain()
    }

    // Ссылка при уничтожении не остается на старый View
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopScan()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.devicesRecycler.apply {
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
            layoutManager = LinearLayoutManager(requireContext())
            adapter = devicesAdapter
        }
        devicesAdapter.addCallBack(this)

        binding.wrapStartScan.setOnClickListener {
            checkLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        binding.ClearRecycler.setOnClickListener {
            if (binding.devicesRecycler != null) {
                viewModel.stopScan()
                devicesAdapter.clear()
                viewModel.clearData()
                viewModel.clearScanCache()

            }
        }

    }

    override fun onStart() {
        super.onStart()
        subscribeOnViewModel()
    }

    private fun subscribeOnViewModel() {
        viewModel.devices.observe(viewLifecycleOwner) { devices ->
            if (devices != null) {
                devicesAdapter.update(devices)
            }
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    @SuppressLint("MissingPermission")
    override fun onItemClick(device: BluetoothDevice) {
        val deviceAddress = device.address
        val deviceName = device.name.toString()
        if (deviceAddress != null) {
            sharedViewModel.updateDeviceAddress(deviceAddress)
            sharedViewModel.updateDevName(deviceName)
            Log.e("DevicesFragment", "Update address $deviceAddress")
            Log.e("DevicesFragment", "Update name $deviceName")
            showPinInputDialogOrConnect(deviceAddress)
        }
    }

     private fun showPinInputDialog(deviceAddress: String) {
        val editTextPin = EditText(requireContext())
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
            .setTitle("Введите ПИН-КОД")
            .setView(editTextPin)
            .setPositiveButton("OK") { dialog, _ ->
                val pinCode = editTextPin.text.toString()
                controlViewModel.savePinCodeForDevice(deviceAddress,pinCode)
                dialog.dismiss()
                val existingReadingDataFragment = parentFragmentManager.findFragmentByTag(ReadingDataFragment.TAG) as? ReadingDataFragment
                if (existingReadingDataFragment != null && controlViewModel.isConnected.value != false) {
                    parentFragmentManager.beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.fragmentContainer, existingReadingDataFragment)
                        .commit()
                } else {
                    val readingDataFragment = ReadingDataFragment.newInstance(deviceAddress)
                    parentFragmentManager.beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.fragmentContainer, readingDataFragment, ReadingDataFragment.TAG)
                        .commit()
                }
            }

        alertDialogBuilder.create().show()
    }

    private fun showPinInputDialogOrConnect(deviceAddress: String) {
        val savedPinCode = controlViewModel.getSavedPinCodeForDevice(deviceAddress)
        Log.d("ControlViewModel", "Getting saved PIN-Code for device: $deviceAddress, PIN: $savedPinCode")
        if (savedPinCode != null) {
            // Пин-код найден в SharedPreferences, используем его для установки соединения
            controlViewModel.pinCode = savedPinCode
            controlViewModel.connect(deviceAddress,"CHECKPIN")
            bleControlManager.setPinCallback {
                if(it == "CORRECT"){
                    controlViewModel.disconnect()
                    val existingReadingDataFragment = parentFragmentManager.findFragmentByTag(ReadingDataFragment.TAG) as? ReadingDataFragment
                    if (existingReadingDataFragment != null && controlViewModel.isConnected.value != false) {
                        parentFragmentManager.beginTransaction()
                            .addToBackStack(null)
                            .replace(R.id.fragmentContainer, existingReadingDataFragment)
                            .commit()
                    } else {
                        val readingDataFragment = ReadingDataFragment.newInstance(deviceAddress)
                        parentFragmentManager.beginTransaction()
                            .addToBackStack(null)
                            .replace(R.id.fragmentContainer, readingDataFragment, ReadingDataFragment.TAG)
                            .commit()
                    }
                }else{
                    controlViewModel.disconnect()
                    showPinInputDialog(deviceAddress)
                }
            }
        } else {
            // Пин-код не найден в SharedPreferences, отображаем диалог для ввода пин-кода
            showPinInputDialog(deviceAddress)
        }
    }

    private val checkLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()){granted ->
        if(granted){
            viewModel.startScan()
        }
    }
    companion object {
        const val TAG = "DeviceFragment"
        fun newInstance() = DevicesFragment()
    }
}

