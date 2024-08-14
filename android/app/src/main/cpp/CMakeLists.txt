cmake_minimum_required(VERSION 3.4.1)

# Define the project
project(uart_app)

# Add the native library
add_library(
        native-lib
        SHARED
        native-lib.cpp
)

# Find the log library
find_library(
        log-lib
        log
)

# Link the native library with the log library
target_link_libraries(
        native-lib
        ${log-lib}
)
