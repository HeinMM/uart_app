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
  

  String _data = 'No data';

  @override
  void initState() {
    super.initState();
    _openSerialPort();
    _setUpDataListener();
  }

  Future<void> _openSerialPort() async {
    try {
      final String result = await platform.invokeMethod('openSerialPort');
      print(result);
    } on PlatformException catch (e) {
      print("Failed to open serial port: '${e.message}'.");
    }
  }

  void _setUpDataListener() {
    
    platform.setMethodCallHandler((call) async {
      if (call.method == "onDataReceived") {
        
        
        final data = call.arguments as Map;
        final canId = data['canId'] as String;
        final dataBytes = data['data'] as String;

        setState(() {
          _data = 'CAN ID: $canId\nData: $dataBytes';
        });
      }
    });
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),

       body: 
        Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Center(
          child: ElevatedButton(
            onPressed: _openSerialPort,
            child: const Text('Open Serial Port'),
          ),
        ),
        const Text(
          'Received UART Data:',
          style: TextStyle(fontSize: 20),
        ),
        const SizedBox(height: 20),
        Text(
          _data,
          style: const TextStyle(fontSize: 16),
          textAlign: TextAlign.center,
        ),
      ],
      ),
      
    );
      
    
  }
}
