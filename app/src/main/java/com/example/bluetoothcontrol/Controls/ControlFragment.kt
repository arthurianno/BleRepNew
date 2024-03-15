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
import android.widget.Button
import java.io.File

class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding: FragmentControlBinding get() = _binding!!
    private lateinit var controlViewModel: ControlViewModel
    private lateinit var controlModel: BleControlManager
    private var isFirstFileSelected = false
    private lateinit var buttonProcessFiles: Button

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
        binding.buttonProcessFiles.setOnClickListener {

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
            val path: String = uri?.path.toString()
            val file = File(path)
            val fileName = file.name

            if (isFirstFileSelected) {
                selectedFilePathBin = path
                binding.fileTwo.text = "File2: $fileName"
                binding.fileTwo.setTextColor(Color.GREEN)
            } else {
                selectedFilePathDat = path
                binding.fileOne.text = "File1: $fileName"
                binding.fileOne.setTextColor(Color.GREEN)
                isFirstFileSelected = true
            }

            // Проверяем, что оба файла выбраны
            if (!selectedFilePathBin.isNullOrEmpty() && !selectedFilePathDat.isNullOrEmpty()) {
                buttonProcessFiles.isEnabled = true // Включаем кнопку, если оба файла выбраны
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

