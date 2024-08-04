import 'dart:typed_data';
import 'package:usb_serial/usb_serial.dart';
import 'dart:async';


class UartService {
  UsbPort? _port;
  Stream<Uint8List>? _inputStream;

  Future<bool> open(String portName) async {
    List<UsbDevice> devices = await UsbSerial.listDevices();
    for (var device in devices) {
      if (device.productName == portName) {
        _port = await device.create();
        if (await _port!.open()) {
          await _port!.setDTR(true);
          await _port!.setRTS(true);
          await _port!.setPortParameters(460800, UsbPort.DATABITS_8, UsbPort.STOPBITS_1, UsbPort.PARITY_NONE);
          _inputStream = _port!.inputStream!.asBroadcastStream();
          return true;
        }
      }
    }
    return false;
  }

  Stream<Uint8List> get inputStream => _inputStream!;

  void close() {
    _port?.close();
  }
}