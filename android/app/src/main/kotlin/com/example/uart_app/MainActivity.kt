package com.example.uart_app


import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.util.Log
import android.content.Context

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.uart_app/uart"
    private lateinit var uartHandler: UARTHandler

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        uartHandler = UARTHandler(this)
        uartHandler.connectToDevice()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                if (call.method == "getData") {
                    val data = uartHandler.getReceivedData()
                    result.success(data)
                } else {
                    result.notImplemented()
                }
            }
    }
}
