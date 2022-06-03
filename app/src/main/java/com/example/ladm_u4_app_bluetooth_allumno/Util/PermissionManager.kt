package com.example.ladm_u4_app_bluetooth_allumno

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionManager(var activity:Activity) {

    private var requestList = arrayListOf<PermissionRequest>()

    fun permissionRequestOrDo(permissionStr: String,doOnPermission: Runnable):PermissionRequest{
       val request = PermissionRequest(requestList.size,permissionStr,doOnPermission)
        requestList.add(request)
        return request
    }

    fun onRequestPermissionResult(requestCode:Int,permissions:Array<String>,grandResults:IntArray){
            requestList.forEach {
                if (requestCode==it.requestId&&grandResults[0]==PackageManager.PERMISSION_GRANTED){
                    it.doOnPermission.run()
                }
            }
    }

    inner class PermissionRequest (var requestId:Int,var permissionStr:String,var doOnPermission:Runnable){

        fun perform(){
            if(ContextCompat.checkSelfPermission(activity,permissionStr)==PackageManager.PERMISSION_GRANTED){
               doOnPermission.run()
            }else{
                activity.requestPermissions(arrayOf(permissionStr),requestId)
            }
        }

    }

}