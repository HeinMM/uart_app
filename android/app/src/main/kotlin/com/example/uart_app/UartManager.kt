package com.example.uart_app

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.plugin.common.MethodChannel
import java.io.File


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
    private var uartThread: Thread? = null

    private var isReading = true

    private val mainHandler = Handler(Looper.getMainLooper()) // Handler for main thread

    var internalRCounter = 0
    var errorCount = 0
    var isFirstTime = true


    fun startReading(devicePath: String, baudRate: Int) {
        val fd = openUART(devicePath, baudRate)
        if (fd < 0) {
            mainHandler.post {
                channel.invokeMethod("onError", "Failed to open UART")
            }
            return
        }

        uartThread =  Thread {
            while (isReading) {
                val buffer = ByteArray(17)
                val bytesRead = readUART(fd, buffer, buffer.size)

//                Log.d("bytesRead", "bytesRead : $bytesRead")

                if (bytesRead == 17 && validatePacket(buffer)) {
                    val data = buffer.toHexString()

//                    val receivedRCounter = (data[8].toString().toInt(16) shl 8) + data[7].toString().toInt(16)
                    val receivedRCounter = ((buffer[8].toInt() and 0xFF) shl 8) + (buffer[7].toInt() and 0xFF)


                    if (receivedRCounter == internalRCounter) {
                        internalRCounter++
                    } else {

                        internalRCounter = receivedRCounter + 1
                        //errorCount++

                        if (isFirstTime){

                            isFirstTime = false

                        }else{
                            errorCount++
//                            Log.d("Error count", "Error detected! Error count : $errorCount")
                        }



                    }


                    if (internalRCounter > 10000) {
                        internalRCounter = 0
                    }

                    // Display the current internal R/Counter value

//                    Log.d("Internal R/Counter", "Current Internal R/Counter : $internalRCounter")



                    mainHandler.post {


                        Log.d("RAW DATA", "RAW DATA : $data")
//                        Log.d("Error count", "Error detected! Error count : $errorCount")
//                        Log.d("Internal R/Counter", "Current Internal R/Counter : $internalRCounter")

                        channel.invokeMethod("onData", mapOf("counter" to internalRCounter , "error" to errorCount))
                    }

                } else if (bytesRead < 0) {

                    mainHandler.post {
                        channel.invokeMethod("onError", "Read error: $bytesRead")
                    }

                }
                Thread.sleep(5) // sleep time to handle 1000 packets per second
            }
            closeUART(fd)
        }
        uartThread?.priority = Thread.MAX_PRIORITY
        uartThread?.start()
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
        return packet[0] == 0x7E.toByte() &&
                packet[1] == 0x0D.toByte() &&
                packet[15] == 0xAA.toByte() &&
                packet[16] == 0x7F.toByte()
//        return true;
    }




}