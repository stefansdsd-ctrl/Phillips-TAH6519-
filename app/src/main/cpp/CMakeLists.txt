cmake_minimum_required(VERSION 3.18)
project(native_convolver)

add_library(native_convolver STATIC convolver.cpp)

# Include directories
target_include_directories(native_convolver PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
