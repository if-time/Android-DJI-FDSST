# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)


set(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} -L${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp
        fdsst/fdssttracker.cpp
        fdsst/fhog.cpp)

include_directories(fdsst)
include_directories(include)

find_library(
        log-lib
        log
)

target_link_libraries( # Specifies the target library.
        native-lib
        jnigraphics
        ${log-lib}
        opencv_java3
        )
