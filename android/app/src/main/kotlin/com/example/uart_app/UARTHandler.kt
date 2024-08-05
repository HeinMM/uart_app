package com.example.uart_app

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface

import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Log



class UARTHandler(private val context: Context) {

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialDevice? = null
    private var receivedData: String = "No data....."
    val byteArray: ByteArray = byteArrayOf(
            0x7E.toByte(),
            0x0D.toByte(),
            0x18.toByte(),
            0xEF.toByte(),
            0x26.toByte(),
            0x23.toByte(),
            0x08.toByte(),
            0x03.toByte(),
            0x22.toByte(),
            0x20.toByte(),
            0x24.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0xAA.toByte(),
            0x7F.toByte()
    )

    

     // Define the TAG constant
    // private val TAG = "UARTHandler"

    // Connect to the USB serial device
    fun connectToDevice() {

        val usbDevices = usbManager.deviceList
        if (usbDevices.isNotEmpty()) {
            for ((_, device) in usbDevices) {
                val vendorId = device.vendorId
                val productId = device.productId

                // Replace with your device's vendorId and productId
                if (vendorId == YOUR_VENDOR_ID && productId == YOUR_PRODUCT_ID) {
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, usbManager.openDevice(device))
                    serialPort?.let {
                        it.open()
                        it.setBaudRate(460800)
                        it.setDataBits(UsbSerialInterface.DATA_BITS_8)
                        it.setStopBits(UsbSerialInterface.STOP_BITS_1)
                        it.setParity(UsbSerialInterface.PARITY_NONE)
                        it.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                        it.read(mCallback)
                    }
                    break
                }
            }
        }


    }

    // Callback to handle received data
    private val mCallback = object : UsbSerialInterface.UsbReadCallback {
        override fun onReceivedData(data: ByteArray) {

            parseData(byteArray)
        }
    }

    // Parse the received data according to the custom format
    private fun parseData(data: ByteArray) {
        if (data.size >= 17) {
            val byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

            val startOfPacket = byteBuffer.get()
            val length = byteBuffer.get()
            val canId = byteBuffer.int
            val dlc = byteBuffer.get()
            val packetData = ByteArray(8)
            byteBuffer.get(packetData)
            val fillerByte = byteBuffer.get()
            val endOfPacket = byteBuffer.get()

            // Example of extracting an R/Counter value from DATA
            val rCounterValue = ByteBuffer.wrap(packetData).order(ByteOrder.BIG_ENDIAN).int

            // Example internal R/Counter value
            val internalRCounterValue = 4


            // print("this is CAN DATA -> ${packetData}");
            Log.d("TAG222", "*****************your log message**********************")
            Log.d("TAG222", "${packetData}")

            

            // Check if R/Counter values match
            receivedData = if (rCounterValue == internalRCounterValue) {
                "Success: R/Counter values match."
            } else {
                "Fail: R/Counter values do not match."
            }
        } else {
            receivedData = "Error: Incomplete packet received."
        }
    }

    // Provide the received data to Flutter
    fun getReceivedData(): String {
        return receivedData
    }

    companion object {
        const val YOUR_VENDOR_ID = 0x1234 // actual vendor ID
        const val YOUR_PRODUCT_ID = 0x5678 // actual product ID
    }
}
