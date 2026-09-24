# Configuración de compilación CMake para el motor nativo de procesamiento de imagen de Cámara.
cmake_minimum_required(VERSION 3.22.1)

project("camera_engine" LANGUAGES CXX)

# Habilitar el estándar moderno C++20
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Definir la biblioteca compartida nativa (.so)
add_library(
    camera_engine
    SHARED
    native-camera-engine.cpp
)

# Buscar bibliotecas del sistema Android requeridas
find_library(log-lib log)
find_library(android-lib android)
find_library(camera-ndk-lib camera2ndk)
find_library(mediandk-lib mediandk)

# Enlazar dependencias del sistema al motor nativo
target_link_libraries(
    camera_engine
    ${log-lib}
    ${android-lib}
    ${camera-ndk-lib}
    ${mediandk-lib}
)

# Opciones de optimización y avisos
target_compile_options(
    camera_engine
    PRIVATE
    -Wall
    -Wextra
    -O3
    -fvisibility=hidden
)
