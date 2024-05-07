package com.example.bluetoothcontrol.Logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothcontrol.Logger
import com.example.bluetoothcontrol.MainActivity
import com.example.bluetoothcontrol.SharedViewModel
import com.example.bluetoothcontrol.databinding.FragmentLogBinding


class LogFragment : Fragment(){
    private var _binding: FragmentLogBinding? = null
    val binding: FragmentLogBinding get() = _binding!!
    private val logAdapter = LogAdapter()
    private val sharedViewModel: SharedViewModel by lazy {
        (requireActivity() as MainActivity).getSharedViewModelFromMain()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLogBinding.inflate(inflater,container,false)
        return (binding.root)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.showBottomNavigationView()
        sharedViewModel.timerActiveFragment.value = true
        Logger.d(TAG, "changed timerState" + sharedViewModel.timerActiveFragment.value)
        binding.logRecView.apply {
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
            layoutManager = LinearLayoutManager(requireContext())
            adapter = logAdapter
        }
        Logger._items.observe(viewLifecycleOwner){ data ->
            logAdapter.updateLog(data)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(!hidden){
            sharedViewModel.timerActiveFragment.value = true
        }
    }


    companion object {
        const val TAG = "LogFragment"
        @JvmStatic
        fun newInstance() = LogFragment()
    }
    }