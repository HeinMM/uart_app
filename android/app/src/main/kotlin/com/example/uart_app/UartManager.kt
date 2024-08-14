package com.example.uart_app

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.plugin.common.MethodChannel

class UartManager(private val channel: MethodChannel) {
    companion object {
        init {
            System.loadLibrary("native-lib") // Load the native library (e.g., libuart-lib.so)
        }
    }

    // Declare the native methods
    external fun openUART(devicePath: String, baudRate: Int): Int
    private external fun readUART(fd: Int, buffer: ByteArray, size: Int): Int
    external fun closeUART(fd: Int): Int

    private var isReading = true

    private val mainHandler = Handler(Looper.getMainLooper()) // Handler for main thread

    fun startReading(devicePath: String, baudRate: Int) {
        val fd = openUART(devicePath, baudRate)
        if (fd < 0) {
            mainHandler.post {
                channel.invokeMethod("onError", "Failed to open UART")
            }
            return
        }

        Thread {
            while (isReading) {
                val buffer = ByteArray(17)
                val bytesRead = readUART(fd, buffer, buffer.size)



                if (bytesRead == 17 && validatePacket(buffer)) {
                    val data = buffer.toHexString()
                    mainHandler.post {
                        Log.d("RAW DATA", "RAW DATA : $data")
                        channel.invokeMethod("onData", mapOf("data" to data))
                    }
                } else if (bytesRead < 0) {
                    mainHandler.post {
                        channel.invokeMethod("onError", "Read error: $bytesRead")
                    }
                }
                Thread.sleep(1) // Adjust sleep time to handle 1000 packets per second
            }
            closeUART(fd)
        }.start()
    }


    fun stopReading() {
        isReading = false
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(" ") { byte ->
            String.format("%02X", byte)
        }
    }



    private fun validatePacket(packet: ByteArray): Boolean {
//        return packet[0] == 0x7E.toByte() &&
//                packet[1] == 0x0D.toByte() &&
//                packet[15] == 0xAA.toByte() &&
//                packet[16] == 0x7F.toByte()
        return true;
    }
}
