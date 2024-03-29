package com.example.bluetoothcontrol
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.bluetoothcontrol.Devices.DevicesAdapter
import com.example.bluetoothcontrol.Devices.DevicesFragment
import com.example.bluetoothcontrol.ReadingData.ReadingDataFragment
import com.example.bluetoothcontrol.TerminalDevice.TerminalDeviceFragment
import com.example.bluetoothcontrol.Controls.BleControlManager
import com.example.bluetoothcontrol.Controls.ControlFragment
import com.example.bluetoothcontrol.Controls.ControlViewModel
import com.example.bluetoothcontrol.Controls.ControlViewModelFactory
import com.example.bluetoothcontrol.Logs.LogFragment
import com.example.bluetoothcontrol.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(),DevicesAdapter.CallBack {

    lateinit var binding: ActivityMainBinding
    lateinit var adapter : BluetoothAdapterProvider
    private var devAddress: String? = null
    private val sharedViewModel: SharedViewModel by viewModels()
    private val controlViewModel: ControlViewModel by viewModels {
        ControlViewModelFactory((application as App).adapterProvider, this)
    }

    companion object{
         lateinit var controlManager: BleControlManager
    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableBluetooth()
        navigate(DevicesFragment.newInstance(), "DeviceFragment",R.id.fragmentContainer)
        hideBottomNavigationView()
        controlManager = BleControlManager(this)
        sharedViewModel.selectedDeviceAddress.observe(this, Observer { deviceAddress ->
            devAddress = deviceAddress
        })
        binding.bottomNavBar.setOnNavigationItemSelectedListener { menuItem ->
            handleBottomNavigationItemSelection(menuItem)
            true
        }
        sharedViewModel.devName.observe(this){ name ->
            binding.deviceName.text = name.toString()
        }
        controlViewModel.isConnected.observe(this){
            if(it == true){
                binding.connectionStatusImageView.setImageResource(R.drawable.baseline_circle_green)
            }else{
                binding.connectionStatusImageView.setImageResource(R.drawable.baseline_circle_red)
            }
        }

    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Получаем текущий фрагмент
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        // Проверяем, является ли текущий фрагмент фрагментом устройств
        val isDeviceFragment = currentFragment is DevicesFragment

        // Если текущий фрагмент не является фрагментом устройств, переходим к фрагменту устройств
        if (!isDeviceFragment) {
            navigate(DevicesFragment.newInstance(), "DeviceFragment", R.id.fragmentContainer)
        } else {
            // Если текущий фрагмент уже является фрагментом устройств, закрываем активность
            super.onBackPressed()
        }
    }

    fun getControlViewModelFromMain(): ControlViewModel {
        return controlViewModel
    }
    fun getControlManagerFromMain(): BleControlManager{
        return controlManager
    }
    private var currentFragmentTag: String? = null

    private fun handleBottomNavigationItemSelection(menuItem: MenuItem) {
        val fragmentTag = when (menuItem.itemId) {
            R.id.readingDataFragment -> ReadingDataFragment.TAG
            R.id.writingDataFragment -> TerminalDeviceFragment.TAG
            R.id.logFragment -> LogFragment.TAG
            R.id.action_boot -> ControlFragment.TAG
            else -> null
        }
        if (fragmentTag != null && fragmentTag != currentFragmentTag) {
            val transaction = supportFragmentManager.beginTransaction()
            // Скрываем все существующие фрагменты
            supportFragmentManager.fragments.forEach { fragment ->
                transaction.hide(fragment)
            }

            // Показываем или добавляем фрагмент
            val fragment = supportFragmentManager.findFragmentByTag(fragmentTag) ?: createFragmentByTag(fragmentTag)
            fragment.let {
                if (!it.isAdded) {
                    transaction.add(R.id.fragmentContainer, it, fragmentTag)
                } else {
                    transaction.show(it)
                }
            }
            // Применяем транзакцию без добавления в стек обратного вызова
            transaction.commit()

            currentFragmentTag = fragmentTag // Обновляем currentFragmentTag только если произошел фактический переход
        }
    }
    private fun createFragmentByTag(tag: String): Fragment {
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)
        return if (existingFragment != null) {
            existingFragment
        } else {
            when (tag) {
                ReadingDataFragment.TAG -> ReadingDataFragment.newInstance(devAddress)
                TerminalDeviceFragment.TAG -> TerminalDeviceFragment.newInstance()
                LogFragment.TAG -> LogFragment.newInstance()
                ControlFragment.TAG -> ControlFragment.newInstance()
                else -> throw IllegalArgumentException("Unknown fragment tag: $tag")
            }
        }
    }

    private fun navigate(fragment: Fragment, tag: String, fragmentId: Int) {
        supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(fragmentId, fragment, tag)
            .commit()

        currentFragmentTag = tag
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED == intent?.action) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                handleBluetoothState(state)
            }
        }
    }



    override fun onResume() {
        super.onResume()
        registerBluetoothStateReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterBluetoothStateReceiver()
    }

    // убирает системный бар
//    override fun onWindowFocusChanged(hasFocus: Boolean) {
//        super.onWindowFocusChanged(hasFocus)
//        if(hasFocus){
//            window.decorView.systemUiVisibility = (
//                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                            or View.SYSTEM_UI_FLAG_FULLSCREEN
//                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
//        }
//    }


    private fun enableBluetooth(){
        adapter = BluetoothAdapterProvider.Base(applicationContext)
        if(!adapter.getAdapter().isEnabled){
            showEnableBluetoothMessage()
        }
        requestEnableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    private val requestEnableBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()
    ){result ->
    if (result.resultCode == RESULT_CANCELED){
        showEnableBluetoothMessage()
    }
    }

    private fun showEnableBluetoothMessage(){
        AlertDialog.Builder(this)
            .setTitle("Enable Bluetooth")
            .setMessage("Для работы приложения необходимо включить Bluetooth. Вк..")
            .setPositiveButton("OK"){_,_ ->
                enableBluetooth()
            }
            .setNegativeButton("Cancel"){dialogInterface,_ ->
                dialogInterface.dismiss()
                finish()
            }
            .create()
            .show()
    }



    private fun handleBluetoothState(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_OFF -> showEnableBluetoothMessage()
            // Другие состояния Bluetooth можно обработать по необходимости
        }
    }

    private fun registerBluetoothStateReceiver() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
    }

    private fun unregisterBluetoothStateReceiver() {
        unregisterReceiver(bluetoothStateReceiver)
    }

    fun showBottomNavigationView() {
        binding.bottomNavBar.visibility = View.VISIBLE
        binding.deviceName.visibility  = View.VISIBLE
    }

    fun hideBottomNavigationView() {
        binding.bottomNavBar.visibility = View.GONE
        binding.deviceName.visibility  = View.GONE

    }


    override fun onItemClick(device: BluetoothDevice) {
        devAddress = device.address
    }

}