package com.example.bluetoothcontrol.Controls

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bluetoothcontrol.MainActivity
import com.example.bluetoothcontrol.databinding.FragmentControlBinding
import android.net.Uri
import android.util.Log
import android.widget.Button
import androidx.fragment.app.activityViewModels
import com.example.bluetoothcontrol.ReadingData.ReadingDataFragment
import com.example.bluetoothcontrol.SharedViewModel
import java.io.File

class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding: FragmentControlBinding get() = _binding!!
    private lateinit var controlViewModel: ControlViewModel
    private lateinit var controlModel: BleControlManager
    private var isFirstFileSelected = false
    private lateinit var buttonProcessFiles: Button
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControlBinding.inflate(inflater, container, false)
        controlViewModel = (requireActivity() as MainActivity).getControlViewModelFromMain()
        controlModel = (requireActivity() as MainActivity).getControlManagerFromMain()
        (activity as? MainActivity)?.showBottomNavigationView()
        buttonProcessFiles = binding.buttonProcessFiles
        binding.button.setOnClickListener {
            showFileChooser()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel.selectedDeviceAddress.observe(viewLifecycleOwner){deviceAddress ->
            if(deviceAddress != null && !controlModel.isConnected){
                binding.buttonProcessFiles.setOnClickListener{
                    controlViewModel.connect(deviceAddress)
                    Log.e(ReadingDataFragment.TAG,"Toggle connection to device with address $deviceAddress")
                }
            }else{
                Log.e(ReadingDataFragment.TAG,"address $deviceAddress is null ")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val TAG = "ControlFragment"
        var selectedFilePathBin: String? = null
        var selectedFilePathDat: String? = null
        fun newInstance() = ControlFragment()
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(Intent.createChooser(intent, "Select File"), 100)
        } catch (ex: Exception) {
            showToast("Please install a file manager")
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            val uri: Uri? = data.data
            val path: String = uri?.path ?: ""
            val file = File(path)
            Log.d(TAG, "File path: $path")
            val fileName = file.name

            val fileException = fileName.substring(fileName.lastIndexOf("."))
            if (!isFirstFileSelected) {
                if(fileException.equals(".bin", ignoreCase = true)) {
                    selectedFilePathBin = uri.toString()
                    binding.fileTwo.text = "File2: $fileName"
                    binding.fileTwo.setTextColor(Color.GREEN)
                    isFirstFileSelected = true
                }else{
                    showToast("Выберите файл формата .bin")
                }
            } else {
                if(fileException.equals(".dat", ignoreCase = true)) {
                    selectedFilePathDat = uri.toString()
                    binding.fileOne.text = "File1: $fileName"
                    binding.fileOne.setTextColor(Color.GREEN)
                    isFirstFileSelected = true
                }
            else {
                showToast("Выберите файл формата .dat")
                }
            }

            // Проверяем, что оба файла выбраны
            if (!selectedFilePathBin.isNullOrEmpty() && !selectedFilePathDat.isNullOrEmpty()) {
                buttonProcessFiles.isEnabled = true // Включаем кнопку, если оба файла выбраны
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

