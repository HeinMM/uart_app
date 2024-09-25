/*
 import 'package:flutter/services.dart';

class Can1DTO{

   static const platform = MethodChannel('com.example.uart_app/uart');

   ////////////////CAN 1 variable/////////////////////////
   String can1InternalRCounter = "0";
   String can1Error = "0";
   String can1Error2 = "0";
   var can1IsStart = false;
   var can1IsOpenPort = false;
   String can1SendData = "";
   String can1Info = "There is no updated Info";
 //////////////////////////////////////////////////////////

   // Can 1 OPEN port
   Future<void> can1OpenUart() async {

     try {
       platform.setMethodCallHandler((call) async {

         if (call.method == "info") {

           setState(() {
             can1Info = "Open UART port successfully";
             can1IsOpenPort = true;
           });
         } else if (call.method == "onError") {
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


}*/
