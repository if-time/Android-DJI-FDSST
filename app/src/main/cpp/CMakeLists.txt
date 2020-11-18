# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)


set(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} -L${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}")
set(distribution_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp
        fdsst/fdssttracker.cpp
        fdsst/fhog.cpp
        YoloV4.cpp
        YoloV4.h
        YoloV5.cpp
        YoloV5.h
        )

include_directories(fdsst)
include_directories(include)
include_directories(ncnnvulkan/include)

find_library(log-lib
        log)
find_library(android-lib
        android)

#OpenCV3.3.0

add_library(libopencv_java3
        SHARED
        IMPORTED )
set_target_properties(libopencv_java3
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_java3.so)

#1.libopencv_aruco
add_library(libopencv_aruco
        STATIC
        IMPORTED )
set_target_properties(libopencv_aruco
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_aruco.a)

#2.libopencv_bgsegm
add_library(libopencv_bgsegm
        STATIC
        IMPORTED )
set_target_properties(libopencv_bgsegm
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_bgsegm.a)

#3.libopencv_bioinspired
add_library(libopencv_bioinspired
        STATIC
        IMPORTED )
set_target_properties(libopencv_bioinspired
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_bioinspired.a)

#4.libopencv_calib3d
add_library(libopencv_calib3d
        STATIC
        IMPORTED )
set_target_properties(libopencv_calib3d
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_calib3d.a)

#5.libopencv_ccalib
add_library(libopencv_ccalib
        STATIC
        IMPORTED )
set_target_properties(libopencv_ccalib
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_ccalib.a)

#6.libopencv_core
add_library(libopencv_core
        STATIC
        IMPORTED )
set_target_properties(libopencv_core
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_core.a)

#7.libopencv_datasets
add_library(libopencv_datasets
        STATIC
        IMPORTED )
set_target_properties(libopencv_datasets
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_datasets.a)

#7.5.libopencv_dpm
add_library(libopencv_dnn
        STATIC
        IMPORTED )
set_target_properties(libopencv_dnn
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_dnn.a)

#8.libopencv_dpm
add_library(libopencv_dpm
        STATIC
        IMPORTED )
set_target_properties(libopencv_dpm
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_dpm.a)

#9.libopencv_face
add_library(libopencv_face
        STATIC
        IMPORTED )
set_target_properties(libopencv_face
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_face.a)

#10.libopencv_features2d
add_library(libopencv_features2d
        STATIC
        IMPORTED )
set_target_properties(libopencv_features2d
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_features2d.a)

#11.libopencv_flann
add_library(libopencv_flann
        STATIC
        IMPORTED )
set_target_properties(libopencv_flann
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_flann.a)

#12.libopencv_fuzzy
add_library(libopencv_fuzzy
        STATIC
        IMPORTED )
set_target_properties(libopencv_fuzzy
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_fuzzy.a)

#13.libopencv_highgui
add_library(libopencv_highgui
        STATIC
        IMPORTED )
set_target_properties(libopencv_highgui
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_highgui.a)

#13.5.libopencv_img_hash
add_library(libopencv_img_hash
        STATIC
        IMPORTED )
set_target_properties(libopencv_img_hash
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_img_hash.a)

#14.libopencv_imgcodecs
add_library(libopencv_imgcodecs
        STATIC
        IMPORTED )
set_target_properties(libopencv_imgcodecs
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_imgcodecs.a)

#15.libopencv_imgproc
add_library(libopencv_imgproc
        STATIC
        IMPORTED )
set_target_properties(libopencv_imgproc
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_imgproc.a)

#16.libopencv_line_descriptor
add_library(libopencv_line_descriptor
        STATIC
        IMPORTED )
set_target_properties(libopencv_line_descriptor
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_line_descriptor.a)

#17.libopencv_ml
add_library(libopencv_ml
        STATIC
        IMPORTED )
set_target_properties(libopencv_ml
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_ml.a)

#18.libopencv_objdetect
add_library(libopencv_objdetect
        STATIC
        IMPORTED )
set_target_properties(libopencv_objdetect
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_objdetect.a)

#19.libopencv_optflow
add_library(libopencv_optflow
        STATIC
        IMPORTED )
set_target_properties(libopencv_optflow
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_optflow.a)

#20.libopencv_phase_unwrapping
add_library(libopencv_phase_unwrapping
        STATIC
        IMPORTED )
set_target_properties(libopencv_phase_unwrapping
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_phase_unwrapping.a)

#21.libopencv_photo
add_library(libopencv_photo
        STATIC
        IMPORTED )
set_target_properties(libopencv_photo
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_photo.a)

#22.libopencv_plot
add_library(libopencv_plot
        STATIC
        IMPORTED )
set_target_properties(libopencv_plot
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_plot.a)

#23.libopencv_reg
add_library(libopencv_reg
        STATIC
        IMPORTED )
set_target_properties(libopencv_reg
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_reg.a)

#24.libopencv_rgbd
add_library(libopencv_rgbd
        STATIC
        IMPORTED )
set_target_properties(libopencv_rgbd
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_rgbd.a)

#25.libopencv_saliency
add_library(libopencv_saliency
        STATIC
        IMPORTED )
set_target_properties(libopencv_saliency
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_saliency.a)

#26.libopencv_shape
add_library(libopencv_shape
        STATIC
        IMPORTED )
set_target_properties(libopencv_shape
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_shape.a)


#27.libopencv_stereo
add_library(libopencv_stereo
        STATIC
        IMPORTED )
set_target_properties(libopencv_stereo
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_stereo.a)

#28.libopencv_stitching
add_library(libopencv_stitching
        STATIC
        IMPORTED )
set_target_properties(libopencv_stitching
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_stitching.a)

#29.libopencv_structured_light
add_library(libopencv_structured_light
        STATIC
        IMPORTED )
set_target_properties(libopencv_structured_light
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_structured_light.a)

#30.libopencv_superres
add_library(libopencv_superres
        STATIC
        IMPORTED )
set_target_properties(libopencv_superres
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_superres.a)


#31.libopencv_surface_matching
add_library(libopencv_surface_matching
        STATIC
        IMPORTED )
set_target_properties(libopencv_surface_matching
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_surface_matching.a)

#32.libopencv_text
add_library(libopencv_text
        STATIC
        IMPORTED )
set_target_properties(libopencv_text
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_text.a)

#32.5.libopencv_tracking
add_library(libopencv_tracking
        STATIC
        IMPORTED )
set_target_properties(libopencv_tracking
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_tracking.a)

#33.libopencv_video
add_library(libopencv_video
        STATIC
        IMPORTED )
set_target_properties(libopencv_video
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_video.a)


#34.libopencv_videoio
add_library(libopencv_videoio
        STATIC
        IMPORTED )
set_target_properties(libopencv_videoio
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_videoio.a)

#35.libopencv_videostab
add_library(libopencv_videostab
        STATIC
        IMPORTED )
set_target_properties(libopencv_videostab
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_videostab.a)

#36.libopencv_xfeatures2d
add_library(libopencv_xfeatures2d
        STATIC
        IMPORTED )
set_target_properties(libopencv_xfeatures2d
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_xfeatures2d.a)


#37.libopencv_ximgproc
add_library(libopencv_ximgproc
        STATIC
        IMPORTED )
set_target_properties(libopencv_ximgproc
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_ximgproc.a)

#38.libopencv_xobjdetect
add_library(libopencv_xobjdetect
        STATIC
        IMPORTED )
set_target_properties(libopencv_xobjdetect
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_xobjdetect.a)


#39.libopencv_xphoto
add_library(libopencv_xphoto
        STATIC
        IMPORTED )
set_target_properties(libopencv_xphoto
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libopencv_xphoto.a)

# ncnn
add_library(ncnn
        STATIC
        IMPORTED )
set_target_properties( # Specifies the target library.
        ncnn

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${distribution_DIR}/libncnn.a )

# ncnnvulkan
add_library(glslang
        STATIC
        IMPORTED)
set_target_properties(glslang
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libglslang.a)

add_library(OGLCompiler
        STATIC
        IMPORTED)
set_target_properties(OGLCompiler
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libOGLCompiler.a)

add_library(OSDependent
        STATIC
        IMPORTED)
set_target_properties(OSDependent
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libOSDependent.a)

add_library(SPIRV
        STATIC
        IMPORTED)
set_target_properties(SPIRV
        PROPERTIES
        IMPORTED_LOCATION ${distribution_DIR}/libSPIRV.a)

target_link_libraries( # Specifies the target library.
        native-lib
        # Links the target library to the log library
        # included in the NDK.
        jnigraphics

        libopencv_java3

        libopencv_aruco
        libopencv_bgsegm
        libopencv_bioinspired
        libopencv_calib3d
        libopencv_ccalib

        libopencv_core
        libopencv_datasets
        libopencv_dnn
        libopencv_dpm
        libopencv_face
        libopencv_features2d

        libopencv_flann
        libopencv_fuzzy
        libopencv_highgui
        libopencv_img_hash
        libopencv_imgcodecs
        libopencv_imgproc

        libopencv_line_descriptor
        libopencv_ml
        libopencv_objdetect
        libopencv_optflow
        libopencv_phase_unwrapping

        libopencv_photo
        libopencv_plot
        libopencv_reg
        libopencv_rgbd
        libopencv_saliency

        libopencv_shape
        libopencv_stereo
        libopencv_stitching
        libopencv_structured_light
        libopencv_superres

        libopencv_surface_matching
        libopencv_text
        libopencv_video
        libopencv_tracking
        libopencv_videoio
        libopencv_videostab

        libopencv_xfeatures2d
        libopencv_ximgproc
        libopencv_xobjdetect
        libopencv_xphoto

        ${log-lib}
        vulkan
        ${android-lib}
        ncnn
        glslang
        SPIRV
        OGLCompiler
        OSDependent )