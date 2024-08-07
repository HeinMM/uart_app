package com.example.uart_app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : FlutterActivity() {

    private val uartPortPath = "/dev/ttymxc1"
    private val baudRate = 460800
    private lateinit var inputStream: FileInputStream
    private lateinit var outputStream: FileOutputStream
    private val handler = Handler(Looper.getMainLooper())

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "uart_channel").setMethodCallHandler { call, result ->
            if (call.method == "startReading") {
                openUartPort()
                result.success("Reading started")
            } else {
                result.notImplemented()
            }
        }
    }

    private fun openUartPort() {
        val uartPort = File(uartPortPath)
        try {
            if (!uartPort.canRead() || !uartPort.canWrite()) {
                val suProcess = Runtime.getRuntime().exec("su")
                suProcess.outputStream.write("chmod 666 ${uartPort.absolutePath}\nexit\n".toByteArray())
                suProcess.waitFor()
            }
            inputStream = FileInputStream(uartPort)
            outputStream = FileOutputStream(uartPort)
            startReading()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startReading() {
        Thread {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val data = buffer.copyOfRange(0, bytesRead)
                        handler.post {
                            flutterEngine?.dartExecutor?.binaryMessenger?.let {
                                MethodChannel(it, "uart_channel")
                                    .invokeMethod("onDataReceived", data.toList())
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        inputStream.close()
        outputStream.close()
    }
}