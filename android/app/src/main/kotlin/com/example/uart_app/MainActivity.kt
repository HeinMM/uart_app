package com.example.uart_app

import android.os.Bundle
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

    private fun configureSerialPort() {
        setBaudRate(460800)
        Thread {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytesRead = serialFileInputStream.read(buffer)
                    Log.d("CheckData", "type check data: $bytesRead")
                    if (bytesRead > 0) {
                        val data = String(buffer, 0, bytesRead)
                        Log.d("SerialPort", "Read data: $data")
                    }
                } catch (e: IOException) {
                    Log.e("SerialPort", "Error reading from serial port", e)
                    break
                }
            }
        }.start()
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

    private fun parseAndSendData(buffer: ByteArray, length: Int) {
        val data = buffer.copyOf(length)
        var index = 0

        while (index < data.size) {
            if (data.size - index < 16) break // Not enough data for a complete packet

            // Parse packet
            val startByte = data[index]
            val lengthByte = data[index + 1]
            val canId = data.copyOfRange(index + 2, index + 6)
            val dlc = data[index + 6]
            val dataBytes = data.copyOfRange(index + 7, index + 15)
            val fillerByte = data[index + 15]
            val endByte = data[index + 16]

            // Validate packet
            if (startByte == 0x7E.toByte() && endByte == 0x7F.toByte() && lengthByte == 13.toByte()) {
                // Construct a string representation of CAN ID and data for Flutter
                val canIdStr = canId.joinToString(separator = " ") { "%02X".format(it) }
                val dataStr = dataBytes.joinToString(separator = " ") { "%02X".format(it) }

                // Send data to Flutter
                MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL).invokeMethod(
                    "onDataReceived",
                    mapOf(
                        "canId" to canIdStr,
                        "data" to dataStr
                    )
                )
            }

            index += 17 // Move to the next packet
        }
    }

}
