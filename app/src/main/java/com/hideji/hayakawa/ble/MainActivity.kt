package com.hideji.hayakawa.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.appcompat.app.AppCompatActivity
import com.hideji.hayakawa.ble.BleUtils.Companion.writeCharacteristic
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var mDeviceAddress: String? = "D4:36:39:6B:97:67"
    lateinit var mBluetoothManager: BluetoothManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val policy =
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        btnOn.setOnClickListener {
            writeCharacteristic(applicationContext, "ligar\n")
        }

        btnOff.setOnClickListener {
            writeCharacteristic(applicationContext, "desligar\n")
        }

        btnServiceOn.setOnClickListener {
            actionOnService(Actions.START)
        }

        btnServiceOff.setOnClickListener {
            actionOnService(Actions.STOP)
        }

        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager.adapter
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress)

        var intentFilter : IntentFilter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
        registerReceiver(powerConnectedReceiver, intentFilter)
    }

    private var powerConnectedReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            actionOnService(Actions.START)
        }
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, BleService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it)
                return
            }
            startService(it)
        }
    }
}