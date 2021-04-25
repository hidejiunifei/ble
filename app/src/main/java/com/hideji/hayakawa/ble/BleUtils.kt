package com.hideji.hayakawa.ble

import android.bluetooth.*
import android.content.Context
import java.util.*

var mDeviceAddress: String? = "D4:36:39:6B:97:67"
lateinit var mBluetoothManager: BluetoothManager
lateinit var mBluetoothDevice: BluetoothDevice
lateinit var mBluetoothAdapter: BluetoothAdapter

class BleUtils {

    companion object {
        private lateinit var mMessage: String

        fun writeCharacteristic(context: Context, value: String) {

            mBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            mBluetoothAdapter = mBluetoothManager.adapter
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress)

            mMessage = value

            if (mBluetoothAdapter.isEnabled) {
                mBluetoothAdapter.disable()
            }

            mBluetoothAdapter.enable()

            mBluetoothDevice.connectGatt(context, false, mGattCallback)
        }

        private val mGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val characteristic: BluetoothGattCharacteristic? =
                        gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))
                            ?.getCharacteristic(
                                UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
                            )
                    characteristic?.setValue(mMessage)
                    gatt.writeCharacteristic(characteristic)
                    mBluetoothAdapter.disable()
                }
            }
        }
    }
}