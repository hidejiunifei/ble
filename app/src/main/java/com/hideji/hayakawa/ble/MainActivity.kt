package com.hideji.hayakawa.ble

import android.bluetooth.*
import android.content.*
import android.os.BatteryManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var mDeviceAddress: String? = "D4:36:39:6B:97:67"
    private lateinit var mBluetoothManager: BluetoothManager
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothDevice: BluetoothDevice
    private lateinit var mBluetoothGatt: BluetoothGatt
    private lateinit var mMessage: String

    companion object {

    }

    private val batteryReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val level: Int = intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            if (level * 100 / scale.toFloat() > 84) {
                writeCharacteristic("desligar\n")
            } else if (level * 100 / scale.toFloat() < 26) {
                writeCharacteristic("ligar\n")
            }
        }
    }

    private fun writeCharacteristic(value : String) {
        mMessage = value
        mBluetoothGatt = mBluetoothDevice.connectGatt(this, false, mGattCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        registerReceiver(batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        btnOn.setOnClickListener {
            writeCharacteristic("ligar\n")
        }

        btnOff.setOnClickListener {
            writeCharacteristic("desligar\n")
        }

        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.adapter
        mBluetoothDevice = mBluetoothAdapter!!.getRemoteDevice(mDeviceAddress)
    }

    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val characteristic: BluetoothGattCharacteristic? =
                    mBluetoothGatt?.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))
                        ?.getCharacteristic(
                            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
                        )
                characteristic?.setValue(mMessage)
                mBluetoothGatt?.writeCharacteristic(characteristic)
                mBluetoothGatt.disconnect()
            }
        }
    }
}