#include <jni.h>
#include <string>
#include <memory>
#include <vector>
#include <android/log.h>

#define TAG "CameraNativeEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/**
 * Motor nativo en C++20 para procesamiento de imagen y cámara de alto rendimiento.
 *
 * Este archivo establece la base nativa compilada directamente en el APK final
 * para ambas arquitecturas (32 bits 'armeabi-v7a' y 64 bits 'arm64-v8a').
 * Servirá de cimiento para algoritmos avanzados de fotografía computacional,
 * descompresión RAW (DNG) y filtros en tiempo real sin pausas de Garbage Collection.
 */

namespace camera::engine {

    struct EngineInfo {
        std::string name = "CameraEngine-Native";
        std::string standard = "C++20";
        int major = 1;
        int minor = 0;
        int patch = 0;

        [[nodiscard]] std::string getFullVersionString() const {
            return name + " v" + std::to_string(major) + "." +
                   std::to_string(minor) + "." + std::to_string(patch) +
                   " (" + standard + ")";
        }
    };

    static std::unique_ptr<EngineInfo> s_engineInfo = nullptr;

    void initializeEngine() {
        if (!s_engineInfo) {
            s_engineInfo = std::make_unique<EngineInfo>();
            LOGI("Motor nativo C++20 inicializado con éxito.");
        }
    }

} // namespace camera::engine

extern "C" {

/**
 * Devuelve la versión y estado del motor nativo en C++20.
 */
JNIEXPORT jstring JNICALL
Java_com_example_camera_nativeengine_NativeCameraEngine_getEngineVersion(
        JNIEnv *env,
        jobject /* this */) {
    camera::engine::initializeEngine();
    std::string versionStr = camera::engine::s_engineInfo->getFullVersionString();
    return env->NewStringUTF(versionStr.c_str());
}

/**
 * Confirma que la biblioteca nativa (.so) se cargó correctamente y está disponible.
 */
JNIEXPORT jboolean JNICALL
Java_com_example_camera_nativeengine_NativeCameraEngine_isNativeEngineAvailable(
        JNIEnv * /* env */,
        jobject /* this */) {
    return JNI_TRUE;
}

} // extern "C"
