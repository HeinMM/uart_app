package com.example.uart_app

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.atomic.AtomicInteger


class UartManager(private val channel: MethodChannel) {
    companion object {
        init {
            System.loadLibrary("native-lib") // Load the native library (e.g., libuart-lib.so)
        }
    }

    // Declare the native methods
    private external fun openUART(devicePath: String, baudRate: Int): Int
    private external fun readUART(fd: Int, buffer: ByteArray, size: Int): Int
    private external fun writeUART(fd: Int, buffer: ByteArray): Int
    private external fun closeUART(fd: Int): Int


    private var uartThread: Thread? = null

    @Volatile private var isReading = true

    private val mainHandler = Handler(Looper.getMainLooper()) // Handler for main thread

    private var internalRCounter = AtomicInteger(0)
    private var errorCount = AtomicInteger(0)
    @Volatile private var isFirstTime = true
    private var controlValue = 0;
    private var tempValue = 0;
    @Volatile private var fd = -1;

    //OPEN PORT UART
    @Synchronized
    fun openUart(devicePath: String, baudRate: Int){
         fd = openUART(devicePath, baudRate)
        Log.d("fd value", "fd value: $fd")
        if (fd < 0) {
            mainHandler.post {
                channel.invokeMethod("onError", "Failed to open UART")
            }

        }else{
            mainHandler.post {
                channel.invokeMethod("info", "Open UART port successfully")
            }
        }

    }

    //READ PORT UART DATA
    fun startReadingPort() {
        if (fd < 0) {
            mainHandler.post {
                channel.invokeMethod("onError", "Failed to READ UART DATA")
            }
            return
        }
        else {
            mainHandler.post {
                channel.invokeMethod("info", "Read UART port successfully")
            }
        }

        uartThread =  Thread {

            while (isReading) {
                readData(fd);
                Thread.sleep(50)
            }

        }
        uartThread?.start()
    }

    //WRITE PORT UART DATA
    fun startWritePort(){
       var count = 0;
        while (count<1){ //adjust write count in here
           if (fd < 0) {
               mainHandler.post {
                   channel.invokeMethod("onError", "Failed to Write UART DATA ")
               }
               return
           }

           // Example CAN data
           val canData = constructPacket()
           val writeResult = writeUART(fd,canData)
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
                channel.invokeMethod("info", "Failed to Close UART DATA ")
            }
            return
        }
        isReading = false
        uartThread?.join()

        closeUART(fd);
        fd = -1
        isFirstTime = true
        Log.d("Test","fd value -> $fd");
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(" ") { byte ->
            String.format("%02X", byte)
        }
    }

    fun getLastFourDigits(value: Int): Int {
        return value % 10000
    }

    private fun validatePacket(packet: ByteArray): Boolean {
        return packet[0] == 0x7E.toByte() &&
                packet[1] == 0x0D.toByte() &&
                packet[15] == 0xAA.toByte() &&
                packet[16] == 0x7F.toByte()
        /*return true;*/
    }

   private fun readData(readFd: Int){

       val buffer = ByteArray(17)

       val bytesRead = readUART(readFd, buffer, buffer.size)

       Log.d("BYTE", "this is bytesRead -> $bytesRead")

       Log.d("RAW DATA", "RAW DATA : ${buffer.toHexString()}")
       if (bytesRead == 17 && validatePacket(buffer)) {
           val data = buffer.toHexString()

           tempValue = ((buffer[8].toInt() and 0xFF) shl 8) + (buffer[7].toInt() and 0xFF)
           val receivedRCounter = getLastFourDigits(tempValue)
           Log.d("receivedRCounter", "receivedRCounter : $receivedRCounter")
           Log.d("internalRCounter", "internalRCounter : ${internalRCounter.get()}")

           updateCounter(receivedRCounter)

           mainHandler.post {


               Log.d("RAW DATA", "RAW DATA : $data")

               channel.invokeMethod("onData", mapOf("counter" to internalRCounter.get() , "error" to errorCount.get()))
           }

       } else if (bytesRead < 0) {

           mainHandler.post {
               channel.invokeMethod("onError", "Read error: $bytesRead")
           }

       }
   }

    fun updateCounter(receivedRCounter : Int){
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

    fun constructPacket(): ByteArray {
        // Packet components
        val startOfPacket: Byte = 0x7E
        val length: Byte = 0x0D.toByte() // length
        val canId: ByteArray = byteArrayOf(0x0F, 0x00, 0x00, 0xFF.toByte())
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
    }


}