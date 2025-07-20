// com/utils/datasync/core/SocketServer.kt
package com.utils.datasync.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket

class SocketServer(private val port: Int, private val secretKey: String) {

    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true

        GlobalScope.launch(Dispatchers.IO) {
            Log.d("SocketServer", "Server starting...")
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket(port)
                Log.d("SocketServer", "Server listening on port $port")

                while (isRunning) {
                    val clientSocket = serverSocket.accept()
                    val clientAddress = clientSocket.inetAddress.hostAddress
                    val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                    val authMessage = reader.readLine()

                    if (authMessage == secretKey) {
                        Log.d("SocketServer", "Client authenticated: $clientAddress")
                        // W przyszłości tutaj będzie pętla do odczytywania komend
                        // Na razie tylko logujemy sukces i zamykamy
                    } else {
                        Log.w("SocketServer", "Failed auth attempt from: $clientAddress")
                    }

                    clientSocket.close()
                }
            } catch (e: Exception) {
                Log.e("SocketServer", "Error in server loop", e)
            } finally {
                serverSocket?.close()
                isRunning = false
                Log.d("SocketServer", "Server stopped.")
            }
        }
    }
}