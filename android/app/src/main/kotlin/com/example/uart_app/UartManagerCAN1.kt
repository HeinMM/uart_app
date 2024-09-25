package com.example.uart_app

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.atomic.AtomicInteger


class UartManagerCAN1(private val channel: MethodChannel) {
    companion object {
        init {
            System.loadLibrary("native-lib") // Load the native library (e.g., libuart-lib.so)
        }
    }

    // Declare the native methods
    private external fun can1OpenUART(devicePath: String, baudRate: Int): Int
    private external fun can1ReadUART(fd: Int, buffer: ByteArray, size: Int): Int
    private external fun can1WriteUART(fd: Int, buffer: ByteArray): Int
    private external fun can1CloseUART(fd: Int): Int
    private external fun sendAPPStateMessage(fd: Int): Int


    private var uartThread: Thread? = null

    @Volatile private var isReading = true

    private val mainHandler = Handler(Looper.getMainLooper()) // Handler for main thread

    private var internalRCounter = AtomicInteger(0)
    private var errorCount = AtomicInteger(0)
    @Volatile private var isFirstTime = true
    private var controlValue = 0
    private var tempValue = 0
    @Volatile private var fd = -1

    //OPEN PORT UART
    @Synchronized
    fun openUart(devicePath: String, baudRate: Int){


        fd = can1OpenUART(devicePath, baudRate)

        Log.d("fd value", "fd value: $fd")
        if (fd <= 0) {
            mainHandler.post {
                channel.invokeMethod("can1OnError", "Failed to open UART")
            }

        }else{
            mainHandler.post {
                channel.invokeMethod("can1Info", "Open UART port successfully")

            }

        }
        Thread.sleep(500)
        sendStateMessage()

    }

    private fun sendStateMessage() {
        if (fd < 0) {
            Log.e("UART", "UART is not opened.")
            return
        }

        val result = sendAPPStateMessage(fd)
        if (result < 0) {
            Log.e("UART", "Failed to send APP State message: $result")
        } else {
            Log.d("UART", "Successfully sent APP State message: $result bytes written")
        }
    }

    //READ PORT UART DATA
    fun startReadingPort() {

        if (fd < 0) {
            mainHandler.post {
                channel.invokeMethod("can1OnError", "Failed to READ UART DATA")
            }
            return
        }
        else {
            mainHandler.post {
                channel.invokeMethod("can1Info", "Read UART port successfully")
            }
        }

        uartThread =  Thread {

            while (isReading) {
                can1ReadData(fd)
            }

        }
        uartThread?.start()
    }

    private fun can1ReadData(readFd: Int){

        val buffer = ByteArray(17)

        val bytesRead = can1ReadUART(readFd, buffer, buffer.size)



         if(bytesRead == 17 && can1ValidatePacket(buffer)){

            can1Reader(readFd,buffer,bytesRead ) // CAN 1 </dev/ttymxc1>

        }
        if (bytesRead < 0) {

            mainHandler.post {
                channel.invokeMethod("can1OnError", "Read error: $bytesRead")
            }

        }

    }

    private fun can1ValidatePacket(packet: ByteArray): Boolean {
        return packet[0] == 0x02.toByte() &&
                packet[14] == 0x00.toByte() &&
                packet[15] == 0x00.toByte() &&
                packet[16] == 0x04.toByte()
        /*return true;*/
    }


    private fun can1Reader(readFd: Int, buffer: ByteArray, bytesRead: Int){

        Log.d("Can1Testing", "Can 1 is working")

        /*if (true) {*/
        val data = buffer.toHexString()

        tempValue = ((buffer[7].toInt() and 0xFF) shl 8) + (buffer[6].toInt() and 0xFF)
        val receivedRCounter = getLastFourDigits(tempValue)
        Log.d("receivedRCounter", "receivedRCounter : $receivedRCounter")
        Log.d("internalRCounter", "internalRCounter : ${internalRCounter.get()}")

        updateCounter(receivedRCounter)

        mainHandler.post {

            Log.d("RAW DATA", "CAN 1 RAW DATA : ${buffer.toHexString()}")

            channel.invokeMethod("can1OnData", mapOf("counter" to internalRCounter.get() , "error" to errorCount.get()))
        }


    }

    //WRITE PORT UART DATA
    fun startWritePort(){
       var count = 0
        while (count<1){ //adjust write count in here
           if (fd < 0) {
               mainHandler.post {
                   channel.invokeMethod("can1OnError", "Failed to Write UART DATA ")
               }
               return
           }

           // Example CAN data
           val canData = constructPacket()
           val writeResult = can1WriteUART(fd,canData)
           Log.e("Write", "write value -> $writeResult")
           if (writeResult == -1) {
               Log.e("Write", "Failed to write data")
           } else {
               Log.d("Write", "Data written successfully")
           }
           count++
       }

    }

    //CLOSE PORT AND STOP READ DATA
    fun stopReading() {
        if (fd < 0) {
            mainHandler.post {
                channel.invokeMethod("can1Info", "Failed to Close UART DATA ")
            }
            return
        }
        isReading = false
        uartThread?.join()

        can1CloseUART(fd)
        fd = -1
        isFirstTime = true
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(" ") { byte ->
            String.format("%02X", byte)
        }
    }

    private fun getLastFourDigits(value: Int): Int {
        return value % 10000
    }





    private fun updateCounter(receivedRCounter : Int){
        if (internalRCounter.get() >= 10000) {
            controlValue++
            internalRCounter.set(0)
            isFirstTime = true
        }

        if (receivedRCounter == internalRCounter.get()) {
            internalRCounter.incrementAndGet()
        } else {

            internalRCounter.set(receivedRCounter + 1)
            //errorCount++

            if (isFirstTime){

                isFirstTime = false

            }else{
                errorCount.incrementAndGet()

            }
        }
    }

    /*fun constructPacket(): ByteArray {
        // Packet components
        val startOfPacket: Byte = 0x7E
        val length: Byte = 0x0D.toByte() // length
        val canId: ByteArray = byteArrayOf(0x18, 0xF0.toByte(), 0x05, 0x03)
//        val canId: ByteArray = byteArrayOf(0x0F, 0x00, 0x00, 0xFF.toByte())
        val dlc: Byte = 0x08
        val data: ByteArray = byteArrayOf(0x01, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val endOfPacket: Byte = 0xAA.toByte()
        val endOfPacket2: Byte = 0x7F

        // Construct the packet
        return byteArrayOf(
            startOfPacket,
            length,
            *canId,
            dlc,
            *data,
            endOfPacket, endOfPacket2
        )
    }*/

    private fun constructPacket(): ByteArray {
        // Packet components
        val stx: Byte = 0x02
        //val canId: ByteArray = byteArrayOf(0xF0.toByte(), 0x00, 0x00, 0xFF.toByte())
        val canId: ByteArray = byteArrayOf(0x18, 0xF0.toByte(), 0x05, 0x03)
        val dlc: Byte = 0x08
        //val data: ByteArray = byteArrayOf(0x01, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val data: ByteArray = byteArrayOf(0x01, 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        val endOfPacket1: Byte = 0x00
        val endOfPacket2: Byte = 0x00
        val endOfPacket3: Byte = 0x04

        // Construct the packet
        return byteArrayOf(
            stx,
            *canId,
            dlc,
            *data,
            endOfPacket1, endOfPacket2, endOfPacket3
        )
    }


}