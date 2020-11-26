# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
#set(CMAKE_VERBOSE_MAKEFILE on)
#set(CMAKE_CXX_STANDARD_REQUIRED ON)
# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        ncnn_jni

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/ncnn_jni.cpp
        src/main/cpp/YoloV4.cpp
        src/main/cpp/YoloV5.cpp
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.


include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include)

set(ncnn_lib ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libncnn.a)
add_library(ncnn_lib STATIC IMPORTED)
set_target_properties(ncnn_lib PROPERTIES IMPORTED_LOCATION ${ncnn_lib})

set(glslang_lib ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libglslang.a)
add_library(glslang_lib STATIC IMPORTED)
set_target_properties(glslang_lib PROPERTIES IMPORTED_LOCATION ${glslang_lib})

set(OGLCompiler_lib ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libOGLCompiler.a)
add_library(OGLCompiler_lib STATIC IMPORTED)
set_target_properties(OGLCompiler_lib PROPERTIES IMPORTED_LOCATION ${OGLCompiler_lib})

set(OSDependent_lib ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libOSDependent.a)
add_library(OSDependent_lib STATIC IMPORTED)
set_target_properties(OSDependent_lib PROPERTIES IMPORTED_LOCATION ${OSDependent_lib})

set(SPIRV_lib ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libSPIRV.a)
add_library(SPIRV_lib STATIC IMPORTED)
set_target_properties(SPIRV_lib PROPERTIES IMPORTED_LOCATION ${SPIRV_lib})

#set(libopencv_java4 ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a/libopencv_java4.so)
#add_library(libopencv_java4 SHARED IMPORTED)
#set_target_properties(libopencv_java4 PROPERTIES IMPORTED_LOCATION ${libopencv_java4})

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

find_library(android-lib
        android)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        ncnn_jni

        ${android-lib}
        ${log-lib}

#        libopencv_java4

        jnigraphics

        vulkan

        ncnn_lib
        glslang_lib
        SPIRV_lib
        OGLCompiler_lib
        OSDependent_lib)