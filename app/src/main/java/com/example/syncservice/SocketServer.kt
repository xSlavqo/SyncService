package com.example.syncservice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine // <-- BRAKUJĄCY IMPORT

class SocketServer(
    private val port: Int,
    private val secretKey: String,
    private val context: Context,
    private val mediaProjection: MediaProjection
) {

    @Volatile
    private var isRunning = false
    private var serverSocket: ServerSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun start() {
        if (isRunning) return
        isRunning = true

        coroutineScope.launch {
            Log.d("SocketServer", "Server starting...")
            try {
                serverSocket = ServerSocket(port)
                Log.d("SocketServer", "Server listening on port $port")

                while (isRunning) {
                    val clientSocket = serverSocket!!.accept()
                    handleClient(clientSocket)
                }
            } catch (e: Exception) {
                if (isRunning) Log.e("SocketServer", "Error in server loop", e)
            } finally {
                Log.d("SocketServer", "Server stopped.")
                isRunning = false
            }
        }
    }

    private fun handleClient(socket: Socket) {
        coroutineScope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = socket.getOutputStream()
                val clientAddress = socket.inetAddress.hostAddress

                val authMessage = reader.readLine()
                if (authMessage != secretKey) {
                    Log.w("SocketServer", "Failed auth attempt from: $clientAddress")
                    socket.close()
                    return@launch
                }
                Log.d("SocketServer", "Client authenticated: $clientAddress")

                while (isRunning) {
                    val command = reader.readLine() ?: break
                    Log.d("SocketServer", "Received command: $command")

                    when (command) {
                        "GET_SCREENSHOT" -> {
                            val screenshotBytes = takeScreenshot()
                            if (screenshotBytes != null) {
                                withContext(Dispatchers.IO) {
                                    writer.writeInt(screenshotBytes.size)
                                    writer.write(screenshotBytes)
                                    writer.flush()
                                }
                                Log.d("SocketServer", "Screenshot sent (${screenshotBytes.size} bytes)")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SocketServer", "Client connection error", e)
            } finally {
                socket.close()
                Log.d("SocketServer", "Client disconnected.")
            }
        }
    }

    private suspend fun takeScreenshot(): ByteArray? = suspendCoroutine { continuation ->
        mainHandler.post {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val metrics: Rect = windowManager.currentWindowMetrics.bounds
            val width = metrics.width()
            val height = metrics.height()

            val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            val virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture", width, height, context.resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface, null, mainHandler
            )

            imageReader.setOnImageAvailableListener({ reader ->
                var image: Image? = null
                try {
                    image = reader.acquireLatestImage()
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width
                        val bitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)

                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        continuation.resume(stream.toByteArray())
                        bitmap.recycle()
                    } else {
                        continuation.resume(null)
                    }
                } catch (e: Exception) {
                    Log.e("Screenshot", "Error capturing image", e)
                    continuation.resume(null)
                } finally {
                    image?.close()
                    virtualDisplay?.release()
                    reader.close()
                }
            }, mainHandler)
        }
    }

    private fun OutputStream.writeInt(value: Int) {
        this.write(byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte()
        ))
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
            coroutineScope.cancel()
        } catch (e: Exception) {
            Log.e("SocketServer", "Error closing server socket", e)
        }
        Log.d("SocketServer", "Server stop requested.")
    }
}