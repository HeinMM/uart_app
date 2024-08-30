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

int fd;

extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_openUART(JNIEnv *env, jobject thiz, jstring devicePath, jint baudRate) {
    const char *path = env->GetStringUTFChars(devicePath, NULL);
     fd = open(path, O_RDWR | O_NOCTTY | O_NONBLOCK);
    env->ReleaseStringUTFChars(devicePath, path);
    if (fd < 0) {
        return -errno;
    }

    struct termios options;
    tcgetattr(fd, &options);

    cfsetispeed(&options, B460800);
    cfsetospeed(&options, B460800);

//    options.c_cflag |= (CLOCAL | CREAD);
//    options.c_cflag &= ~CSIZE;
//    options.c_cflag |= CS8 ;
//
//    options.c_cflag &= ~PARENB;
//    options.c_cflag &= ~CSTOPB;

    // Set raw input (binary) mode
    options.c_iflag &= ~(INLCR | ICRNL | IGNCR | IXON | IXOFF | IXANY);
//    options.c_iflag &= ~(INLCR | ICRNL | IGNCR ); // Disable newline and carriage return processing
    options.c_oflag &= ~OPOST;


    options.c_cflag |= (CS8 | CREAD | CLOCAL);
    options.c_cflag |= CRTSCTS;  // Enable hardware flow control (RTS/CTS)


    options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);



    tcsetattr(fd, TCSANOW, &options);
    tcflush(fd, TCIFLUSH);
    return fd;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_readUART(JNIEnv *env, jobject thiz, jint fd, jbyteArray buffer, jint size) {
    jbyte *buf = env->GetByteArrayElements(buffer, NULL);
    int totalBytesRead = 0;
    int bytesRead = 0;

    while (totalBytesRead < size) {
        bytesRead = read(fd, buf + totalBytesRead, size - totalBytesRead);
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

    int result = write(fd, bytes, length);
    __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "This is result ->%d", result);
    env->ReleaseByteArrayElements(data, bytes, 0);

    return result;

}


extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_closeUART(JNIEnv *env, jobject thiz, jint fd) {
    int result = close(fd);
    if (result < 0) {
        return -errno;
    }
    return 0;
}

