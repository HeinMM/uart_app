#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <termios.h>
#include <cerrno>

extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_openUART(JNIEnv *env, jobject thiz, jstring devicePath, jint baudRate) {
    const char *path = env->GetStringUTFChars(devicePath, NULL);
    int fd = open(path, O_RDWR | O_NOCTTY | O_NONBLOCK);
    env->ReleaseStringUTFChars(devicePath, path);
    if (fd < 0) {
        return -errno;
    }

    struct termios options;
    tcgetattr(fd, &options);

    cfsetispeed(&options, baudRate);
    cfsetospeed(&options, baudRate);

//    options.c_cflag |= (CLOCAL | CREAD);    // Enable receiver and set local mode
//    options.c_cflag &= ~CSIZE;              // Mask the character size bits
//    options.c_cflag |= CS6 ;// Select 8 data bits
//    options.c_cflag = BINARY;
//    options.c_cflag &= ~PARENB;             // No parity bit
//    options.c_cflag &= ~CSTOPB;             // 1 stop bit

    // Set raw input (binary) mode
    options.c_iflag &= ~(INLCR | ICRNL | IGNCR); // Disable newline and carriage return processing
    options.c_oflag &= ~OPOST; // Disable output processing

    // Set control flags
    options.c_cflag |= (CS8 | CREAD | CLOCAL); // 8 data bits, enable receiver, local mode

    // Set local modes
    options.c_lflag &= ~(ICANON | ECHO | ISIG); // Disable canonical mode, echo, and signals

    tcsetattr(fd, TCSANOW, &options);
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

            usleep(1000);
        } else {

            env->ReleaseByteArrayElements(buffer, buf, 0);
            return -errno;
        }
    }
    env->ReleaseByteArrayElements(buffer, buf, 0);
    return totalBytesRead;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_uart_1app_UartManager_closeUART(JNIEnv *env, jobject thiz, jint fd) {
    int result = close(fd);
    if (result < 0) {
        return -errno;
    }
    return 0;
}
