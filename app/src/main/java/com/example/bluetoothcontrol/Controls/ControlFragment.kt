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
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import com.example.bluetoothcontrol.ReadingData.ReadingDataFragment
import com.example.bluetoothcontrol.SharedViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@Suppress("DEPRECATION")
class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding: FragmentControlBinding get() = _binding!!
    private lateinit var controlViewModel: ControlViewModel
    private lateinit var controlModel: BleControlManager
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
            showZipChooser()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel.selectedDeviceAddress.observe(viewLifecycleOwner){deviceAddress ->
            if(deviceAddress != null && !controlModel.isConnected){
                binding.buttonProcessFiles.setOnClickListener{
                    controlViewModel.connect(deviceAddress,"BOOT")
                    Log.e(ReadingDataFragment.TAG," connection to device with address $deviceAddress")
                }
            }else{
                Log.e(ReadingDataFragment.TAG,"address $deviceAddress is null ")
            }
            controlModel.setTimerCallback { stage ->
                if(stage){
                    startProcess()
                }else{
                    finishProcess(stage)
                }
            }
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun startProcess() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun finishProcess(stage : Boolean) {
        if(stage){
            binding.progressBar.visibility = View.GONE
            binding.checkmark.visibility = View.VISIBLE
        }else{
            binding.progressBar.visibility = View.GONE
            binding.errormark.visibility = View.VISIBLE
        }
    }


    companion object {
        const val TAG = "ControlFragment"
        var selectedFilePathBin: String? = null
        var selectedFilePathDat: String? = null
        fun newInstance() = ControlFragment()
    }

    private fun showZipChooser() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/zip"
        startActivityForResult(intent, 101)
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 101 && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            uri?.let { zipUri ->
                try {
                    requireActivity().contentResolver.openInputStream(zipUri)?.use { inputStream ->
                        val zipInputStream = ZipInputStream(inputStream)
                        var entry: ZipEntry?
                        var binFile: Uri? = null
                        var datFile: Uri? = null

                        while (zipInputStream.nextEntry.also { entry = it } != null) {
                            val fileName = entry?.name ?: continue
                            if (fileName.endsWith(".bin")) {
                                binFile = createTempFile(fileName, ".bin").toUri()
                                FileOutputStream(binFile.path).use { outputStream ->
                                    zipInputStream.copyTo(outputStream)
                                }
                            } else if (fileName.endsWith(".dat")) {
                                datFile = createTempFile(fileName, ".dat").toUri()
                                FileOutputStream(datFile.path).use { outputStream ->
                                    zipInputStream.copyTo(outputStream)
                                }
                            }
                        }

                        if (binFile != null && datFile != null) {
                            // Оба файла найдены и их uri передаются в companion object
                            selectedFilePathBin = binFile.toString()
                            selectedFilePathDat = datFile.toString()
                            binding.fileOne.text = "File1: ${binFile.path?.let { File(it).name }}"
                            binding.fileTwo.text = "File2: ${datFile.path?.let { File(it).name }}"
                            binding.fileOne.setTextColor(Color.GREEN)
                            binding.fileTwo.setTextColor(Color.GREEN)
                            buttonProcessFiles.isEnabled = true
                        } else {
                            showToast("Выберите .bin и .dat файлы внутри .zip папки")
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    showToast("Ошибка при обработке .zip файла")
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}

