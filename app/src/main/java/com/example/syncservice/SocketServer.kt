package com.example.syncservice

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket

class SocketServer(private val port: Int, private val secretKey: String) {

    @Volatile private var isRunning = false
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (isRunning) return
        isRunning = true

        GlobalScope.launch(Dispatchers.IO) {
            Log.d("SocketServer", "Server starting...")
            try {
                serverSocket = ServerSocket(port)
                Log.d("SocketServer", "Server listening on port $port")

                while (isRunning) {
                    val clientSocket = serverSocket!!.accept()
                    val clientAddress = clientSocket.inetAddress.hostAddress
                    val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                    val authMessage = reader.readLine()

                    if (authMessage == secretKey) {
                        Log.d("SocketServer", "Client authenticated: $clientAddress")
                        // W przyszłości tutaj będzie pętla do odczytywania komend
                    } else {
                        Log.w("SocketServer", "Failed auth attempt from: $clientAddress")
                    }

                    clientSocket.close()
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e("SocketServer", "Error in server loop", e)
                }
            } finally {
                Log.d("SocketServer", "Server stopped.")
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("SocketServer", "Error closing server socket", e)
        }
        Log.d("SocketServer", "Server stop requested.")
    }
}