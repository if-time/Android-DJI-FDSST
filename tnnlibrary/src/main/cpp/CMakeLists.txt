# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

set(lib_DIR ${CMAKE_SOURCE_DIR}/../jniLibs)

include_directories(include)

set(TNN_OPENCL_ENABLE ON CACHE BOOL "" FORCE)
set(TNN_ARM_ENABLE ON CACHE BOOL "" FORCE)

#file(GLOB_RECURSE WRAPPER_SRCS ${CMAKE_SOURCE_DIR}/../cpp/*.cc)
#add_library(tnn_wrapper SHARED imageclassify_jni.cpp ${WRAPPER_SRCS})
file(GLOB_RECURSE WRAPPER_SRCS ${CMAKE_SOURCE_DIR}/../cpp/*.cpp)
add_library(tnn_wrapper SHARED ${WRAPPER_SRCS})

add_library(tnn SHARED IMPORTED)
set_target_properties(tnn PROPERTIES IMPORTED_LOCATION ${lib_DIR}/${ANDROID_ABI}/libTNN.so)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        tnn_wrapper
        -ljnigraphics
        tnn
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})