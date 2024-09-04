#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <termios.h>
#include <cerrno>
#include <chrono>
#include <iostream>
#include <sys/ioctl.h>
#include <cstdint>
#include <android/log.h>

// Global buffer declaration
/*jbyte* globalBuf = nullptr;*/
bool isReading = true;
std::mutex uartMutex;



extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_openUART(JNIEnv *env, jobject thiz, jstring devicePath, jint baudRate) {
    isReading = true;
    const char *path = env->GetStringUTFChars(devicePath, nullptr);
     int fd = open(path, O_RDWR | O_NOCTTY | O_NONBLOCK  );
    env->ReleaseStringUTFChars(devicePath, path);
    if (fd < 0) {
        return -errno;
    }

    struct termios options;
    tcgetattr(fd, &options);

    /*cfsetispeed(&options, B460800);
    cfsetospeed(&options, B460800);*/

    // Set baud rate
    if (cfsetispeed(&options, B460800) < 0 || cfsetospeed(&options, B460800) < 0) {
        close(fd);
        return -errno;
    }


    // Configure UART for 8N1: 8 data bits, no parity, 1 stop bit
    options.c_cflag &= ~CSIZE;         // Clear current data size bits
    options.c_cflag |= CS8;            // Set data bits to 8
    options.c_cflag &= ~PARENB;        // No parity
    options.c_cflag &= ~CSTOPB;        // 1 stop bit
    options.c_cflag &= ~CRTSCTS;       // No hardware flow control


    // Set raw input (binary) mode
    options.c_iflag &= ~(INLCR | ICRNL | IGNCR | IXON | IXOFF | IXANY);
    options.c_oflag &= ~OPOST;

    /*options.c_cflag |= (CS8 | CREAD | CLOCAL);
    options.c_cflag |= CRTSCTS;  // Enable hardware flow control (RTS/CTS)*/



    options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG); //Raw input

    /*tcsetattr(fd, TCSANOW, &options);*/
    if (tcsetattr(fd, TCSANOW, &options) < 0) {
        close(fd);
        return -errno;
    }
    tcflush(fd, TCIFLUSH);

    return fd;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_readUART(JNIEnv *env, jobject thiz,jint fd1, jbyteArray buffer, jint size) {
    // Read with JNI Buffer
   /* std::lock_guard<std::mutex> lock(uartMutex);

    int totalBytesRead = 0;
    int bytesRead = 0;


    jbyte* localBuf = env->GetByteArrayElements(buffer, nullptr);




    while (totalBytesRead < size && isReading) {
        bytesRead = read(fd1, localBuf + totalBytesRead, size - totalBytesRead);
        if (bytesRead > 0) {

            totalBytesRead += bytesRead;
            // Look for the start of a valid CAN frame (e.g., specific start bytes like 0x7E 0x0D)
            if (totalBytesRead >= 17) {
                if (localBuf[0] != 0x7E && localBuf[1] != 0x0D) {
                    memmove(localBuf, localBuf + 1, totalBytesRead - 1);
                    totalBytesRead--;
                }
            }
        } else if (bytesRead == 0) {

            usleep(100);
        }
        else if (bytesRead == -1 && errno == EAGAIN) {
            continue;
        }else {

            env->ReleaseByteArrayElements(buffer, localBuf, 0);
            return -errno;
        }
    }

    env->ReleaseByteArrayElements(buffer, localBuf, 0);
    return totalBytesRead;*/

    // Read with C++ NativeBuffer
    /*if (fd1 < 0 || buffer == nullptr || size <= 0) {
        return -1;  // Return an error if inputs are invalid
    }

    jbyte* nativeBuffer = new jbyte[size];
    if (!nativeBuffer) {
        return -1;  // Return an error if allocation fails
    }

    int totalBytesRead = 0;
    while (totalBytesRead < size) {
        int bytesRead = read(fd1, nativeBuffer + totalBytesRead, size - totalBytesRead);
        if (bytesRead > 0) {
            totalBytesRead += bytesRead;
        } else if (bytesRead == 0) {
            // No data available, sleep briefly to avoid busy-waiting
            usleep(1000);  // Sleep for 1 millisecond
        } else if (bytesRead < 0 && errno == EAGAIN) {
            // No data available but no error (non-blocking mode)
            continue;
        } else {
            // Handle other read errors
            delete[] nativeBuffer;
            return -errno;
        }
    }

    // Copy the data from the native buffer to the Java array
    env->SetByteArrayRegion(buffer, 0, totalBytesRead, nativeBuffer);

    delete[] nativeBuffer;  // Free the native buffer
    return totalBytesRead;*/

    // Testing Code
    jbyte* globalBuf = env->GetByteArrayElements(buffer, nullptr);
    int totalBytesRead = 0;
    int bytesRead = 0;

    while (totalBytesRead < size) {
        bytesRead = read(fd1, globalBuf + totalBytesRead, size - totalBytesRead);

        if (bytesRead > 0) {
            totalBytesRead += bytesRead;
        } else if (bytesRead == 0) {
            // No data available; potentially end of file or no data yet
            usleep(100); // Sleep for a short while to avoid busy-waiting
        } else if (bytesRead == -1 && errno == EAGAIN) {
            // Non-blocking mode, no data available right now
            continue;
        } else {
            // An error occurred
            env->ReleaseByteArrayElements(buffer, globalBuf, 0);
            return -errno;
        }
    }

    env->ReleaseByteArrayElements(buffer, globalBuf, 0);
    return totalBytesRead;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_writeUART(JNIEnv* env, jobject obj, jint fd2, jbyteArray data) {
    std::lock_guard<std::mutex> lock(uartMutex);

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    jsize length = env->GetArrayLength(data);

    /*int result = write(fd, bytes, length);*/
    int result = write(fd2, bytes, length);
    __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "This is result ->%d", result);
    env->ReleaseByteArrayElements(data, bytes, 0);

    return result;

}


extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_closeUART(JNIEnv *env, jobject thiz, jint fd1) {
    std::lock_guard<std::mutex> lock(uartMutex);
    // Flush the port
    /*tcflush(fd1, TCIOFLUSH);*/
    isReading = false;
    int result = close(fd1);

    /*int result = 0;*/
    if (result < 0) {
        return -errno;
    }
    return 0;
}

void clearBuffer(jbyte* buf, jsize length) {
    if (buf != nullptr && length > 0) {
        memset(buf, 0, length);
    }
}

