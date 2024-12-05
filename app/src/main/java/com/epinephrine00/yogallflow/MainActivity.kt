package com.epinephrine00.yogallflow

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.epinephrine00.yogallflow.data.LEDSequence
import com.epinephrine00.yogallflow.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.IOException

// MOTTO : 일단 급하게 임시로 만드는 앱이니 코드가 더러워도(더러운거 인지하고있음) 일단 작동되게 만들자

class MainActivity : AppCompatActivity() {
    lateinit var handler : Handler

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var bluetoothAdapter : BluetoothAdapter
    private var  bluetoothLE : Boolean = false
    var  bluetoothEnable : Boolean = false
    private val REQUEST_ENABLE_BT = 100
    lateinit var sequenceData: ArrayList<LEDSequence>
    private lateinit var sequenceFile:File

    private val exportFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
            uri?.let { saveJsonToFile(it) }
        }

    var bluetoothAvailable:Boolean = false


    var pairDeviceList : ArrayList<String> = ArrayList()
    lateinit var pairDevicesFile:File


    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var activity: MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity = this as MainActivity
        handler = Handler(Looper.getMainLooper())
        sequenceFile = File(activity.filesDir, "ledSequences.json")
        pairDevicesFile = File(activity.filesDir, "pairdevices.txt")
        loadSequenceData()
        loadPairDevices()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        initBluetooth()

    }


    fun exportJsonFile() {
        // sequenceFile의 내용 읽기
        if (sequenceFile.exists()) {
            // 파일 내용 읽기
            val fileContent = readFile(sequenceFile)

            // 사용자에게 파일을 저장할 위치 지정 요청
            exportFileLauncher.launch("LEDSequenceData.json")  // 파일 이름 지정
        } else {
            // 파일이 존재하지 않으면 알림
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
        }
    }
    // 파일 내용을 읽는 함수
    private fun readFile(file: File): String {
        return try {
            val inputStream = FileInputStream(file)
            inputStream.bufferedReader().use { it.readText() } // 파일 내용을 읽어서 반환
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }
    private fun saveJsonToFile(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val content = readFile(sequenceFile)
                outputStream.write(content.toByteArray())
                Toast.makeText(this, "File exported successfully!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to export file", Toast.LENGTH_SHORT).show()
        }
    }
    fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/json" // JSON 파일만 선택
        startActivityForResult(intent, 2000)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 2000 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // 선택된 파일의 URI가 uri로 전달됨
                loadSequenceDataFromUri(uri)
            }
        }
    }
    private fun loadSequenceDataFromUri(uri: Uri) {
        try {
            // ContentResolver를 사용하여 URI에서 파일 읽기
            val inputStream = contentResolver.openInputStream(uri)
            val jsonData = inputStream?.bufferedReader().use { it?.readText() }

            if (jsonData != null) {
                val gson = Gson()
                val listType = object : TypeToken<ArrayList<LEDSequence>>() {}.type
                Log.d("loadSeq", "로드 전 sequenceData : ${sequenceData.size}")
                sequenceData.addAll(gson.fromJson(jsonData, listType))
                Log.d("loadSeq", "로드 후 sequenceData : ${sequenceData.size}")
                Log.d("loadSeq", "데이터 성공적인 적재하다.")

                activity.saveSequenceData()
                FirstFragment.fragment.renderSeqLinearLayout()
            } else {
                Log.d("loadSeq", "파일 내용이 비어 있음.")
            }
        } catch (e: IOException) {
            Log.e("loadSeq", "데이터 적재 오류 : ${e.message}")
        } catch (e: Exception) {
            Log.e("loadSeq", "예상치못한 오류 : ${e.message}")
        }
    }




    fun addLEDSequence(duration:ArrayList<Int>, ledList:ArrayList<ArrayList<ArrayList<Int>>>, name:String){
        // Structure
        // ArrayList<    duration, [ (RGB0), (RGB1), ..., (RGB10), (RGB11) ]     >    // duration : Int, (RGB) : ArrayList<Int>
        // TODO : ....를 위한 data class를 만들겠음. ArrayList<LEDSequence> 이런식으로 해야지
        // ㄴ뭐 별것도 아닌걸로 TODO를 적고있어 아니 애초에 구현했으면 지우세요
        //val data = listOf(duration, ledList, name) // 이거 말고ㅇㅇ
        val data = LEDSequence(duration, ledList, name)
        sequenceData.add(data)
        saveSequenceData()
    }


    fun loadPairDevices(){
        if(pairDevicesFile.exists()) {
            pairDevicesFile.forEachLine { line ->
                pairDeviceList.add(line)
            }
        }
    }
    fun savePairDevices(){
        val tmp = pairDeviceList.distinct().filter { it.isNotEmpty() }
        pairDeviceList = tmp as ArrayList<String>
        pairDevicesFile.printWriter().use{ out ->
            pairDeviceList.forEach{ line ->
                out.println()(line)
            }

        }
    }

    fun saveSequenceData(){
        try{
            val gson = Gson()
            val jsonData = gson.toJson(sequenceData)
            sequenceFile.writeText(jsonData)
            Log.d("saveSeq", "데이터 성공적인 구해졌다.")
        }
        catch(e:IOException){
            Log.d("saveSeq", "데이터 구원 오류 : ${e.message}")
        }
    }

    fun loadSequenceData(){
        try{
            if(!sequenceFile.exists()){
                Log.d("loadSeq", "파일이 아니다 존재하는. 초기화하는중 빈 파일...")
                sequenceData = arrayListOf()
                return
            }

            val jsonData = sequenceFile.readText()
            val gson = Gson()

            val listType = object : TypeToken<ArrayList<LEDSequence>>() {}.type
            sequenceData = gson.fromJson(jsonData, listType)
            Log.d("loadSeq", "데이터 성공적인 적재하다. ")
        }
        catch (e: IOException) {
            println("데이터 적재 오류 : ${e.message}")
            sequenceData = arrayListOf()
        }
        catch (e: Exception) {
            println("예상치못한 오류 : ${e.message}")
            sequenceData = arrayListOf()
        }
    }

    fun startBluetoothOperations(){
        return
    }

    fun initBluetooth() {

        val bluetoothManager = this.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        bluetoothLE = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

        bluetoothEnable = bluetoothAdapter.isEnabled

        bluetoothCheck()

        Log.d("MainActivity-initBluetooth", bluetoothEnable.toString())

        if(!bluetoothAvailable)
            return

    }


    fun setStatusBluetooth(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun requestBluetoothPermissionCheck() : Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //os 12+
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN), 1000)

                return false
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN), 1000)

            return false
        }
        return true
    }
    private fun requestLocationPermissionCheck() : Boolean {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            val locationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            ActivityCompat.requestPermissions(this, locationPermissions, 1000)
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    fun bluetoothCheck(isMainActivity:Boolean = true) : Boolean {
        if(!bluetoothLE){
            if(isMainActivity) setStatusBluetooth("저전력 블루투스를 지원하지 않는 기기입니다.")
            return false
        }

        if(!requestBluetoothPermissionCheck()) {
            if(isMainActivity) setStatusBluetooth("블루투스 권한이 필요합니다. ")
            return false
        }

        if(!requestLocationPermissionCheck()) {
            if(isMainActivity) setStatusBluetooth("위치 권한이 필요합니다. ")
            return false
        }

        if(!bluetoothEnable){
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            if(isMainActivity) setStatusBluetooth("블루투스를 켜지 않았습니다.")
            return false
        }

        bluetoothAvailable = true
        Log.d("bluetoothAvailable", bluetoothAvailable.toString())
        return true
    }

    fun checkBluetoothPermissions(): Boolean {
        return bluetoothCheck()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}

private operator fun Unit.invoke(line: String) {

}
