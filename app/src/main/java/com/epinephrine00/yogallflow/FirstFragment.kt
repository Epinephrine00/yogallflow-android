package com.epinephrine00.yogallflow

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.epinephrine00.yogallflow.data.LEDSequence
import com.epinephrine00.yogallflow.databinding.FragmentFirstBinding
import com.epinephrine00.yogallflow.thread.BluetoothThread
import com.google.gson.annotations.Expose
import java.util.UUID
import com.epinephrine00.yogallflow.MainActivity.Companion.activity as mainActivity

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */

class FirstFragment : Fragment(), SerialListener{
    lateinit var handler : Handler

    private var _binding: FragmentFirstBinding? = null

    lateinit var LEDSequencesLinearLayout:LinearLayout
    lateinit var buttonConnect:Button
    lateinit var pairListLinearLayout: LinearLayout
    private var bluetoothThread : BluetoothThread? = null

    private var scanning : Boolean = false
    private val deviceAddressList = ArrayList<String>()
    private var alertDialog: AlertDialog? = null
    private var alertDialog1: AlertDialog? = null
    lateinit var deviceListLinearLayout: LinearLayout
    lateinit var deviceListRefreshLayout:SwipeRefreshLayout
    lateinit var exportButton:Button
    lateinit var importButton:Button

    var selectBluetoothDevice: BluetoothDevice? = null

    var service: SerialService? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var fragment: FirstFragment
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        handler = Handler(Looper.getMainLooper())
        fragment = this

        LEDSequencesLinearLayout = binding.LEDSequencesLinearLayout
        buttonConnect = binding.buttonConnect
        importButton = binding.ImportButton
        exportButton = binding.ExportButton

        exportButton.setOnClickListener {
            mainActivity.saveSequenceData()
            mainActivity.exportJsonFile()
        }
        importButton.setOnClickListener {
            mainActivity.openFilePicker()
        }

        buttonConnect.setOnClickListener {
            var doContinue = true
            if (!mainActivity.bluetoothCheck(isMainActivity = false)){
                    doContinue = false
            }
            Log.d("asdf", "asdfasdfasdf $doContinue")
            if(doContinue)
                showDialog()
        }


        binding.buttonFirst.visibility = View.VISIBLE
        return binding.root

    }

    fun showDialog() {
        if (!mainActivity.bluetoothCheck(false)) {
            Toast.makeText(mainActivity, "블루투스 또는 권한이 켜져있지 않거나 지원되지 않습니다", Toast.LENGTH_SHORT)
                .show()
            mainActivity.initBluetooth()
            return
        }
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_pair_device, null)
        pairListLinearLayout = dialogLayout.findViewById<LinearLayout>(R.id.pairListLinearLayout)

        fun addButton(address:String){
            val button = Button(activity)
            val params =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            button.text = "블루투스 ($address)" //${device.name}
            button.layoutParams = params
            button.setOnClickListener {
                BluetoothThread.connect = ""
                var device: BluetoothDevice? = null
                var doConnect = true
                try {
                    device = mainActivity.bluetoothAdapter.getRemoteDevice(address)
                    deviceSelect(device)
                }
                catch(e:Exception){
                    Toast.makeText(mainActivity, "장치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    doConnect = false
                    Log.d("addButton", "${e.message}")
                }
                if(doConnect) {
                    connectDevice(selectBluetoothDevice!!)
                    alertDialog1?.dismiss()
                }

            }
            pairListLinearLayout.addView(button)
        }
        for(i in 0..<mainActivity.pairDeviceList.size){
            try {
                //val device = bluetoothAdapter.getRemoteDevice(mainActivity.pairDeviceList[i])
                //addButton(device)
                Log.d("addButton", "${mainActivity.pairDeviceList[i]}")
                addButton(mainActivity.pairDeviceList[i])
            }
            catch (e:Exception){
                Log.d("addPairButton", "${e.message}")
            }
        }


        builder.apply {
            setNegativeButton("닫기") { dialog, which ->
                dialog.dismiss()
            }
            setNeutralButton("새 장치 등록"){dialog, which ->
                showConnectNewDialog()
                dialog.dismiss()
            }
            setView(dialogLayout)
        }
        alertDialog1 = builder.create()
        alertDialog1?.setOnDismissListener {
            deviceAddressList.clear()
        }
        alertDialog1?.show()
    }

    fun showConnectNewDialog(){

        // 다이얼로그 빌더 생성
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_connect, null)

        deviceListLinearLayout = dialogLayout.findViewById<LinearLayout>(R.id.deviceListLinearLayout)
        deviceListRefreshLayout = dialogLayout.findViewById<SwipeRefreshLayout>(R.id.deviceListRefreshLayout)
        deviceListRefreshLayout.setOnRefreshListener {
            scanLeDevice()
        }
        scanLeDevice()
        builder.apply {
            setNegativeButton("닫기") { dialog, which ->
                dialog.dismiss()
            }
            setView(dialogLayout)
        }
        alertDialog = builder.create()
        alertDialog?.setOnDismissListener {
            deviceAddressList.clear()
        }
        alertDialog?.show()
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        mainActivity.bluetoothAvailable
        if(!mainActivity.bluetoothAvailable)
            return

        val bluetoothLeScanner = mainActivity.bluetoothAdapter!!.bluetoothLeScanner
        // Stops scanning after 10 seconds.
        val SCAN_PERIOD: Long = 10000

        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                deviceListRefreshLayout.isRefreshing = false
                bluetoothLeScanner.stopScan(this.leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            deviceListRefreshLayout.isRefreshing = true
            deviceAddressList.clear()
            bluetoothLeScanner.startScan(this.leScanCallback)
        } else {
            scanning = false
            deviceListRefreshLayout.isRefreshing = false
            bluetoothLeScanner.stopScan(this.leScanCallback)
        }

    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("BLE SCAN", "스캔 중 에러 발생 $errorCode")
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)

        }

        @SuppressLint("SuspiciousIndentation", "MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            if (deviceAddressList.contains(result.device.address))
                return


//            val scanRecord = result.scanRecord
//            val serviceUuids = scanRecord?.serviceUuids

            var devname_ = result.device.name?:"null"
            var devName = devname_.trim()
//            createDeviceButton(result.device)
            Log.d("LEScanResult", "device name : ${result.device.name}")
//            Log.d("LEScanResult", "isFilter1 : ${devName.equals(LEdeviceFilter, true)}")
//            Log.d("LEScanResult", "isFilter2 : ${devName.equals(LEdeviceFilter2, true)}")
//            Log.d("LEScanResult", "isFilter3 : ${devName.equals(LEdeviceFilter3, true)}")
            if (devName.equals("mpy-uart", true) ||
                devName.equals("yogallflow", true)) {
//                if (serviceUuids != null) {
//                    for (uuid in serviceUuids) {
//                        Log.d("BLEDevice", "UUID: ${uuid.uuid}")
//                    }
//                }
                createDeviceButton(result.device)
            }
        }
    }
    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun createDeviceButton(device: BluetoothDevice) : Button {
        val button = Button(activity)
        val params =
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        button.text = "블루투스 (${device.address})" //${device.name}
        button.layoutParams = params
        button.setOnClickListener {
            BluetoothThread.connect = ""
            deviceSelect(device)
            connectDevice(selectBluetoothDevice!!)
            alertDialog?.dismiss()
        }
        deviceListLinearLayout.addView(button)

        mainActivity.pairDeviceList.add(device.address.toString())
        Log.d("address", "${device.address}")
        mainActivity.savePairDevices()

        deviceAddressList.add(device.address)
        return button
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun deviceSelect(device: BluetoothDevice) {
        selectBluetoothDevice = device
    }


    private fun connectDevice(device: BluetoothDevice) {
        service = SerialService()
        val socket = SerialSocket(mainActivity.applicationContext, device)
        service?.connect(socket)
    }

    private fun connectDevice_legacy(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(mainActivity,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val bluetoothGatt = device.connectGatt(mainActivity, false, object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    fragment.handler.post(Runnable {
                        var text = "${device.address}에 연결되었습니다!"
                        Toast.makeText(mainActivity, text, Toast.LENGTH_SHORT).show()
                    })
                    Log.d("BluetoothThread", "Connected to GATT server.")
                    gatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BluetoothThread", "Disconnected from GATT server.")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Log.d("target", "onServicesDiscovered Called!")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    var uartUUID: UUID? = null
                    var txCharUUID: UUID? = null
                    var cccdUUID: UUID? = null

                    try {
                        for (service in gatt.services) {
                            //00001801, 6e400001
                            Log.d("BluetoothThread", "Service UUID: ${service.uuid}")
                            for (characteristic in service.characteristics) {
                                Log.d(
                                    "BluetoothThread",
                                    "Characteristic UUID: ${characteristic.uuid}"
                                )

                                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                                    txCharUUID = characteristic.uuid
                                    uartUUID = service.uuid
                                    cccdUUID =
                                        characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))?.uuid
                                    Log.d("BluetoothThread", "Found UUID Set: $uartUUID, TX UUID: $txCharUUID, CCCD UUID: $cccdUUID")
                                    //throw Exception()
                                }
                            }

                        }
                    }
                    catch(e:Exception){
                        Log.d("BluetoothThread", "Found UART UUID: $uartUUID, TX UUID: $txCharUUID, CCCD UUID: $cccdUUID")
                    }

                    if (uartUUID != null && txCharUUID != null && cccdUUID != null) {
                        Log.d("BluetoothThread", "Found UART UUID: $uartUUID, TX UUID: $txCharUUID, CCCD UUID: $cccdUUID")

                        if (bluetoothThread!=null)
                            bluetoothThread!!.stop()

                        bluetoothThread = BluetoothThread(device)
                        bluetoothThread!!.start()
                    } else {
                        Log.e("BluetoothThread", "Could not find required UUIDs in the connected device.")
                    }
                } else {
                    Log.w("BluetoothThread", "onServicesDiscovered received: $status")
                }
            }
        })

        bluetoothGatt.discoverServices()
    }

    fun sendDataToDevice(message:String){
        try{
            val data = message.toByteArray()
            service?.write(data)
        }
        catch(e:Exception){
            Log.e("write", "${e.message}")
        }
    }

    fun sendSequence(data: LEDSequence){
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

    fun renderSeqLinearLayout(){
        LEDSequencesLinearLayout.removeAllViews()
        for(i in 0..<mainActivity.sequenceData.size){

            val linearLayouttmp = LinearLayout(mainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            var colorSet = mainActivity.sequenceData[i].ledList[0]

            var constraintLayout = ConstraintLayout(mainActivity).apply{
                layoutParams = LayoutParams(500, 500)
                //LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                setPadding(16)
            }
            for(j in 0..5)
                constraintLayout.addView(createRotatedLinearLayout(j*30f, colorSet[j], colorSet[6+j]))
            linearLayouttmp.addView(constraintLayout)
            var nametv = TextView(mainActivity).apply{
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                text = mainActivity.sequenceData[i].name
                textSize = 24f
                gravity = Gravity.LEFT
            }
            linearLayouttmp.addView(nametv)
            val space = Space(mainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply{
                    weight = 4f
                }
            }
            linearLayouttmp.addView(space)

            val setButton =  Button(mainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                text = "설정"
            }
            val removeButton =  Button(mainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                text = "삭제"
            }
            setButton.setOnClickListener {
                if(selectBluetoothDevice!=null) {
                    sendSequence(mainActivity.sequenceData[i])
                }
                else {
                    Toast.makeText(mainActivity, "장치와 연결되지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            linearLayouttmp.addView(setButton)
            //linearLayouttmp.addView(removeButton)

            LEDSequencesLinearLayout.addView(linearLayouttmp)

        }
    }
    fun createRotatedLinearLayout(rotation: Float, color1:ArrayList<Int>, color2:ArrayList<Int>): LinearLayout {
        return LinearLayout(mainActivity).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            this.rotation = rotation

            // First Button
            val button1 = Button(mainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(50, 50)
            }
            var r = ((color1[0]/100f)*255f).toInt()
            var g = ((color1[1]/100f)*255f).toInt()
            var b = ((color1[2]/100f)*255f).toInt()
            button1.setBackgroundColor(Color.rgb(r,g,b))

            // Space between Buttons
            val space = Space(mainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 250)
            }

            // Second Button
            val button2 = Button(mainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(50, 50)
            }
            r = ((color2[0]/100f)*255f).toInt()
            g = ((color2[1]/100f)*255f).toInt()
            b = ((color2[2]/100f)*255f).toInt()
            button2.setBackgroundColor(Color.rgb(r,g,b))

            addView(button1)
            addView(space)
            addView(button2)
        }
    }

    override fun onStart() {
        super.onStart()
        if (service != null) service?.attach(this)
        else mainActivity.startService(
            Intent(
                activity,
                SerialService::class.java
            )
        ) // prevents service destroy on unbind from recreated activity caused by orientation change
    }
    override fun onStop() {
        if (service != null && !mainActivity.isChangingConfigurations) service?.detach()
        super.onStop()
    }

    override fun onSerialConnect() {
        status("connected")
        //connected = Connected.True
    }
    override fun onSerialConnectError(e: java.lang.Exception) {
        status("connection failed: " + e.message)
        disconnect()
    }
    override fun onSerialRead(data: ByteArray) {
//        val datas: ArrayDeque<ByteArray> = ArrayDeque()
//        datas.add(data)
//        receive(datas)
    }

    override fun onSerialRead(datas: java.util.ArrayDeque<ByteArray>?) {
        TODO("Not yet implemented")
    }

    fun onSerialRead(datas: ArrayDeque<ByteArray?>?) {
//        receive(datas)
    }

    override fun onSerialIoError(e: java.lang.Exception) {
        status("connection lost: " + e.message)
        disconnect()
    }


    private fun disconnect() {
        //connected = Connected.False
        service?.disconnect()
    }

    private fun status(asdf:String){
        Log.d("status", asdf)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        renderSeqLinearLayout()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}