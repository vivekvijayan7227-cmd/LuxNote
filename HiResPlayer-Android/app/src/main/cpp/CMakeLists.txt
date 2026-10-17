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

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(hires_audio
    ${log-lib}
    ${android-lib}
)
