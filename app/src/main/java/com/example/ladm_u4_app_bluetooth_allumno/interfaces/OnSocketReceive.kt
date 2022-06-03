package com.example.bluetoothchatapp.interfaces

import android.bluetooth.BluetoothSocket

interface OnSocketReceive {
    fun onReceive(blueSocket: BluetoothSocket)
}