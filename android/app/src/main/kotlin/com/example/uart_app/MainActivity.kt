package com.example.uart_app

import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private lateinit var uartManager: UartManager
    private val CHANNEL = "com.example.uart_app/uart"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        flutterEngine?.dartExecutor?.binaryMessenger?.let {
            MethodChannel(it, CHANNEL).apply {
                setMethodCallHandler { call, result ->
                    when (call.method) {

                        "openUart" -> {
                            val devicePath = call.argument<String>("devicePath")
                            val baudRate = call.argument<Int>("baudRate") ?: 460800
                            uartManager = UartManager(this)
                            uartManager.openUart(devicePath ?: "/dev/ttymxc1", baudRate)
                        }

                        "startReading" -> {
                            //uartManager = UartManager(this)
                            uartManager.startReadingPort()
                        }

                        "writeData" -> {
                            uartManager.startWritePort()
                            /*result.success(null)*/
                        }

                        "stopReading" -> {
                            uartManager.stopReading()
                            result.success(null)
                        }

                        else -> result.notImplemented()
                    }
                }
            }
        }
    }
}
