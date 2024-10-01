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
#include <time.h>
#include <future>


std::mutex can1UartMutex;
jint fd;



extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManagerCAN1_sendAPPStateMessage(JNIEnv *env, jobject obj, jint fd2) {
    std::lock_guard<std::mutex> lock(can1UartMutex);

    // Construct the message
    unsigned char message[17] = {
            0x02, // STX
            0xF0,
            0x00,
            0x00,
            0xFF,// CAN ID (change as needed)
            0x08,// Data Length
            0x01,
            0xFF, 0xFF, 0xFF, 0xFF, // Data
            0xFF, 0xFF, 0xFF, 0x00, 0x00, // also Data
            0x04  // ETX
    };

    //unsigned char message[] = {};

    // Send the message
    ssize_t bytesWritten = write(fd2, message, sizeof(message));
    if (bytesWritten < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "UART", "Failed to send message: %s", strerror(errno));
        return -errno;
    } else {
        __android_log_print(ANDROID_LOG_INFO, "UART", "Sent %zd bytes: ", bytesWritten);
        for (int i = 0; i < sizeof(message); i++) {
            __android_log_print(ANDROID_LOG_INFO, "UART", "0x%02X ", message[i]);
        }
    }

    return bytesWritten; // Return the number of bytes sent
}

// Function to convert jint baudRate to termios-compatible speed_t
 speed_t getBaudRate(jint baudRate) {
    switch (baudRate) {
        case 110:
            return B110;
        case 300:
            return B300;
        case 600:
            return B600;
        case 1200:
            return B1200;
        case 2400:
            return B2400;
        case 4800:
            return B4800;
        case 9600:
            return B9600;
        case 19200:
            return B19200;
        case 38400:
            return B38400;
        case 57600:
            return B57600;

        case 115200:
            return B115200;
        case 230400:
            return B230400;
        case 460800:
            return B460800;
        case 921600:
            return B921600;
            // Add more cases for different baud rates as needed
        default:
            return -1;  // Invalid baud rate
    }
}


extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManagerCAN1_can1OpenUART(JNIEnv *env, jobject thiz, jstring devicePath, jint baudRate) {

    const char *path = env->GetStringUTFChars(devicePath, nullptr);
     /*int fd = open(path, O_RDWR | O_NOCTTY | O_NONBLOCK  );*/
     fd = open(path, O_RDWR  | O_NOCTTY  );
    env->ReleaseStringUTFChars(devicePath, path);
    if (fd < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "UART", "Failed to open UART device: %s and fd value less than 0",
                            strerror(errno));
        return 0;
    }

    struct termios options{};
    if (tcgetattr(fd, &options) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "UART", "Failed to get terminal attributes: %s tcgetattr value less than 0", strerror(errno));
        close(fd);
        return 0;
    }

    // Set baud rate B460800 \\ B115200

    if (cfsetispeed(&options, getBaudRate(baudRate)) < 0 || cfsetospeed(&options, getBaudRate(baudRate)) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "UART", "Failed to set baud rate: %s", strerror(errno));
        close(fd);
        return 0;
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

    options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG); //Raw input

    if (tcsetattr(fd, TCSANOW, &options) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "UART", "Failed to set terminal attributes: %s", strerror(errno));
        close(fd);
        return 0;
    }
    tcflush(fd, TCIFLUSH);

    return fd;
}



void sleepInMicroseconds(int microseconds) {
    struct timespec ts{};
    ts.tv_sec = microseconds / 1000000;
    ts.tv_nsec = (microseconds % 1000000) * 1000;
    nanosleep(&ts, nullptr);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManagerCAN1_can1ReadUART(JNIEnv *env, jobject thiz, jint fd1, jbyteArray buffer, jint size) {



        std::lock_guard<std::mutex> lock(can1UartMutex);

        jbyte* globalBuf = env->GetByteArrayElements(buffer, nullptr);
        int totalBytesRead = 0;
        int bytesRead = 0;

        while (totalBytesRead < size) {

            //sleepInMicroseconds(140000);  // Sleep for 10000 microseconds (10 millisecond)

            bytesRead = read(fd1, globalBuf + totalBytesRead, size - totalBytesRead);

            if (bytesRead > 0) {
                totalBytesRead += bytesRead;
            } else if (bytesRead == 0) {
                // No data available; potentially end of file or no data yet
                __android_log_print(ANDROID_LOG_ERROR, "UART", "No data available; potentially end of file or no data yet");
                usleep(100); // Sleep for a short while to avoid busy-waiting
            } else if (bytesRead == -1 && errno == EAGAIN) {
                // Non-blocking mode, no data available right now
                __android_log_print(ANDROID_LOG_ERROR, "UART", "No data available; (bytesRead == -1 && errno == EAGAIN");
                //usleep(100);
                continue;
            } else {
                // An error occurred
                __android_log_print(ANDROID_LOG_ERROR, "UART", "Read error on An error occurred");
                env->ReleaseByteArrayElements(buffer, globalBuf, 0);
                return -errno;
            }

        }

        env->ReleaseByteArrayElements(buffer, globalBuf, 0);

        return totalBytesRead;

    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManagerCAN1_can1WriteUART(JNIEnv* env, jobject obj, jint fd2, jbyteArray data) {

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    jsize length = env->GetArrayLength(data);

    int result = write(fd2, bytes, length);
    __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "This is result ->%d", result);
    env->ReleaseByteArrayElements(data, bytes, 0);

    return result;

}


extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManagerCAN1_can1CloseUART(JNIEnv *env, jobject thiz, jint fd1) {
    std::lock_guard<std::mutex> lock(can1UartMutex);

    int result = close(fd1);

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

