package com.example.ladm_u4_app_bluetooth_allumno

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Message
import com.example.bluetoothchatapp.interfaces.OnHandlerMsg
import com.example.bluetoothchatapp.interfaces.OnSocketReceive
import java.io.IOException
import java.util.*

class BServerSocket(bluetoothAdapter: BluetoothAdapter, uuid: UUID,handlerM: OnHandlerMsg, onReceive: OnSocketReceive):Thread() {

    private lateinit var bluetoothServerSocket: BluetoothServerSocket
    private var onHandlerMsg = handlerM
    private var onSocketReceive = onReceive
    init {
        try {
            bluetoothServerSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(R.string.app_name.toString(),uuid)
        }catch (e:IOException){
            e.printStackTrace()
        }
    }

    override fun run() {
        var bluetoothSocket:BluetoothSocket?=null
        while (bluetoothSocket==null){
            try {
                val msg = Message.obtain()
                msg.what = BluetoothStateCustom.STATE_CONNECTING.state
                onHandlerMsg.onMsgGet(msg)
                bluetoothSocket = bluetoothServerSocket.accept()
            }catch (e:IOException){
                e.printStackTrace()
                val msg = Message.obtain()
                msg.what = BluetoothStateCustom.STATE_CONNECTION_FAILED.state
                onHandlerMsg.onMsgGet(msg)
            }
            if (bluetoothSocket!=null){
                val msg = Message.obtain()
                msg.what = BluetoothStateCustom.STATE_CONNECTED.state
                onHandlerMsg.onMsgGet(msg)
                onSocketReceive.onReceive(bluetoothSocket)
            }
        }
    }

}