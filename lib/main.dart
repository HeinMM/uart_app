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
      home: const MyHomePage(title: 'UART TEST APP (/dev/ttymxc)'),
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


  ////////////////CAN 1 variable/////////////////////////
  String can1InternalRCounter = "0";
  String can1Error = "0";
  String can1Error2 = "0";
  bool can1IsOpenPort = false;
  bool can1IsStart = false;
  String can1SendData = "";
  String can1Info = "There is no updated Info";
  //////////////////////////////////////////////////////////


  ////////////////CAN 2 variable/////////////////////////
  String can2InternalRCounter = "0";
  String can2Error = "0";
  String can2Error2 = "0";
 bool can2IsStart = false;
 bool can2IsOpenPort = false;
  String can2SendData = "";
  String can2Info = "There is no updated Info";
  //////////////////////////////////////////////////////////

  Timer? _timer; // For continuous updates

  @override
  void initState() {
    super.initState();

  }

 /* void _startCounter() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if(can1IsStart&&can1IsOpenPort){
        can1StartReadingUart();
      }
    });
  }*/

  // Can 1 OPEN port
  Future<void> can1OpenUart() async {

    try {
      platform.setMethodCallHandler((call) async {

        if (call.method == "can1Info") {


            setState(() {
              can1Info = "Open UART port successfully";
              can1IsOpenPort = true;
            });

        } else if (call.method == "can1OnError") {

            setState(() {
              can1Info = "Something is wrong in OPEN PORT";
              can1IsOpenPort = false;
            });

        }
      });

        await platform.invokeMethod('can1OpenUart', {'devicePath': '/dev/ttymxc1', 'baudRate': 115200});

    } on PlatformException catch (e) {

        setState(() {
          can1Error = "Failed to open UART: ${e.message}";
          can1IsOpenPort = false;
        });

    }
  }

  // Can 1 READ Data
  Future<void> can1StartReadingUart() async {

    try {
      platform.setMethodCallHandler((call) async {
        if (call.method == "can1OnData") {
              setState(() {
                can1InternalRCounter = call.arguments['counter'].toString();
                can1Error = call.arguments['error'].toString();

                can1IsStart = true;
              });


        } else if (call.method == "can1OnError") {

            Future.microtask((){
              setState(() {
                can1InternalRCounter = "Error : ${call.arguments}";
                can1Error = "Something is wrong in error counter";

                can1IsStart = false;
              });
            });

        }
      });
      await platform.invokeMethod('can1StartReading', {'devicePath': '/dev/ttymxc1', 'baudRate': 115200});

    } on PlatformException catch (e) {
     Future.microtask((){
       setState(() {
         can1InternalRCounter = "Failed to start UART: ${e.message}";
         can1Error2 = "";
       });
     });
    }
  }

  // Can 1 Write Data
  Future<void> can1WriteDataUart() async {

    try {

      await platform.invokeMethod('can1WriteData');

    } on PlatformException catch (e) {

        setState(() {
          can1InternalRCounter = "Failed to start UART: ${e.message}";
          can1Error2 = "";
        });

    }
  }

  // Can 1 CLOSE port
  Future<void> can1CloseUart() async {

    try {
      platform.setMethodCallHandler((call) async {

        if (call.method == "can1Info") {


          setState(() {
            can1Info = "Close UART port successfully";

          });

        } else if (call.method == "can1OnError") {

          setState(() {
            can1Info = "Something is wrong in CLOSE PORT";
          });

        }
      });

      await platform.invokeMethod('can1StopReading');

    } on PlatformException catch (e) {

      setState(() {
        can1Error = "Failed to open UART: ${e.message}";
      });

    }
  }

  // Can 2 OPEN port
  Future<void> can2OpenUart() async {

    try {
      platform.setMethodCallHandler((call) async {

        if (call.method == "info") {


            setState(() {
              can2Info = "Open UART port successfully";
              can2IsOpenPort = true;
            });

        } else if (call.method == "onError") {

            setState(() {
              can2Info = "Something is wrong in OPEN PORT";
              can2IsOpenPort = false;
            });

        }
      });

      await platform.invokeMethod('can2OpenUart', {'devicePath': '/dev/ttymxc2', 'baudRate': 460800});

    } on PlatformException catch (e) {

        setState(() {
          can2Error = "Failed to open UART: ${e.message}";
          can2IsOpenPort = false;
        });

    }
  }

  // Can 2 READ Data
  Future<void> can2StartReadingUart() async {

    try {
      platform.setMethodCallHandler((call) async {
        if (call.method == "onData") {

            Future.microtask((){
              setState(() {
                can2InternalRCounter = call.arguments['counter'].toString();
                can2Error = call.arguments['error'].toString();

                can2IsStart = true;
              });
            });



        } else if (call.method == "onError") {
          Future.microtask((){
            setState(() {
              can2InternalRCounter = "Error : ${call.arguments}";
              can2Error = "Something is wrong in error counter";

              can2IsStart = false;

            });
          });
        }
      });
      await platform.invokeMethod('can2StartReading', {'devicePath': '/dev/ttymxc2', 'baudRate': 460800});

    } on PlatformException catch (e) {
      setState(() {
        can2InternalRCounter = "Failed to start UART: ${e.message}";
        can2Error2 = "";
      });
    }
  }

  // Can 1 Write Data
  Future<void> can2WriteDataUart() async {

    try {

      await platform.invokeMethod('can2WriteData');

    } on PlatformException catch (e) {
      setState(() {
        can2InternalRCounter = "Failed to start UART: ${e.message}";
        can2Error2 = "";
      });
    }
  }

  // Can 1 OPEN port
  Future<void> can2CloseUart() async {

    try {
      platform.setMethodCallHandler((call) async {

        if (call.method == "can2Info") {


          setState(() {
            can2Info = "Close UART port successfully";

          });

        } else if (call.method == "can2OnError") {

          setState(() {
            can1Info = "Something is wrong in CLOSE PORT";
          });

        }
      });

      await platform.invokeMethod('can2StopReading');

    } on PlatformException catch (e) {

      setState(() {
        can1Error = "Failed to open UART: ${e.message}";
      });

    }
  }


  @override
  void dispose() {
    platform.invokeMethod('can1StopReading');
    platform.invokeMethod('can2StopReading');
    super.dispose();
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(

      appBar: AppBar(
        backgroundColor: Colors.indigo,
        title: Text(widget.title,
        style: const TextStyle(
          color: Colors.white
        ),
        ),
      ),
      backgroundColor: Colors.black26,
      body: Center(

        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            /*Padding(padding: EdgeInsets.all(10),child:
            TextField(
              decoration: InputDecoration(
                  prefixText: '/dev/ttymxc1',
                  icon: Icon(Icons.folder)
              ),
            )
              ,),*/
        Padding(padding: EdgeInsets.all(20),
        child:

            Card( // CAN 1 CARD
              color: Colors.indigo,
              child: Padding(padding: const EdgeInsets.all(10),
                child:
              Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [




                  const Text('CAN 1',
                    style: TextStyle(
                        fontSize: 35.0,
                        color: Colors.white
                    ),
                  ),



                  const SizedBox(height: 30,),
                  const Text('                                                                                '
                  ),



                  Text('Info: $can1Info',
                    style:  const TextStyle(
                      fontSize: 20.0,
                      color: Colors.white
                    ),
                  ),
                  const SizedBox(height: 10,),
                  Text('Internal Counter: $can1InternalRCounter',
                    overflow: TextOverflow.ellipsis,
                    style:  const TextStyle(
                      fontSize: 20.0,
                        color: Colors.white
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text('Error count: $can1Error',
                    style: const TextStyle(
                      fontSize: 18.0,
                        color: Colors.white
                    ),
                  ),

                  const SizedBox(height: 10),

                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      can1IsStart||can1IsOpenPort ? const SizedBox() : ElevatedButton(child: const Text('Open'), onPressed: (){

                        can1OpenUart();

                        setState(() {
                          can1IsOpenPort = true;
                        });

                      }),

                      can1IsOpenPort? ElevatedButton(child: const Text('Start'), onPressed: (){
                        if(can1IsStart==false){
                          can1StartReadingUart();
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



                      can1IsOpenPort? ElevatedButton(onPressed: (){
                        can1WriteDataUart();
                      }, child: const Text("Send Data")): const SizedBox(),

                      can1IsOpenPort? ElevatedButton(child: const Text('Restart'), onPressed: (){
                        can1CloseUart();
                        setState(() {
                          can1IsStart=true;
                          can1IsOpenPort = false;
                        }
                        );

                        Navigator.pushReplacement(
                            context,
                            MaterialPageRoute(
                                builder: (BuildContext context) => super.widget));
                      },): const SizedBox(),
                    ],),

                ],


              )

                ,)
            ),
        ),

            const SizedBox(width: 80,),


            Padding(padding: const EdgeInsets.all(20),
              child:
                Card( // CAN 2 CARD
                    color: Colors.indigo,
                child: Padding(padding: const EdgeInsets.all(10),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Text('CAN 2',
                      style: TextStyle(
                          fontSize: 35.0,
                          color: Colors.white
                      ),
                    ),

                    const SizedBox(height: 30,),
                    const Text('                                                                                '
                    ),
                    Text('Info: $can2Info',
                      style:  const TextStyle(
                          fontSize: 20.0,
                          color: Colors.white
                      ),
                    ),

                    const SizedBox(height: 10,),
                    Text('Internal Counter: $can2InternalRCounter',
                      overflow: TextOverflow.ellipsis,
                      style:  const TextStyle(
                          fontSize: 20.0,
                          color: Colors.white
                      ),
                    ),
                    const SizedBox(height: 10),
                    Text('Error count: $can2Error',
                      style: const TextStyle(
                          fontSize: 18.0,
                          color: Colors.white
                      ),
                    ),

                    const SizedBox(height: 40),

                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        can2IsStart||can2IsOpenPort ? const SizedBox() : ElevatedButton(child: const Text('Open'), onPressed: (){

                          can2OpenUart();

                          setState(() {
                            can2IsOpenPort = true;
                          });

                        }),

                        can2IsOpenPort? ElevatedButton(child: const Text('Start'), onPressed: (){
                          if(can2IsStart==false){
                            can2StartReadingUart();
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
                        }):const SizedBox(),



                        can2IsOpenPort? ElevatedButton(onPressed: (){
                          can2WriteDataUart();
                        }, child: const Text("Send Data")): const SizedBox(),

                        can2IsOpenPort? ElevatedButton(child: const Text('Restart'), onPressed: (){
                          can2CloseUart();
                          setState(() {
                            can2IsStart=true;
                            can2IsOpenPort = false;
                          }
                          );

                          Navigator.pushReplacement(
                              context,
                              MaterialPageRoute(
                                  builder: (BuildContext context) => super.widget));
                        },): const SizedBox(),
                      ],)

                  ],
                ),)
                )
              ,)
          ],
        )
      ),
      
    );



    
  }
}
