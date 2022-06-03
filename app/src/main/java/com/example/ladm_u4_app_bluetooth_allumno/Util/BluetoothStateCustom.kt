package com.example.ladm_u4_app_bluetooth_allumno

enum class BluetoothStateCustom(val state:Int) {
    STATE_LISTENING(1),
    STATE_CONNECTING(2),
    STATE_CONNECTED(3),
    STATE_CONNECTION_FAILED(4),
    STATE_MESSAGE_RECEIVED(5),
    STATE_SEND_FILE(6),
    STATE_LOCATION(7)
}