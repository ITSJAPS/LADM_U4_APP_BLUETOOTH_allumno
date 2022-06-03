package com.example.ladm_u4_app_bluetooth_allumno
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object FileTransfer {
    val bufferSizeMax = 1024*1024
    fun getFileFromInput(filename:String,inputStream: InputStream?): File? {
        inputStream?.let { iS->
            val gotFile = File(filename)
            try {
                val fileOutputStream = FileOutputStream(gotFile)
                var len = 0
                val buffer = ByteArray(1024)
                while (iS.read(buffer).also { len = it }!=-1){
                    fileOutputStream.write(buffer,0,len)
                }
                iS.close()
                fileOutputStream.flush()
                fileOutputStream.close()
                return gotFile
            }catch (e: IOException){
                e.printStackTrace()
            }
        }
        return null
    }
}