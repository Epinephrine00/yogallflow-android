package com.epinephrine00.yogallflow.thread

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.pm.PackageManager
import android.health.connect.datatypes.units.Pressure
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.epinephrine00.yogallflow.MainActivity.Companion.activity
import com.epinephrine00.yogallflow.FirstFragment.Companion.fragment
import com.epinephrine00.yogallflow.MainActivity
import com.epinephrine00.yogallflow.SerialService
import com.epinephrine00.yogallflow.data.LEDSequence
import java.util.*
import kotlin.collections.ArrayList


class BluetoothThread(val device: BluetoothDevice) : Thread() {
//                      val uartUUID: UUID,
//                      val txCharUUID: UUID,
//                      val cccdUUID: UUID) : Thread() {
    companion object {
        var connect: String = ""
    }

    lateinit var service:SerialService

    private var bluetoothGatt: BluetoothGatt? = null
    override fun run() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        )
            return
        if (connect == "") {
            //service.attach(fragment)
            //bluetoothGatt = device.connectGatt(activity, true, gattCallback)
            connect = device.address
        }
    }

    var dataLength: Int = -1
    var inner = true

    var outList = ArrayList<Int>()
    var inList = ArrayList<Int>()

    //    var UART_UUID    = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
//    var TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
//    //var UART_UUID = UUID.fromString("FDA50693-A4E2-4FB1-AFCF-C6EB07647825")
//    var CCCD_UUID    = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
//    var TX_CHAR_UUID = txCharUUID
//    var UART_UUID    = uartUUID
    //var CCCD_UUID    = cccdUUID

    @SuppressLint("MissingPermission")
    fun sendDataToDevice(data: String) {

//        val dataBytes = data.toByteArray()
//
//        bluetoothGatt?.let { gatt ->
//            // TX 특성 가져오기
//            val txCharacteristic = gatt.getService(UART_UUID)?.getCharacteristic(TX_CHAR_UUID)
//
//            txCharacteristic?.let {
//                // 특성에 데이터를 설정
//                it.value = dataBytes
//
//                // 특성에 데이터 전송 요청
//                val success = gatt.writeCharacteristic(it)
//
//                // 전송 성공 여부 로그
//                if (success) {
//                    Log.d("BluetoothThread", "Data sent successfully: $data")
//                } else {
//                    Log.d("BluetoothThread", "Failed to send data")
//                }
//            }
//        }
    }

    fun sendSequence(data:LEDSequence){
        val duration = data.duration
        val ledList = data.ledList
        sendDataToDevice("entry")
        Log.d("sendingData","entry")
        for (i in 0..<duration.size){
            sendDataToDevice("start")
            Log.d("sendingData","start")
            sendDataToDevice(String.format("d:%d", duration[i]))
            Log.d("sendingData",String.format("d:%d", duration[i]))
            for(j in 0..<ledList[i].size){
                val r = ledList[i][j][0]
                val g = ledList[i][j][1]
                val b = ledList[i][j][2]
                sendDataToDevice(String.format("v:%d:%02x%02x%02x", j, g, r, b))
                Log.d("sendingData", String.format("v:%d:%02x%02x%02x", j, g, r, b))
            }
            sendDataToDevice("end")
            Log.d("sendingData","end")
        }
        sendDataToDevice("eof")
        Log.d("sendingData", "eof")

    }


    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt?.discoverServices()
                    fragment.handler.post(Runnable {
                        Toast.makeText(activity, "${device.name}에 연결되었습니다!", Toast.LENGTH_SHORT).show()
                    })
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    activity.handler.post(Runnable {
                        //fragment.selectBluetoothDevice = null
                        Log.d("BluetoothThread", "Device Disconnected! ")
                    })
                }

                else -> {
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    //var tx = gatt.getService(UART_UUID).getCharacteristic(TX_CHAR_UUID);
//                    if (gatt.setCharacteristicNotification(tx, true)) {
//                        val descriptor: BluetoothGattDescriptor = tx.getDescriptor(CCCD_UUID)
//                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                        gatt.writeDescriptor(descriptor)
//                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status:Int){
            Log.d("BluetoothThread", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            val data = characteristic!!.value
            val str = String(data).replace(Regex("[^0-9]"), "")
            Log.d("BluetoothThread", "str = $str")
        }

        override fun onDescriptorRead(gatt : BluetoothGatt?, descriptor : BluetoothGattDescriptor?, status:Int){

            Log.d("BluetoothThread", "str = $descriptor")
        }

        @SuppressLint("MissingPermission", "SetTextI18n")
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {

        }
    }
}



