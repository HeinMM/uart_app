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
  var isOpenPort = false;

  String sendData = "";
  String info = "There is no updated Info";

  @override
  void initState() {
    super.initState();

  }

  // OPEN port
  Future<void> openUart() async {

    try {
      platform.setMethodCallHandler((call) async {

        if (call.method == "info") {

          setState(() {
            info = "Open UART port successfully";
            isOpenPort = true;
          });
        } else if (call.method == "onError") {
          setState(() {
            info = "Something is wrong in OPEN PORT";
            isOpenPort = false;
          });
        }
      });

      await platform.invokeMethod('openUart', {'devicePath': '/dev/ttymxc1', 'baudRate': 460800});

    } on PlatformException catch (e) {
      setState(() {
        error = "Failed to open UART: ${e.message}";
        isOpenPort = false;
      });
    }
  }

  // READ Data
  Future<void> startReadingUart() async {

    try {
      platform.setMethodCallHandler((call) async {
        if (call.method == "onData") {
          setState(() {
            internalRCounter = call.arguments['counter'].toString();
            error = call.arguments['error'].toString();

              isStart = true;


          });
        } else if (call.method == "onError") {
          setState(() {
            internalRCounter = "Error : ${call.arguments}";
            error = "Something is wrong in error counter";

              isStart = false;

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

  // Write Data
  Future<void> writeDataUart() async {

    try {
      platform.setMethodCallHandler((call) async {
        if (call.method == "sendData") {
          setState(() {
            sendData = call.arguments['sendData'].toString();
          });
        } else if (call.method == "onError") {
          setState(() {
            sendData = "Error : ${call.arguments}";
          });
        }
      });
      await platform.invokeMethod('writeData');

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
            Text('Info: $info',
              style:  const TextStyle(
                fontSize: 20.0,

              ),
            ),
            const SizedBox(height: 10,),
            Text('Internal Counter: $internalRCounter',
              style:  const TextStyle(
                fontSize: 20.0,

              ),
            ),
            const SizedBox(height: 10),
            Text('Error count: $error',
             style: const TextStyle(
               fontSize: 18.0,

             ),
            ),

            const SizedBox(height: 10),

            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                isStart||isOpenPort ? const SizedBox() : ElevatedButton(child: const Text('Open'), onPressed: (){

                  openUart();

                  setState(() {
                    isOpenPort = true;
                  });

                }),

              isOpenPort? ElevatedButton(child: const Text('Start'), onPressed: (){
                if(isStart==false){
                  startReadingUart();
                  /*isStart=true;*/
                }else{
                  showDialog(
                    barrierDismissible: false,
                    context: context,
                    builder: (BuildContext context) => AlertDialog(
                      title: const Text("WARING"),
                      content: const Text("YOU ALREADY STARTED. YOU NEED RESTART?"),
                      elevation: 24,
                      actions: [
                        OutlinedButton(child: const Text("OK"), onPressed: () {
                          Navigator.of(context).pop();
                        },)
                      ],
                    ),
                  );
                }
              }): const SizedBox(),



               isOpenPort? ElevatedButton(onPressed: (){
                      writeDataUart();
                }, child: const Text("Send Data")): const SizedBox(),

                isOpenPort? ElevatedButton(child: const Text('Restart'), onPressed: (){
                  setState(() {
                    isStart=true;
                    isOpenPort = false;
                  }
                  );

                  Navigator.pushReplacement(
                      context,
                      MaterialPageRoute(
                          builder: (BuildContext context) => super.widget));
                },): const SizedBox(),
            ],)
          ],
        ),
      ),
      
    );
      
    
  }
}
