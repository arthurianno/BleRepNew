package com.example.bluetoothcontrol.Controls

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.bluetoothcontrol.Logger
import com.example.bluetoothcontrol.Logs.LogFragment
import com.example.bluetoothcontrol.MainActivity
import com.example.bluetoothcontrol.ReadingData.ReadingDataFragment
import com.example.bluetoothcontrol.SharedViewModel
import com.example.bluetoothcontrol.databinding.FragmentControlBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@Suppress("DEPRECATION")
class ControlFragment : Fragment(),BleControlManager.AcceptedCommandCallback{

    private var _binding: FragmentControlBinding? = null
    private val binding: FragmentControlBinding get() = _binding!!
    private lateinit var controlViewModel: ControlViewModel
    private lateinit var controlModel: BleControlManager
    private lateinit var buttonProcessFiles: Button
    private val sharedViewModel: SharedViewModel by lazy {
        (requireActivity() as MainActivity).getSharedViewModelFromMain()
    }
    private var timer: CountDownTimer? = null
    private var progressBarSize = 379

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
        controlModel.setChunkSize(128) // изначально меняем на 128
        binding.progressBarHor.max = (48600.0/128).toInt()
        sharedViewModel.timerActiveFragment.value = false
        progressBarSize = (48600.0/128).toInt()
        sharedViewModel.timerFragmentActive.value = false
        Logger.d(LogFragment.TAG, "from CONTROLFRAGMENT changed data  " +  sharedViewModel.timerFragmentActive.value)
        sharedViewModel.selectedDeviceAddress.observe(viewLifecycleOwner) { deviceAddress ->
            if (deviceAddress != null && !controlModel.isConnected) {
                binding.buttonProcessFiles.setOnClickListener {
                    if (selectedFilePathBin != null && selectedFilePathDat != null) {
                        BleControlManager.requestData.value?.clear()
                        controlViewModel.connect(deviceAddress, "BOOT")
                        Log.e(
                            ReadingDataFragment.TAG,
                            "connection to device with address $deviceAddress"
                        )
                    } else {
                        showToast("Выберите .bin и .dat файлы внутри .zip папки")
                    }
                }
            } else {
                Log.e(ReadingDataFragment.TAG, "address $deviceAddress is null ")
            }
            binding.editTextNumber.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val input = binding.editTextNumber.text.toString()
                    val size = input.toIntOrNull()
                    sizeOfChunk = if (size in 32..240) {
                        size?.let { controlModel.setChunkSize(it) }
                        showToast("Размер пакета установлен!")
                        binding.progressBarHor.max = (48600.0 / input.toInt()).toInt()
                        progressBarSize = (48600.0 / input.toInt()).toInt()
                        size // Возвращаем корректное значение
                    } else {
                        showToast("Размер пакета должен быть в диапазоне от 32 до 240")
                        controlModel.setChunkSize(128)
                        null // Сбрасываем значение sizeOfChunk
                    }
                    true // Возвращаем true, чтобы указать, что обработка события завершена
                } else {
                    false // Возвращаем false для обработки других действий
                }
            }
        }
        controlModel.setTimerCallback(object : BleControlManager.TimerCallback{
            override fun onTickSucces(stage: Int) {
                binding.progressBarHor.progress += 1
                if(binding.progressBarHor.progress == progressBarSize){
                    showToast("ЗАПИСЬ ФАЙЛОВ И КОНФИГУРАЦИИ УСПЕШНА!")
                    binding.progressBarHor.progress = 0
                    selectedFilePathBin = null
                    selectedFilePathDat = null
                    binding.fileOne.text = null
                    binding.fileTwo.text = null
                }
            }

            override fun onTickFailed(stage: Int) {
                showToast("Ошибка при ЗАПИСИ/КОНФИГУРАЦИИ")
                binding.progressBarHor.progress = 0
                selectedFilePathBin = null
                selectedFilePathDat = null
                binding.fileOne.text = null
                binding.fileTwo.text = null
                controlModel.isConnected.let {
                    if (it) {
                        controlModel.disconnect()
                    }
                }
            }

        })
       controlViewModel.setDisconnectionCallBack(object : ControlViewModel.DisconnectionCallback{
           override fun onDeviceDisconnected() {
               showToast("Устройство отключено, попробуйте снова")
               binding.progressBarHor.progress = 0
               selectedFilePathBin = null
               selectedFilePathDat = null
               binding.fileOne.text = null
               binding.fileTwo.text = null

           }

       })
        controlViewModel.setConnectionCallback(object : ControlViewModel.ConnectionCallback{
            override fun onDeviceFailedToConnect() {
                showToast("Проблема с подключением повторите команду!")
            }
        })
        controlViewModel.setConnectingCallBack(object : ControlViewModel.ConnectingCallback{
            override fun onDeviceConnecting() {
                showToast("Идет подключение к устройству")
            }
        })
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(!hidden){
            sharedViewModel.timerFragmentActive.value = false
            Logger.d(LogFragment.TAG, "from CONTROLFRAGMENT changed data  " +  sharedViewModel.timerFragmentActive.value)
        }
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        controlViewModel.removeConnectionCallback()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(!hidden){
            sharedViewModel.timerActiveFragment.value = false
        }
    }

    companion object {
        const val TAG = "ControlFragment"
        var selectedFilePathBin: String? = null
        var selectedFilePathDat: String? = null
        var sizeOfChunk: Int? = null
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
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            requireActivity().contentResolver.openInputStream(zipUri)
                                ?.use { inputStream ->
                                    val zipInputStream = ZipInputStream(inputStream)
                                    var entry: ZipEntry?
                                    var binFile: Uri? = null
                                    var datFile: Uri? = null

                                    while (zipInputStream.nextEntry.also { entry = it } != null) {
                                        val fileName = entry?.name ?: continue
                                        if (fileName.toLowerCase(Locale.ROOT).endsWith(".bin")) {
                                            binFile = createTempFile(fileName, ".bin").toUri()
                                            FileOutputStream(binFile.path).use { outputStream ->
                                                zipInputStream.copyTo(outputStream)
                                            }
                                        } else if (fileName.toLowerCase(Locale.ROOT).endsWith(".dat")) {
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
                                        withContext(Dispatchers.Main) {
                                            binding.fileOne.text =
                                                "File1: ${binFile.path?.let { File(it).name }}"
                                            binding.fileTwo.text =
                                                "File2: ${datFile.path?.let { File(it).name }}"
                                            binding.fileOne.setTextColor(Color.GREEN)
                                            binding.fileTwo.setTextColor(Color.GREEN)
                                            buttonProcessFiles.isEnabled = true
                                        }
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
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    override fun onAcc(acc: Boolean) {
    }



}

