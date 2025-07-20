package com.example.syncservice

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serviceSwitch: SwitchMaterial = findViewById(R.id.serviceSwitch)
        val ipAddressTextView: TextView = findViewById(R.id.ipAddressTextView)

        ipAddressTextView.text = getIpAddress() ?: "Not Found"

        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            val serviceIntent = Intent(this, SocketService::class.java)
            if (isChecked) {
                startForegroundService(serviceIntent)
            } else {
                stopService(serviceIntent)
            }
        }
    }

    private fun getIpAddress(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces().toList()
            for (networkInterface in networkInterfaces) {
                val addresses = networkInterface.inetAddresses.toList()
                for (address in addresses) {
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