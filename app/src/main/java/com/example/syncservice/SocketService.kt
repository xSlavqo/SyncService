package com.example.syncservice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SocketService : Service() {

    private var server: SocketServer? = null
    private val channelId = "SocketServiceChannel"
    private val notificationId = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sync Service Active")
            .setContentText("Listening for connections...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(notificationId, notification)

        val serverPort = 48151
        val serverKey = "aBcDeFgHiJkLmNoPqRsTuVwXyZ123456"
        server = SocketServer(serverPort, serverKey)
        server?.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        stopForeground(true)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            channelId,
            "Socket Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}