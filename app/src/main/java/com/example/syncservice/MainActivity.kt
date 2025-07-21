package com.example.syncservice

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private lateinit var serviceSwitch: SwitchMaterial

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val serviceIntent = Intent(this, SocketService::class.java).apply {
                action = "START"
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            startForegroundService(serviceIntent)
        } else {
            // Jeśli użytkownik anuluje, upewnij się, że przełącznik jest wyłączony
            serviceSwitch.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        serviceSwitch = findViewById(R.id.serviceSwitch)
        val ipAddressTextView: TextView = findViewById(R.id.ipAddressTextView)

        ipAddressTextView.text = getIpAddress() ?: "Not Found"

        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Uruchom tylko jeśli usługa jeszcze nie działa
                if (!isServiceRunning()) {
                    screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                }
            } else {
                stopService(Intent(this, SocketService::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Synchronizuj stan przełącznika za każdym razem, gdy wracasz do aplikacji
        serviceSwitch.isChecked = isServiceRunning()
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SocketService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun getIpAddress(): String? {
        try {
            for (networkInterface in NetworkInterface.getNetworkInterfaces()) {
                for (address in networkInterface.inetAddresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}