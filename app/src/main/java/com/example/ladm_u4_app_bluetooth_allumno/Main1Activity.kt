package com.example.ladm_u4_app_bluetooth_allumno

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.ladm_u4_app_bluetooth_allumno.databinding.ActivityMain1Binding
import com.google.firebase.auth.FirebaseAuth

class Main1Activity : AppCompatActivity() {
    lateinit var binding:ActivityMain1Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding=ActivityMain1Binding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.registrar.setOnClickListener {
            if (binding.noControl.text.equals("")) {
                AlertDialog.Builder(this)
                    .setTitle("Atencion")
                    .setMessage("Ingresa tu numero de control!")
                    .show()
            }else {
                val autenticacion = FirebaseAuth.getInstance()
                val dialogo = ProgressDialog(this)
                dialogo.setMessage("Registrando...")
                dialogo.setCancelable(false)
                dialogo.show()
                autenticacion.createUserWithEmailAndPassword(
                    binding.noControl.text.toString() + "@hotmail.com",
                    binding.noControl.text.toString()
                )
                    .addOnCompleteListener {
                        dialogo.dismiss()
                        if (it.isSuccessful) {
                            Toast.makeText(this, "Registrado con éxito", Toast.LENGTH_LONG).show()
                            autenticacion.signOut()
                        } else {
                            AlertDialog.Builder(this)
                                .setMessage("Error: No se pudo registrar.")
                                .show()
                        }
                    }
                binding.noControl.text.clear()
            }
        }

        binding.ingresar.setOnClickListener {
            val autenticacion = FirebaseAuth.getInstance()
            val dialogo = ProgressDialog(this)
            dialogo.setMessage("Verificando número de control")
            dialogo.setCancelable(false)
            dialogo.show()
            autenticacion.signInWithEmailAndPassword(
                binding.noControl.text.toString()+"@hotmail.com",
                binding.noControl.text.toString()
            ).addOnCompleteListener {
                dialogo.dismiss()
                if (it.isSuccessful){
                    var ventana = Intent(this,EnvioAsistencia::class.java)
                    var usuario = FirebaseAuth.getInstance().currentUser
                    var partes:List<String>
                    usuario.let {
                        partes = usuario?.email.toString().split("@")
                    }
                    ventana.putExtra("No.Control",partes.get(0))
                    startActivity(ventana)
                    finish()
                    return@addOnCompleteListener
                }
                AlertDialog.Builder(this)
                    .setMessage("Error: No se encontró el No. Control")
                    .show()
            }
            binding.noControl.text.clear()

        }
        if (FirebaseAuth.getInstance().currentUser!=null){
            //Sesion activa
            var ventana = Intent(this,EnvioAsistencia::class.java)
            var usuario = FirebaseAuth.getInstance().currentUser
            var partes:List<String>
            usuario.let {
                partes = usuario?.email.toString().split("@")
            }
            ventana.putExtra("No.Control",partes.get(0))
            startActivity(ventana)
            finish()
        }
    }
}