// ignore_for_file: prefer_is_empty, prefer_final_fields, avoid_print, avoid_function_literals_in_foreach_calls, prefer_interpolation_to_compose_strings, unused_field
import 'dart:async';
import 'package:flutter/material.dart';
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
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.lightBlue),
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

  static const platform = MethodChannel('com.example.uart_app/uart');
  String data = 'No data';
  String canData = 'No CAN Data';

  @override
  void initState() {
    super.initState();
    startReadingUart();
  }

  Future<void> startReadingUart() async {

    try {
      platform.setMethodCallHandler((call) async {
        if (call.method == "onData") {
          setState(() {
            data = call.arguments['data'].toString();

          });
        } else if (call.method == "onError") {
          setState(() {
            data = "Error: ${call.arguments}";
            canData = "";
          });
        }
      });
      await platform.invokeMethod('startReading', {'devicePath': '/dev/ttymxc1', 'baudRate': 460800});
      print("state done1");
    } on PlatformException catch (e) {
      setState(() {
        data = "Failed to start UART: ${e.message}";
        canData = "";
      });
    }
  }

  @override
  void dispose() {
    platform.invokeMethod('stopReading');
    super.dispose();
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),

      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('RAW DATA: $data'),
            const SizedBox(height: 10),

          ],
        ),
      ),
      
    );
      
    
  }
}
