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

int fd = -1;

extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_openUART(JNIEnv *env, jobject thiz, jstring devicePath, jint baudRate) {
    const char *path = env->GetStringUTFChars(devicePath, nullptr);
     fd = open(path, O_RDWR | O_NOCTTY | O_NONBLOCK | O_ASYNC);
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
    jbyte *buf = env->GetByteArrayElements(buffer, nullptr);
    int totalBytesRead = 0;
    int bytesRead = 0;

    while (totalBytesRead < size) {
        bytesRead = read(fd1, buf + totalBytesRead, size - totalBytesRead);
        if (bytesRead > 0) {
            totalBytesRead += bytesRead;
        } else if (bytesRead == 0) {

            usleep(100);
        }
        else if (bytesRead == -1 && errno == EAGAIN) {
            continue;
        }else {

            env->ReleaseByteArrayElements(buffer, buf, 0);
            return -errno;
        }
    }

    env->ReleaseByteArrayElements(buffer, buf, 0);
    return totalBytesRead;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_writeUART(JNIEnv* env, jobject obj, jbyteArray data) {

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    jsize length = env->GetArrayLength(data);

    /*int result = write(fd, bytes, length);*/
    int result = write(fd, bytes, length);
    __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "This is result ->%d", result);
    env->ReleaseByteArrayElements(data, bytes, 0);

    return result;

}


extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_closeUART(JNIEnv *env, jobject thiz, jint fd) {
    int result = close(fd);
    /*int result = 0;*/
    if (result < 0) {
        return -errno;
    }
    return 0;
}

