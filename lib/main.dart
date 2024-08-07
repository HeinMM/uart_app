// ignore_for_file: prefer_is_empty, prefer_final_fields, avoid_print, avoid_function_literals_in_foreach_calls, prefer_interpolation_to_compose_strings
import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:usb_serial/usb_serial.dart';
import 'package:usb_serial/transaction.dart';
import 'dart:math';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'UART TEST APP',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'UART TEST APP'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  // UsbPort? _port;
  // String _status = "Idle";
  // String _receivedData = "No data received";
  // int internalRCounterValue = 0x03222024; // Example value
  // StreamSubscription<Uint8List>? _subscription;
  // Transaction<Uint8List>? _transaction;
  // List<Uint8List> _dataBuffer = [];

  // Timer? _uiUpdateTimer;

  // bool isTesting = true;

  // @override
  // void initState() {
  //   super.initState();
  //   _dataBuffer = generateRandomPackets(10000000);
  //   connectToUART();
  //   startUIUpdateTimer();
  // }

  // @override
  // void dispose() {
  //   _subscription?.cancel();
  //   _transaction?.dispose();
  //   _port?.close();
  //   _uiUpdateTimer?.cancel();
  //   super.dispose();
  // }

  // // void generateAndPrintRandomPackets() {
  // //   List<Uint8List> packets = generateRandomPackets(10);
  // //   for (int i = 0; i < packets.length; i++) {
  // //     print("Packet ${i + 1}: ${packets[i]}");
  // //   }
  // // }

  // Uint8List generateRandomPacket() {
  //   final random = Random();

  //   final List<int> packet = [
  //     0x7E, // Start of packet
  //     0x0D, // Length (fixed)
  //     random.nextInt(256), // CAN ID (4 bytes, Big Endian)
  //     random.nextInt(256),
  //     random.nextInt(256),
  //     random.nextInt(256),
  //     random.nextInt(256), // DLC (1 byte)
  //     random.nextInt(256), // DATA (8 bytes)
  //     random.nextInt(256),
  //     random.nextInt(256),
  //     random.nextInt(256),
  //     random.nextInt(256),
  //     random.nextInt(256),
  //     random.nextInt(256),
  //     random.nextInt(256), // Filler byte
  //     0xAA, // End of packet
  //     0x7F
  //   ];

  //   return Uint8List.fromList(packet);
  // }

  // List<Uint8List> generateRandomPackets(int count) {
  //   return List<Uint8List>.generate(count, (_) => generateRandomPacket());
  // }

  // void onFabPressed() {
  //   // Handle FAB press action
  //   // This can be any action you want to perform
  //   setState(() {
  //     isTesting = !isTesting;
  //   });

  //   if (isTesting == false) {
  //     setState(() {
  //       _dataBuffer = [];
  //       print("Stop testing");
  //     });
  //   } else {
  //     setState(() {
  //       _dataBuffer = generateRandomPackets(10000);
  //       connectToUART();
  //       print("Start testing");
  //     });
  //   }
  // }

  // void connectToUART() async {
  //   List<UsbDevice> devices = await UsbSerial.listDevices();
  //   if (devices.isEmpty) {
  //     setState(() {
  //       _status = "No USB devices found";
  //     });
  //     return;
  //   }

  //   _port = await devices[0].create();
  //   bool openResult = await _port!.open();
  //   if (!openResult) {
  //     setState(() {
  //       _status = "Failed to open port";
  //     });
  //     return;
  //   }

  //   await _port!.setDTR(true);
  //   await _port!.setRTS(true);
  //   await _port!.setPortParameters(
  //     460800,
  //     UsbPort.DATABITS_8,
  //     UsbPort.STOPBITS_1,
  //     UsbPort.PARITY_NONE,
  //   );

  //   _transaction = Transaction.terminated(
  //     _port!.inputStream!,
  //     Uint8List.fromList(
  //         [0x7F]), // Assume 0x7F as end of packet for splitting data
  //   );

  //   _subscription = _transaction!.stream.listen((Uint8List data) {
  //     _dataBuffer.add(data);
  //   });

  //   setState(() {
  //     _status = "Connected to ${devices[0].productName}";
  //   });
  // }

  // void startUIUpdateTimer() {
  //   _uiUpdateTimer = Timer.periodic(const Duration(milliseconds: 100), (timer) {
  //     if (_dataBuffer.isNotEmpty) {
  //       parseData(_dataBuffer.removeAt(0));
  //     }
  //   });
  // }

  // void parseData(Uint8List data) {
  //   if (data.length >= 16) {
  //     int startOfPacket = data[0];
  //     int length = data[1];
  //     int canId = (data[2] << 24) | (data[3] << 16) | (data[4] << 8) | data[5];
  //     int dlc = data[6];
  //     Uint8List packetData = data.sublist(7, 15);
  //     int fillerByte = data[15];
  //     int endOfPacket = data[16];

  //     int rCounterValue = (packetData[0] << 24) |
  //         (packetData[1] << 16) |
  //         (packetData[2] << 8) |
  //         packetData[3];

  //     String result =
  //         rCounterValue == internalRCounterValue ? "Success" : "Fail";

  //     setState(() {
  //       _receivedData = "Parsed Data:\n"
  //           "StartOfPacket: $startOfPacket\n"
  //           "Length: $length\n"
  //           "CAN ID: $canId\n"
  //           "DLC: $dlc\n"
  //           "Packet Data: $packetData\n"
  //           "Filler Byte: $fillerByte\n"
  //           "End Of Packet: $endOfPacket\n"
  //           "R/Counter Value: $rCounterValue\n"
  //           "Result: $result";
  //     });
  //   }
  // }

  static const platform = MethodChannel('uart_channel');
  String _data = 'No data';

  @override
  void initState() {
    super.initState();
    platform.setMethodCallHandler((call) async {
      if (call.method == 'onDataReceived') {
        setState(() {
          _data = call.arguments.toString();
        });
      }
    });
    _startReading();
  }

  Future<void> _startReading() async {
    try {
      await platform.invokeMethod('startReading');
    } on PlatformException catch (e) {
      print("Failed to start reading: '${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),

      body: Center(
        child: Text(_data),
      ),
      // body: Center(
      //   child: Column(
      //     mainAxisAlignment: MainAxisAlignment.center,
      //     children: <Widget>[
      //       Text('Status: $_status'),
      //       const SizedBox(height: 20),
      //       Text(_receivedData),
      //     ],
      //   ),
      // ),
      // floatingActionButton: FloatingActionButton(
      //   onPressed: onFabPressed,
      //   tooltip: 'Action',
      //   child: const Icon(Icons.stop_circle_outlined),
      // ),
    );
  }
}
