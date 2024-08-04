// ignore_for_file: prefer_is_empty, prefer_final_fields, avoid_print, avoid_function_literals_in_foreach_calls, prefer_interpolation_to_compose_strings
import 'package:flutter/material.dart';
import 'dart:typed_data';
import 'services/uart_service.dart';

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
  late UartService _uartService;
  List<String> _receivedData = [];
  int internalRCounterValue = 0; // Initial internal R/Counter value
  final int internalRCounterModValue = 10001; // Modulo value
  final Uint8List uartData = Uint8List.fromList([
    0x7E,
    0x0D,
    0x18,
    0xEF,
    0x26,
    0x23,
    0x08,
    0x03,
    0x22,
    0x20,
    0x24,
    0x00,
    0x00,
    0x00,
    0x00,
    0xAA,
    0x7F
  ]);

  @override
  void initState() {
    super.initState();
    _uartService = UartService();
    _openPortAndListen();
  }

  Future<void> _openPortAndListen() async {
    handleBinaryData(uartData);
    print("{$uartData}");

    // bool result = await _uartService
    //     .open('/dev/ttymxc1'); // Replace with your actual port name
    // if (result) {
    //   _uartService.inputStream.listen((data) {
    //     handleBinaryData(uartData);

    //   });
    // } else {
    //   print('Failed to open port');
    // }
  }

  @override
  void dispose() {
    _uartService.close();
    super.dispose();
  }

  String byteDataToHexString(ByteData byteData) {
  List<String> hexStrings = [];
  for (int i = 0; i < byteData.lengthInBytes; i++) {
    hexStrings.add('0x${byteData.getUint8(i).toRadixString(16).toUpperCase().padLeft(2, '0')}');
  }
  return hexStrings.join(' ');
}

  void handleBinaryData(Uint8List data) {
    int offset = 0;
    const int packetSize = 17;

    while (offset + packetSize <= data.length) {
      // Extract the packet
      int startOfPacket = data[offset];
      ByteData canId = ByteData.sublistView(data, 2, 6);
      String hexString = byteDataToHexString(canId);

      print("this is canId {$hexString}");
      // int length = data[offset + 1];
      // int canId = ((data[offset + 2] << 24) |
      //              (data[offset + 3] << 16) |
      //              (data[offset + 4] << 8) |
      //              data[offset + 5]);
      // int dlc = data[offset + 6];
      Uint8List payload = data.sublist(offset + 7, offset + 15);
      print("this is payload CAN DATA {$payload}");
      // int filler = data[offset + 15];
      int endOfPacket = data[offset + 16];

      if (startOfPacket == 0x7E && endOfPacket == 0x7F) {
        int receivedRCounterValue = (payload[0] << 24) |
            (payload[1] << 16) |
            (payload[2] << 8) |
            payload[3];

        print("This is receivedRCounterValue ->{$receivedRCounterValue}");

        setState(() {
          if (receivedRCounterValue == internalRCounterValue) {
            // Increase internal R/Counter value by 1
            internalRCounterValue =
                (internalRCounterValue + 1) % internalRCounterModValue;
            _receivedData.add(
                'Success: R/Counter value matches. Updated R/Counter to $internalRCounterValue');
          } else {
            // Set internal R/Counter value to received R/Counter value
            int sum = internalRCounterValue + 1;
            print("This is internalRCounterValue ->{$sum}");

            internalRCounterValue =
                receivedRCounterValue % internalRCounterModValue;
            _receivedData.add(
                'Fail: R/Counter value does not match. Updated R/Counter to $internalRCounterValue');
          }
        });
      } else {
        setState(() {
          _receivedData.add('Invalid packet');
        });
      }

      // Move to the next packet
      offset += packetSize;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: ListView.builder(
        itemCount: _receivedData.length,
        itemBuilder: (context, index) {
          if (_receivedData.isEmpty) {
            return const Text("There is no data");
          } else {
            return ListTile(
              title: Text(_receivedData[index]),
            );
          }
        },
      ),
    );
  }
}
