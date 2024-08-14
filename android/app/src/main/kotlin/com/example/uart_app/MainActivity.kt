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
    private lateinit var serialPort: SerialPort
    private var buffer = ByteArray(1024) // Initialize an empty buffer
    private val handler = Handler(Looper.getMainLooper())
    private var lastProcessedPacket: ByteArray? = null // To store the last processed packet
    private var packetCounter: Int = 0 // Counter to make each log unique

    private val recentPackets = LinkedHashSet<String>()
    private val MAX_RECENT_PACKETS = 10 // Adjust as needed for your use case

    private var internalRollingCounter = 0
    private var errorCount = 0
    private val MAX_ROLLING_COUNTER = 10000

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "openSerialPort") {
                val baudRate =  460800
                val flags =  0
                openSerialPort(result, baudRate, flags)
            } else {
                result.notImplemented()
            }
        }
    }

    private fun openSerialPort(result: MethodChannel.Result, baudRate: Int, flags: Int) {
        val serialFile = File("/dev/ttymxc1")
        val realBaud = baudRate
        if (!serialFile.exists()) {
            result.error("FILE_NOT_FOUND", "Serial port file not found", null)
            return
        }

        try {
            serialPort = SerialPort(serialFile, baudRate, flags)
            startReadingSerialPort()
            result.success("Serial port opened successfully")
        } catch (e: SecurityException) {
            result.error("PERMISSION_DENIED", "Permission denied to access the serial port", null)
        } catch (e: IOException) {
            result.error("IO_EXCEPTION", "Error opening serial port: ${e.message}", null)
        }
    }

    private fun processBuffer() {// End byte
        val packetLength = 17 // Packet length

        handler.post {
            while (buffer.size >= packetLength) {
                val packetStartIndex = findPacketStart()
                if (packetStartIndex == -1 || buffer.size - packetStartIndex < packetLength) {
                    break
                }

                // Extract the packet
                val packet = buffer.copyOfRange(packetStartIndex, packetStartIndex + packetLength)

                // Process the packet
                if (validatePacket(packet)) {
                    processPacket(packet)
                }


                // Update the buffer to remove the processed packet
                buffer = buffer.copyOfRange(packetStartIndex + packetLength, buffer.size)

                // Break the loop if buffer size is reduced, avoiding any double processing
                if (buffer.isEmpty()) {
                    break
                }
            }
        }

    }

    private fun startReadingSerialPort() {
//        Thread {
//            val readBuffer = ByteArray(1024)
//            while (true) {
//                try {
//                    val bytesRead = serialPort.read(readBuffer)
//
//                    if (bytesRead > 0) {
//                        synchronized(this) {
//                            buffer += readBuffer.copyOfRange(0, bytesRead)
//                            processBuffer()
//                        }
//                    }
//                } catch (e: IOException) {
//                    Log.e("SerialPort", "Error reading from serial port", e)
//                    break
//                }
//            }
//        }.start()

        Thread {
            val readBuffer = ByteArray(1024)
            while (true) {
                try {
                    val bytesRead = serialPort.read(readBuffer)

                    if (bytesRead > 0) {
                        // Log the raw bytes read
//                        val rawBytes = readBuffer.copyOfRange(0, bytesRead).joinToString(" ") { "%02X".format(it) }
                        //Log.d("SerialPort", "Raw bytes read: $rawBytes")

                        synchronized(this) {
                            buffer += readBuffer.copyOfRange(0, bytesRead)
                            processBuffer()
                        }
                    }
                } catch (e: IOException) {
                    Log.e("SerialPort", "Error reading from serial port", e)
                    break
                }
            }
        }.start()
    }



    private fun findPacketStart(): Int {
        for (i in buffer.indices) {
            if (buffer[i] == 0x7E.toByte()) {
                return i
            }
        }
        return -1
    }

    private fun validatePacket(packet: ByteArray): Boolean {
        return packet.size == 17 && packet[0] == 0x7E.toByte() && packet[16] == 0x7F.toByte()
    }



    private fun processPacket(packet: ByteArray) {


        // Ignore incomplete packets
        if (packet.size < 17) return



        // Check if the packet is a duplicate of the last processed packet
        if (lastProcessedPacket != null && lastProcessedPacket!!.contentEquals(packet)) {

            return
        }

        // Check for sequence progression
        val currentSequence = packet[7] // Assuming the sequence byte is at index 7
        val lastSequence = lastProcessedPacket?.get(7)

        if (lastSequence != null && currentSequence == lastSequence) {

            return
        }

        val rawPacketHex = packet.joinToString(" ") { "%02X".format(it) }
        Log.d("testing raw", "this is raw data : $rawPacketHex")

        // Update the last processed packet
        lastProcessedPacket = packet.copyOf()


        // Add to recent packets set and maintain size
        recentPackets.add(rawPacketHex)
        if (recentPackets.size > MAX_RECENT_PACKETS) {
            recentPackets.remove(recentPackets.first())
        }

        // Update the last processed packet
        lastProcessedPacket = packet.copyOf()

        // Increment the packet counter
        packetCounter++

        // Extract rolling counter from the packet (assuming DATA 1 is at index 7 and DATA 2 at index 8)
        val test7 = packet[7].toInt() and 0xFF
        val test8 = packet[8].toInt() and 0xFF
//        Log.d("test7", "test7: $test7")
//        Log.d("test8", "test8: $test8")
        val receivedCounter = (packet[7].toInt() and 0xFF) shl 8 or (packet[8].toInt() and 0xFF)

        if (receivedCounter == internalRollingCounter) {
            // Success: Rolling counters match
            Log.d("RollingCounter", "Success: Received R/Counter matches internal R/Counter.")
            internalRollingCounter = (internalRollingCounter + 1) % (MAX_ROLLING_COUNTER + 1)
        } else {
            // Failure: Rolling counters do not match
//            Log.d("RollingCounter", "Error: Received R/Counter $receivedCounter does not match internal R/Counter $internalRollingCounter.")
            internalRollingCounter = (receivedCounter + 1) % (MAX_ROLLING_COUNTER + 1)
            errorCount++
        }



        // Send this data to the Flutter side if needed
        handler.post {
            MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL).invokeMethod(
                "onDataReceived",
                mapOf("rawPacket" to rawPacketHex)
            )
        }


    }

    }


class SerialPort(private val device: File, private val baudRate: Int, private val flags: Int) {
    var inputStream: FileInputStream? = null
        private set
    var outputStream: FileOutputStream? = null
        private set

    init {
        openSerialPort()
    }

    private fun openSerialPort() {
        try {
            inputStream = FileInputStream(device)
            outputStream = FileOutputStream(device)
            configurePort()
        } catch (e: IOException) {
            Log.e("SerialPort", "Error opening serial port: ${e.message}")
            throw IOException("Cannot open serial port.")
        }
    }




    private fun configurePort() {
        try {
            val flagsString = when (flags) {
                0 -> "" // No additional flags
                else -> flags.toString() // Replace with appropriate flags
            }

            val process = Runtime.getRuntime().exec(
//                arrayOf("/bin/sh", "-c", "stty -F  raw  </dev/ttymxc1 $baudRate" ))
                "stty -F ${device.absolutePath} $baudRate raw -parenb cs8 -cstopb -ixon ")
            process.waitFor()
            if (process.exitValue() != 0) {
                throw IOException("Failed to configure serial port.")
            }
        } catch (e: IOException) {
            Log.e("SerialPort", "Error configuring serial port: ${e.message}")
            throw IOException("Cannot configure serial port.")
        } catch (e: InterruptedException) {
            Log.e("SerialPort", "Configuration interrupted: ${e.message}")
            throw IOException("Configuration interrupted.")
        }
    }


//    private fun flagsToString(flags: Int): String {
//
//        return when (flags) {
//            0 -> ""
//
//            else -> throw IllegalArgumentException("Unsupported flags value: $flags")
//        }
//    }

    @Throws(IOException::class)
    fun close() {
        inputStream?.close()
        outputStream?.close()
    }

    fun write(data: ByteArray) {
        outputStream?.write(data)
        outputStream?.flush()
    }

    fun read(buffer: ByteArray): Int {
        return inputStream?.read(buffer) ?: -1
    }
}




