package com.example.uart_app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.uart_app/uart"
    private lateinit var serialFileInputStream: FileInputStream
    private lateinit var serialFileOutputStream: FileOutputStream
    private var buffer = ByteArray(0) // Initialize an empty buffer
    private val handler = Handler(Looper.getMainLooper()) // Handler to post to the main thread

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "openSerialPort") {
                openSerialPort(result)
            } else {
                result.notImplemented()
            }
        }
    }

    private fun openSerialPort(result: MethodChannel.Result) {
        val serialFile = File("/dev/ttymxc1")
        if (!serialFile.exists()) {
            result.error("FILE_NOT_FOUND", "Serial port file not found", null)
            return
        }

        try {
            serialFileInputStream = FileInputStream(serialFile)
            serialFileOutputStream = FileOutputStream(serialFile)
            configureSerialPort()
            result.success("Serial port opened successfully")
        } catch (e: SecurityException) {
            result.error("PERMISSION_DENIED", "Permission denied to access the serial port", null)
        } catch (e: IOException) {
            result.error("IO_EXCEPTION", "Error opening serial port: ${e.message}", null)
        }
    }

    private fun setBaudRate(baudRate: Int) {
        try {
            val process = Runtime.getRuntime().exec("stty -F /dev/ttymxc1 $baudRate")
            process.waitFor()
            if (process.exitValue() != 0) {
                Log.e("SerialPort", "Failed to set baud rate")
            } else {
                Log.d("SerialPort", "Baud rate set to $baudRate")
            }
        } catch (e: IOException) {
            Log.e("SerialPort", "Error setting baud rate", e)
        } catch (e: InterruptedException) {
            Log.e("SerialPort", "Error setting baud rate", e)
        }
    }

    private fun configureSerialPort() {
        setBaudRate(460800)
    Thread {
        val readBuffer = ByteArray(1024 * 10) // Read buffer for incoming data
        while (true) {
            try {
                val bytesRead = serialFileInputStream.read(readBuffer)
                if (bytesRead > 0) {
                    // Append the new data to the existing buffer
                    
                    synchronized(this) {
                        buffer = buffer.copyOf(buffer.size + bytesRead)
                        System.arraycopy(readBuffer, 0, buffer, buffer.size - bytesRead, bytesRead)
                        
                        // Process the data
                        processBuffer()
                        
                        // Limit buffer size to prevent excessive growth
                        if (buffer.size > 1024 * 10) { // Example: limit buffer to 10 KB
                            buffer = buffer.copyOfRange(buffer.size - 1024 * 2, buffer.size) // Keep the last 2 KB
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("SerialPort", "Error reading from serial port", e)
                break
            }
        }
    }.start()
    }

    

    private fun processBuffer() {
        // Ensure processing and sending data to the UI thread
        handler.post {
            val packetLength = 17 // Total fixed length
            while (buffer.size >= packetLength) {
                val packetStartIndex = findPacketStart()
                if (packetStartIndex == -1 || buffer.size - packetStartIndex < packetLength) {
                    break
                }

                val packet = buffer.copyOfRange(packetStartIndex, packetStartIndex + packetLength)
                processPacket(packet)

                // Update the buffer to keep only remaining data
                buffer = buffer.copyOfRange(packetStartIndex + packetLength, buffer.size)
            }
        }

        
    }

    

    private fun findPacketStart(): Int {
        // Look for the start of the packet
        for (i in 0 until buffer.size - 1) {
            if (buffer[i + 1] == 0x0D.toByte()) {
                return i
            }
        }
        return -1
    }

    private fun processPacket(packet: ByteArray) {
        if (packet.size < 17) return // Ignore incomplete packets

        val lengthByte = packet[1]
        if (lengthByte != 0x0D.toByte()) return // Invalid packet length

        // Extract CAN ID in big-endian format (4 bytes)
        // Extract CAN ID in big-endian format (4 bytes)
    val canIdBytes = packet.copyOfRange(2, 6)
        val canId = (canIdBytes[0].toInt() shl 24) or
                (canIdBytes[1].toInt() shl 16) or
                (canIdBytes[2].toInt() shl 8) or
                (canIdBytes[3].toInt())

        // Debugging: Log the raw CAN ID bytes and resulting integer value
    Log.d("SerialPort_Testing", "CAN ID Bytes: ${canIdBytes.joinToString(" ") { "%02X".format(it) }}")
    Log.d("SerialPort_Testing", "CAN ID Integer: $canId")
    

        val dlc = packet[6]
        val dataArray = packet.copyOfRange(7, 15)
        val fillerByte = packet[15]
        val endByte = packet[16]

        val canIdStr = "%08X".format(canId) // Format CAN ID as an 8-digit hexadecimal string
        val dataStr = dataArray.joinToString(separator = " ") { "%02X".format(it) }

        // Ensure this operation is on the main thread
        handler.post {
            MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL).invokeMethod(
                "onDataReceived",
                mapOf(
                    "canId" to canIdStr,
                    "data" to dataStr
                )
            )
        }
    }
}
