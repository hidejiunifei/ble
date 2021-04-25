package com.hideji.hayakawa.ble

import android.bluetooth.*
import android.content.Context
import java.util.*

lateinit var mBluetoothDevice: BluetoothDevice
lateinit var mBluetoothAdapter: BluetoothAdapter
var mBlueToothGatt : BluetoothGatt? = null

class BleUtils {

    companion object {
        private lateinit var mMessage: String

        fun writeCharacteristic(context: Context, value: String) {
            mMessage = value

            if (mBluetoothAdapter.isEnabled) {
                mBluetoothAdapter.disable()
            }

            mBluetoothAdapter.enable()

            if (mBlueToothGatt != null) {
                mBlueToothGatt?.close()
            }
            mBluetoothDevice.connectGatt(context, false, mGattCallback)
        }

        private val mGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                mBlueToothGatt = gatt
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                mBlueToothGatt = gatt
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val characteristic: BluetoothGattCharacteristic? =
                        gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))
                            ?.getCharacteristic(
                                UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
                            )
                    characteristic?.setValue(mMessage)
                    gatt.writeCharacteristic(characteristic)
                    gatt.close()
                    mBluetoothAdapter.disable()
                }
            }
        }
    }
}