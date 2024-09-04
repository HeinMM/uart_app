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

    options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG); //Raw input

    if (tcsetattr(fd, TCSANOW, &options) < 0) {
        close(fd);
        return -errno;
    }
    tcflush(fd, TCIFLUSH);

    return fd;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_readUART(JNIEnv *env, jobject thiz,jint fd1, jbyteArray buffer, jint size) {

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

    int result = write(fd2, bytes, length);
    __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "This is result ->%d", result);
    env->ReleaseByteArrayElements(data, bytes, 0);

    return result;

}


extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_closeUART(JNIEnv *env, jobject thiz, jint fd1) {
    std::lock_guard<std::mutex> lock(uartMutex);

    isReading = false;
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

