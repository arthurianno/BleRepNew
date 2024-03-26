package com.example.bluetoothcontrol.TerminalDevice

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bluetoothcontrol.MainActivity
import com.example.bluetoothcontrol.databinding.FragmentTerminalDeviceBinding


class TerminalDeviceFragment(): Fragment() {

    private var _binding: FragmentTerminalDeviceBinding? = null
    private val binding: FragmentTerminalDeviceBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminalDeviceBinding.inflate(inflater, container, false)
        return (binding.root)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.showBottomNavigationView()

        super.onViewCreated(view, savedInstanceState)
        
    }

    companion object {
        const val TAG = "AboutDeviceFragment"

        @JvmStatic
        fun newInstance() = TerminalDeviceFragment()
    }


}