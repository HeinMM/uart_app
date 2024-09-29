package com.example.uart_app

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.atomic.AtomicInteger
import java.lang.Thread


class UartManagerCAN2(private val channel: MethodChannel) {
    companion object {
        init {
            System.loadLibrary("native-lib") // Load the native library (e.g., libuart-lib.so)
        }
    }

    // Declare the native methods
    private external fun can2OpenUART(devicePath: String, baudRate: Int): Int
    private external fun can2ReadUART(fd: Int, buffer: ByteArray, size: Int): Int
    private external fun can2WriteUART(fd: Int, buffer: ByteArray): Int
    private external fun can2CloseUART(fd: Int): Int
    private external fun can2SendAPPStateMessage(fd: Int): Int


    private var uartThread: Thread? = null

    @Volatile private var isReading = true

    private val mainHandler = Handler(Looper.getMainLooper()) // Handler for main thread

    private var internalRCounter = AtomicInteger(0)
    private var errorCount = AtomicInteger(0)
    @Volatile private var isFirstTime = true
    private var controlValue = 0;
    private var tempValue = 0;
    @Volatile private var fd = -1;

    private var incompleteData = ByteArray(0)

    //OPEN PORT UART
    @Synchronized
    fun openUart(devicePath: String, baudRate: Int){

        fd = can2OpenUART(devicePath, baudRate)
        Log.d("fd value", "fd value: $fd")
        if (fd <= 0) {
            mainHandler.post {
                channel.invokeMethod("onError", "Failed to open UART")
            }

        }else{
            mainHandler.post {
                channel.invokeMethod("info", "Open UART port successfully")

            }

        }
        Thread.sleep(500)
        sendStateMessage()

    }

    fun sendStateMessage() {
        if (fd < 0) {
            Log.e("UART", "UART is not opened.")
            return
        }

        val result = can2SendAPPStateMessage(fd)
        if (result < 0) {
            Log.e("UART", "Failed to send APP State message: $result")
        } else {
            Log.d("UART", "Successfully sent APP State message: $result bytes written")
        }
    }

    //READ PORT UART DATA
    fun startReadingPort() {
        Log.d("Done", "This is testing done")
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

            try {
                while (isReading && !Thread.currentThread().isInterrupted) {
                    readData(fd);
                    Thread.sleep(50)
                }
            } catch (e: InterruptedException){
                Log.d("UART", "Thread interrupted, stopping reading.")
                        Thread.currentThread().interrupt()
            }

        }
        uartThread?.start()
    }

    /*private fun readData(readFd: Int){

        val buffer = ByteArray(256)

        val bytesRead = can2ReadUART(readFd, buffer, buffer.size)

        if (bytesRead > 0) {
            // Log the raw data before processing
            Log.d("RAW UART DATA", "Received: ${buffer.take(bytesRead).toByteArray().toHexString()}")
        }



        if (bytesRead == 17 && can2ValidatePacket(buffer)) {

            can2Reader(readFd,buffer,bytesRead ) // CAN 2 </dev/ttymxc2>

        }
        if (bytesRead < 0) {

            val errorMsg = "Error during UART read: $bytesRead"
            Log.e("UART", errorMsg)

            mainHandler.post {
                channel.invokeMethod("onError", "Read error: $bytesRead")
            }

        }

    }*/

    /*private fun readData(readFd: Int) {
        val buffer = ByteArray(1020) // Larger buffer to capture more data
        val bytesRead = can2ReadUART(readFd, buffer, buffer.size)
        Log.d("Testing UART DATA", "bytesRead = ${bytesRead}")

        if (bytesRead > 0) {
            Log.d("RAW UART DATA", "Received: ${buffer.take(bytesRead).toByteArray().toHexString()}")
            var i = 0
            while (i + 17 <= bytesRead) {
                // Find the start of a packet (0x7E)
                if (buffer[i] == 0x7E.toByte() && i + 16 < bytesRead) {
                    val packet = buffer.copyOfRange(i, i + 17)

                    // Validate and process the packet
                    if (can2ValidatePacket(packet)) {
                        Log.d("RAW UART DATA", "Received packet: ${packet.toHexString()}")
                        can2Reader(readFd, packet, packet.size)
                    } else {
                        Log.d("UART ERROR", "Invalid packet")
                        mainHandler.post {
                            channel.invokeMethod("onError", "Invalid packet")
                        }
                    }

                    // Move to the next packet
                    i += 17
                } else {
                    // Skip to the next byte if no packet start found
                    i++
                }
            }
        } else if (bytesRead < 0) {
            Log.d("UART ERROR", "Read error: $bytesRead")
            mainHandler.post {
                channel.invokeMethod("onError", "Read error: $bytesRead")
            }
        }
    }*/

    private fun readData(readFd: Int) {
        val buffer = ByteArray(119) // Larger buffer size
        val bytesRead = can2ReadUART(readFd, buffer, buffer.size)



        if (bytesRead > 0) {
            Log.d("RAW UART DATA", "Received: ${buffer.take(bytesRead).toByteArray().toHexString()}")
            // Combine the previous incomplete data with the newly read data
            val newData = incompleteData + buffer.copyOf(bytesRead)

            // Process complete packets from the combined data
            var currentIndex = 0
            while (currentIndex <= newData.size - 17) {  // 17 is the packet size
                if (newData[currentIndex] == 0x7E.toByte() && newData[currentIndex + 16] == 0x7F.toByte()) {
                    // Extract the full packet
                    val packet = newData.copyOfRange(currentIndex, currentIndex + 17)

                    // Process the complete packet
                    processPacket(packet)

                    // Move to the next potential packet
                    currentIndex += 17
                } else {
                    currentIndex++
                }
            }

            // Store any leftover (incomplete) data for the next read
            incompleteData = newData.copyOfRange(currentIndex, newData.size)
        }
    }

    private fun processPacket(packet: ByteArray) {
        if (can2ValidatePacket(packet)) {
            can2Reader(fd, packet, packet.size)
        }
    }



    private fun can2Reader(readFd: Int, buffer: ByteArray, bytesRead: Int){

        Log.d("Can2Testing", "Can 2 is working")

        /*if (true) {*/
        val data = buffer.toHexString()

        tempValue = ((buffer[8].toInt() and 0xFF) shl 8) + (buffer[7].toInt() and 0xFF)
        val receivedRCounter = tempValue
            //val receivedRCounter = getLastFourDigits(tempValue)
        Log.d("receivedRCounter", "receivedRCounter : $receivedRCounter")
        Log.d("internalRCounter", "internalRCounter : ${internalRCounter.get()}")
        Log.d("eError Count", "ErrorCounter : ${errorCount.get()}")

        updateCounter(receivedRCounter)

        mainHandler.post {

            Log.d("RAW DATA", "CAN 2 RAW DATA : ${buffer.toHexString()}")

            channel.invokeMethod("onData", mapOf("counter" to internalRCounter.get() , "error" to errorCount.get()))
        }


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

            Log.d("UART SYNC ERROR", "Expected ${internalRCounter.get()}, but received $receivedRCounter")
            internalRCounter.set(receivedRCounter + 1)
            //errorCount++

            if (isFirstTime){

                isFirstTime = false

            }else{
                errorCount.incrementAndGet()
                isReading =  false
            }
        }
    }

    private fun can2ValidatePacket(packet: ByteArray): Boolean {
        if (packet.size != 17) return false

        return packet[0] == 0x7E.toByte() &&
                packet[1] == 0x0D.toByte() &&
                packet[15] == 0xAA.toByte() &&
                packet[16] == 0x7F.toByte()
        /*return true;*/
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
            val writeResult = can2WriteUART(fd,canData)
            Log.e("Write", "write value -> $writeResult")
            if (writeResult == -1) {
                Log.e("Write", "Failed to write data")
            } else {
                Log.d("Write", "Data written successfully")
            }
            count++
        }

    }



    private fun ByteArray.toHexString(): String {
        return joinToString(" ") { byte ->
            String.format("%02X", byte)
        }
    }

    /*private fun getLastFourDigits(value: Int): Int {
        return value % 10000
    }*/







    fun constructPacket(): ByteArray {
        // Packet components
        val startOfPacket: Byte = 0x7E
        val length: Byte = 0x0D.toByte() // length
        val canId: ByteArray = byteArrayOf(0x18, 0xF0.toByte(), 0x05, 0x03)
//        val canId: ByteArray = byteArrayOf(0x0F, 0x00, 0x00, 0xFF.toByte())
        val dlc: Byte = 0x08
        val data: ByteArray = byteArrayOf(0x02, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00)
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

    //CLOSE PORT AND STOP READ DATA
    fun stopReading() {
        Log.d("Test","fd value -> $fd");
        if (fd < 0) {
            mainHandler.post {
                channel.invokeMethod("can2Info", "Failed to Close UART DATA ")
            }
            return
        }
        isReading = false
        uartThread?.interrupt()  // Interrupt the reading thread safely

        can2CloseUART(fd)
        fd = -1
        isFirstTime = true
    }


}