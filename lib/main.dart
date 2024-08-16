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
      home: const MyHomePage(title: 'UART TEST APP (/dev/ttymxc1)'),
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
  String internalRCounter = "0";
  String error = "0";
  String error2 = "0";
  var isStart = false;

  @override
  void initState() {
    super.initState();

  }

  Future<void> startReadingUart() async {

    try {
      platform.setMethodCallHandler((call) async {
        if (call.method == "onData") {
          setState(() {
            internalRCounter = call.arguments['counter'].toString();
            error = call.arguments['error'].toString();
          });
        } else if (call.method == "onError") {
          setState(() {
            internalRCounter = "Error : ${call.arguments}";
            error = "Something is wrong in error counter";
          });
        }
      });
      await platform.invokeMethod('startReading', {'devicePath': '/dev/ttymxc1', 'baudRate': 460800});

    } on PlatformException catch (e) {
      setState(() {
        internalRCounter = "Failed to start UART: ${e.message}";
        error2 = "";
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
            Text('Internal Counter: $internalRCounter'),
            const SizedBox(height: 10),
            Text('Error count: $error'),

            const SizedBox(height: 10),

            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
              ElevatedButton(child: const Text('Start'), onPressed: (){
                if(isStart==false){
                  startReadingUart();
                  isStart=true;
                }else{

                }

              }),
              // ElevatedButton(child: const Text('Stop'), onPressed: () async {
              //   try {
              //             await platform.invokeMethod('stopReading');
              //     } on PlatformException catch (e) {
              //     setState(() {
              //             internalRCounter = "Failed to start UART: ${e.message}";
              //             error2 = "";
              //     });
              //   }
              // }),
                ElevatedButton(child: const Text('Restart'), onPressed: (){
                  isStart==false;
                  Navigator.pushReplacement(
                      context,
                      MaterialPageRoute(
                          builder: (BuildContext context) => super.widget));
                }),
            ],)
          ],
        ),
      ),
      
    );
      
    
  }
}
