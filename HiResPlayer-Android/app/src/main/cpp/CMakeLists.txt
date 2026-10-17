cmake_minimum_required(VERSION 3.22.1)

project("hires_audio" VERSION 1.0.0 LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(hires_audio SHARED
    native-bridge.cpp
    HiResAudioEngine.cpp
)

target_include_directories(hires_audio PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Compiler optimization & vectorization configuration
if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
    target_compile_options(hires_audio PRIVATE
        -O3
        -ffast-math
        -fvectorize
        -fslp-vectorize
        -Wall
        -Wextra
    )
endif()

# Architecture-specific SIMD instruction sets
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(hires_audio PRIVATE -march=armv8-a+simd)
elseif(ANDROID_ABI STREQUAL "x86_64")
    target_compile_options(hires_audio PRIVATE -msse4.2 -mpopcnt)
endif()

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(hires_audio
    ${log-lib}
    ${android-lib}
)
