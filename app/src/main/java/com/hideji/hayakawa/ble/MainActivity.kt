package com.hideji.hayakawa.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.BatteryManager
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var mConnected = false
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mDeviceAddress: String? = "D4:36:39:6B:97:67"

    companion object {

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }

    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService!!.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    private val batteryReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val level: Int = intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            if (mConnected) {
                if (level * 100 / scale.toFloat() > 84) {
                    writeCharacteristic("desligar\n")
                } else if (level * 100 / scale.toFloat() < 50) {
                    writeCharacteristic("ligar\n")
                }
            }
        }
    }

    private fun writeCharacteristic(value : String)
    {
        if (mConnected) {
            val characteristic: BluetoothGattCharacteristic? =
                mBluetoothLeService?.mBluetoothGatt?.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))
                    ?.getCharacteristic(
                        UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
                    )
            characteristic?.setValue(value)
            mBluetoothLeService?.mBluetoothGatt?.writeCharacteristic(characteristic)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val batteryStatus: Intent? = registerReceiver(batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        btnOn.setOnClickListener {
            writeCharacteristic("ligar\n")
        }

        btnOff.setOnClickListener {
            writeCharacteristic("desligar\n")
        }

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
    }

    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    mConnected = true
                    invalidateOptionsMenu()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    mConnected = false
                    invalidateOptionsMenu()
                }
            }
        }
    }

}