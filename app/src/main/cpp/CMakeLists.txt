# CMakeLists for native convolver (builds shared JNI library)
cmake_minimum_required(VERSION 3.18)
project(native_convolver)

add_library(native_convolver SHARED
    convolver.cpp
    fft_convolver.cpp
)

# Include directories
target_include_directories(native_convolver PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# log library for Android
find_library(log-lib log)

target_link_libraries(native_convolver PRIVATE ${log-lib})
