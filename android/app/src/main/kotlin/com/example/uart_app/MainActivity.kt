package com.example.uart_app

import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private lateinit var uartManagerCAN1: UartManagerCAN1
    private lateinit var uartManagerCAN2: UartManagerCAN2


    private val CHANNEL = "com.example.uart_app/uart"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        flutterEngine?.dartExecutor?.binaryMessenger?.let {
            MethodChannel(it, CHANNEL).apply {
                setMethodCallHandler { call, result ->
                    when (call.method) {
                    ///////////////////////CAN 1 Channel////////////////////////////////
                        "can1OpenUart" -> {
                            val devicePath = call.argument<String>("devicePath")?: "/dev/ttymxc1"
                            val baudRate = call.argument<Int>("baudRate") ?: 115200
                            uartManagerCAN1 = UartManagerCAN1(this)
                            uartManagerCAN1.openUart(devicePath , baudRate)
                        }

                        "can1StartReading" -> {
                            uartManagerCAN1.startReadingPort()
                            uartManagerCAN2.startReadingPort()
                        }

                        "can1WriteData" -> {
                            Log.d("Testing", "work from write Data")
                            uartManagerCAN1.startWritePort()
                        }

                        "can1StopReading" -> {

                            uartManagerCAN1.stopReading()
                            result.success(null)
                        }

                        ///////////////////////CAN 2 Channel////////////////////////////////

                        "can2OpenUart" -> {
                            val devicePath = call.argument<String>("devicePath")?: "/dev/ttymxc2"
                            val baudRate = call.argument<Int>("baudRate") ?: 460800
                            uartManagerCAN2 = UartManagerCAN2(this)
                            uartManagerCAN2.openUart(devicePath , baudRate)
                        }

                        "can2StartReading" -> {
                            uartManagerCAN2.startReadingPort()
                        }

                        "can2WriteData" -> {
                            Log.d("Testing", "work from write Data")
                            uartManagerCAN2.startWritePort()
                        }

                        "can2StopReading" -> {

                            uartManagerCAN2.stopReading()
                            result.success(null)
                        }

                        else -> result.notImplemented()
                    }
                }
            }
        }
    }
}
