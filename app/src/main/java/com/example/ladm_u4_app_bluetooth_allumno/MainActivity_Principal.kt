package com.example.ladm_u4_app_bluetooth_allumno

import android.Manifest
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.bluetoothchatapp.interfaces.OnHandlerMsg
import com.example.bluetoothchatapp.interfaces.OnSocketReceive
import com.example.ladm_u4_app_bluetooth_allumno.databinding.ActivityMainPrincipalBinding
import com.google.firebase.auth.FirebaseAuth
import java.io.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity_Principal : AppCompatActivity() {
    lateinit var binding: ActivityMainPrincipalBinding

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothDevices: MutableList<BluetoothDevice>
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private lateinit var listDevicesNamed:MutableList<String>

    private var selectedFile: File?=null
    private var isFileTransferFlag = false
    private lateinit var myLocationListener: MyLocationListener
    private val uuid: UUID = UUID.fromString("9c338e64-2b9c-11ec-8d3d-0242ac130003")
    private var sendReceiveMsg: SendReceiveMsg?=null
    private var currentLocation: Location?=null
    private var enemyLocation: Location?=null
    private lateinit var locationManager: LocationManager
    private val REQUEST_ENABLE_BLUETOOTH = 111
    private val CHOOSER_FILE=112
    private val REQUEST_PERMESSIONS_LOC = 222
    private val REQUEST_PERMESSIONS_EXTERNAL = 222
    var noControl=""

    override fun onCreate(savedInstanceState: Bundle?) {
        binding= ActivityMainPrincipalBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (FirebaseAuth.getInstance().currentUser!=null) {
            //Sesion activa
            var usuario = FirebaseAuth.getInstance().currentUser
            var partes: List<String>
            usuario.let {
                partes = usuario?.email.toString().split("@")
            }
            noControl=partes.get(0).toString()
        }

        //val noControl = intent.getStringExtra("control").toString()
        this.setTitle("ChatJAPS Alumno $noControl")

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        listDevicesNamed = mutableListOf()
        bluetoothDevices = mutableListOf()
        myLocationListener = MyLocationListener(this)
        arrayAdapter = ArrayAdapter(applicationContext,android.R.layout.simple_list_item_1,listDevicesNamed)
        binding.listView.adapter = arrayAdapter


        if (!bluetoothAdapter.isEnabled){
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),REQUEST_ENABLE_BLUETOOTH)
        }
        binding.apply {

            btnListen.setOnClickListener {
                /*val serverSocket = ServerSocket()
                serverSocket.start()*/
                listView.isEnabled = false
                btnListDevices.isEnabled = false
                val bServerSocket = BServerSocket(bluetoothAdapter,uuid,object: OnHandlerMsg {
                    override fun onMsgGet(msg: Message) {
                        handler.sendMessage(msg)
                    }
                }, object: OnSocketReceive {
                    override fun onReceive(blueSocket: BluetoothSocket) {
                        sendReceiveMsg = SendReceiveMsg(blueSocket)
                        sendReceiveMsg!!.start()
                    }
                })
                bServerSocket.start()
            }

            btnListDevices.setOnClickListener {
                showDevices()
            }

            btnSend.setOnClickListener {
                val msg:String = edMessage.text.toString()
                sendReceiveMsg?.writeBegin(1)
                sendReceiveMsg?.write(msg.toByteArray())
                //Toast.makeText(applicationContext,"Bytes:"+msg.toByteArray().contentToString(),Toast.LENGTH_SHORT).show()
            }

            listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                /* val clientSocket = ClientSocket(bluetoothDevices[position])
                 clientSocket.start()*/
                btnListen.isEnabled = false
                val bClientSocket = BClientSocket(bluetoothDevices[position],uuid,object:
                    OnHandlerMsg {
                    override fun onMsgGet(msg: Message) {
                        handler.sendMessage(msg)
                    }
                }, object: OnSocketReceive {
                    override fun onReceive(blueSocket: BluetoothSocket) {
                        sendReceiveMsg = SendReceiveMsg(blueSocket)
                        sendReceiveMsg!!.start()
                    }
                })
                bClientSocket.start()
                tvStatus.text = "Connecting"
            }

                checkPermissionLocationOrStart()


        }

    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==REQUEST_PERMESSIONS_EXTERNAL){
            if (grantResults[0]== PackageManager.PERMISSION_GRANTED&&grantResults[1]== PackageManager.PERMISSION_GRANTED)
            {
                startIntentChooser()
            } else {
                Toast.makeText(applicationContext,"Permissions write/read files are denied!", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode==REQUEST_PERMESSIONS_LOC){
            if (grantResults[0]== PackageManager.PERMISSION_GRANTED&&grantResults[1]== PackageManager.PERMISSION_GRANTED)
            {
                checkPermissionLocationOrStart()
            } else {
                Toast.makeText(applicationContext,"Permissions location are denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionFiles(): Boolean= (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
            &&(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)

    private fun checkPermissionLocationOrStart(){
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
            &&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION),REQUEST_PERMESSIONS_LOC)
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,2.0f,myLocationListener)
        }
    }

    fun onLocationListener(location: Location) {
        updateUI(location)
    }


    private fun updateUI(location: Location) {
        currentLocation = location
        currentLocation?.let { currLoc->
            //binding.tvYourLocation.text ="Your location: ${currLoc.latitude}|${currLoc.longitude}"
            sendReceiveMsg?.let { rec->
                rec.writeBegin(3)
                val loc = "${currLoc.latitude}"+ " "+"${currLoc.longitude}"
                rec.write(loc.toByteArray())
            }
            enemyLocation?.let {
                val distance = currLoc.distanceTo(it)
                //binding.tvEnemyLocation.text = "Enemy location: ${it.latitude}|${it.longitude}"
                //binding.tvDistance.text = "Distance:$distance"
            }
        }
    }

    fun locationString(location: Location): String {
        return Location.convert(location.latitude, Location.FORMAT_DEGREES) + " " + Location.convert(location.longitude, Location.FORMAT_DEGREES)
    }

    private fun startIntentChooser() {
        val intent = Intent()
            .setType("text/plain")
            .setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, "Select a file"), CHOOSER_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSER_FILE && resultCode == RESULT_OK) {
            val uri = data?.data
            uri?.let {
                val sdf = SimpleDateFormat("yyMMddHHmmssZ",Locale.getDefault())
                val currFile = sdf.format(Date())
                selectedFile = FileTransfer.getFileFromInput(filesDir.path+"/$currFile"+it.lastPathSegment,contentResolver.openInputStream(it))
                if (selectedFile!=null){
                    Toast.makeText(applicationContext,"file:"+selectedFile?.absolutePath, Toast.LENGTH_SHORT).show()
                    sendReceiveMsg?.writeBegin(2)
                    sendReceiveMsg?.writeFile(selectedFile!!)
                }
            }
            //Toast.makeText(applicationContext,"file:"+selectedFile.absolutePath,Toast.LENGTH_SHORT).show()
        } else if (requestCode==REQUEST_ENABLE_BLUETOOTH){
            if (resultCode== RESULT_OK){
                Toast.makeText(applicationContext,"Bluetooth is enabled", Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(applicationContext,"Bluetooth is cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDevices() {
        listDevicesNamed.clear()
        bluetoothDevices.clear()
        val listDevices:Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        listDevices.forEach {
            listDevicesNamed.add(it.name)
            bluetoothDevices.add(it)
        }
        arrayAdapter.notifyDataSetChanged()
    }

    private var handler: Handler = Handler { msg ->
        when (msg.what) {
            BluetoothStateCustom.STATE_LISTENING.state -> {
                binding.tvStatus.text = "Listening"
            }
            BluetoothStateCustom.STATE_CONNECTED.state -> {
                binding.tvStatus.text = "Connected"
            }
            BluetoothStateCustom.STATE_CONNECTING.state -> {
                binding.tvStatus.text = "Connecting"
            }
            BluetoothStateCustom.STATE_CONNECTION_FAILED.state -> {
                binding.tvStatus.text = "Connection failed!"
            }
            BluetoothStateCustom.STATE_MESSAGE_RECEIVED.state -> {
                val bytesRec:ByteArray = msg.obj as ByteArray
                val str = String(bytesRec,0,msg.arg1)
                //Toast.makeText(applicationContext,"Bytes:"+msg.arg1,Toast.LENGTH_SHORT).show()
                binding.tvMessage.text = str
            }
            BluetoothStateCustom.STATE_SEND_FILE.state ->{
                Toast.makeText(applicationContext,"You put some file!", Toast.LENGTH_SHORT).show()
            }
            BluetoothStateCustom.STATE_LOCATION.state->{
                val bytesArr = msg.obj as ByteArray
                val str = String(bytesArr,0,msg.arg1)
                val locEnemy = str.split(" ")
                if (enemyLocation==null) enemyLocation = Location(LocationManager.GPS_PROVIDER)
                enemyLocation?.let {
                    if(locEnemy.size==2){
                        it.latitude = locEnemy[0].toDouble()
                        it.longitude = locEnemy[1].toDouble()
                    }
                }
            }
        }
        true
    }

    inner class SendReceiveMsg(var bluetoothSocket: BluetoothSocket):Thread(){
        private var inputStream: DataInputStream?
        private var outputStream: DataOutputStream?

        init {
            var tempIS: DataInputStream?=null
            var tempOS: DataOutputStream?=null
            try {
                tempIS = DataInputStream(bluetoothSocket.inputStream)
                tempOS = DataOutputStream(bluetoothSocket.outputStream)
            }catch (e: IOException){
                e.printStackTrace()
            }
            inputStream = tempIS
            outputStream = tempOS
        }

        override fun run() {
            var l = 0;
            if (!bluetoothSocket.isConnected)
                bluetoothSocket.close()
            else{
                inputStream?.let {
                    l = it.readInt()
                }
                if (l==1){
                    val buffer = ByteArray(1024)
                    var bytes = 0
                    inputStream?.let { iS ->
                        while (true){
                            //-1 = because not used param arg2
                            try {
                                bytes = iS.read(buffer)
                                handler.obtainMessage(BluetoothStateCustom.STATE_MESSAGE_RECEIVED.state,bytes,-1,buffer).sendToTarget()
                            }catch (e: Exception){
                                break
                            }
                        }

                    }
                } else if (l==2) {
                    inputStream?.let { iS->
                        val sdf = SimpleDateFormat("yyMMddHHmmssZ",Locale.getDefault())
                        val currFile = sdf.format(Date())
                        val fileOutputStream = FileOutputStream(File(Environment.getExternalStorageDirectory().toString() + "/Download/$currFile.txt"))
                        var bufferSize = Math.min(FileTransfer.bufferSizeMax,iS.available())
                        val buffer = ByteArray(bufferSize)
                        var bytesRead = iS.read(buffer,0,bufferSize)
                        while (bytesRead>0){
                            fileOutputStream.write(buffer,0,bufferSize)
                            bufferSize = Math.min(FileTransfer.bufferSizeMax,iS.available())
                            bytesRead = iS.read(buffer,0,bufferSize)
                        }
                        fileOutputStream.flush()
                        fileOutputStream.close()
                        isFileTransferFlag = false
                        handler.obtainMessage(BluetoothStateCustom.STATE_SEND_FILE.state).sendToTarget()
                    }
                } else if (l==3){
                    val buffer = ByteArray(1024)
                    var bytes = 0
                    inputStream?.let { iS->
                        while (true){
                            try {
                                bytes = iS.read(buffer)
                                handler.obtainMessage(BluetoothStateCustom.STATE_LOCATION.state,bytes,-1,buffer).sendToTarget()
                            }catch (e: Exception){
                                break
                            }
                        }
                    }
                }
            }
        }

        fun write(buffer:ByteArray){
            try {
                if (!isFileTransferFlag){
                    outputStream?.write(buffer)
                    //outputStream?.flush()
                }
                //outputStream?.close()
            }catch (e: IOException){
                e.printStackTrace()
            }
        }

        fun writeBegin(v: Int){
            try {
                outputStream?.writeInt(v)
                outputStream?.flush()
                //outputStream?.close()
            }catch (e: IOException){
                e.printStackTrace()
            }
        }

        fun writeFile(file: File){
            try{
                outputStream?.let { os->
                    val fileInputStream = FileInputStream(file)
                    var bufferSize = Math.min(FileTransfer.bufferSizeMax,fileInputStream.available())
                    val buffer = ByteArray(bufferSize)
                    var bufferRead = fileInputStream.read(buffer,0,bufferSize)
                    while (bufferRead>0){
                        os.write(buffer,0,bufferSize)
                        bufferSize = Math.min(FileTransfer.bufferSizeMax,fileInputStream.available())
                        bufferRead = fileInputStream.read(buffer,0,bufferSize)
                    }
                    os.flush()
                    fileInputStream.close()
                    file.delete()
                }
            }catch (e: IOException){
                e.printStackTrace()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menuoculto,menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            R.id.acerca->{
                Toast.makeText(this,"Ahora eres Tecnologia",Toast.LENGTH_LONG).show()
            }
            R.id.secion->{
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this,Main1Activity::class.java))
                finish()
            }
            R.id.salir->{
                finish()
            }
        }

        return true
    }

}