package com.example.bluetoothcontrol.Controls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bluetoothcontrol.MainActivity
import com.example.bluetoothcontrol.databinding.FragmentControlBinding

class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding: FragmentControlBinding get() = _binding!!
    private lateinit var controlViewModel: ControlViewModel
    private lateinit var controlModel: BleControlManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControlBinding.inflate(inflater, container, false)
        controlViewModel = (requireActivity() as MainActivity).getControlViewModelFromMain()
        controlModel = (requireActivity() as MainActivity).getControlManagerFromMain()
        (activity as? MainActivity)?.showBottomNavigationView()
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

}