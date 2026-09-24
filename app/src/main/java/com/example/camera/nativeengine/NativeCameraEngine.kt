package com.example.camera.nativeengine

import android.graphics.Bitmap
import android.util.Log

/**
 * Puente JNI hacia el motor nativo de procesamiento de imagen en C++20.
 *
 * Este componente carga la biblioteca nativa (.so) empaquetada en el APK final,
 * ofreciendo la base para ejecutar procesamiento de imagen de alto rendimiento
 * y bajo consumo de memoria para arquitecturas de 32 bits (armeabi-v7a) y 64 bits (arm64-v8a).
 */
object NativeCameraEngine {

    private const val TAG = "NativeCameraEngine"

    private var isLoaded = false

    init {
        try {
            System.loadLibrary("camera_engine")
            isLoaded = true
            Log.i(TAG, "Librería nativa 'camera_engine' (.so) cargada exitosamente.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "No se pudo cargar la librería nativa camera_engine: ${e.message}")
            isLoaded = false
        }
    }

    /**
     * Retorna si el motor nativo en C++20 está disponible y listo para operar.
     */
    fun isAvailable(): Boolean {
        return isLoaded && try {
            isNativeEngineAvailable()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retorna la cadena de versión y estándar de C++ compilado en el motor.
     */
    fun getVersion(): String {
        return if (isLoaded) {
            try {
                getEngineVersion()
            } catch (e: Exception) {
                "C++20 (Enlace pendiente)"
            }
        } else {
            "Motor nativo no disponible"
        }
    }

    /**
     * Aplica el filtro de belleza y suavizado con preservación de contraste sobre un [Bitmap].
     *
     * @param bitmap Imagen a procesar en formato ARGB_8888.
     * @param intensity Intensidad del suavizado (0.0f a 1.0f).
     * @param isVulkan Si es true utiliza el pipeline de Vulkan 1.1; de lo contrario OpenGL ES 3.2.
     * @return true si se procesó exitosamente en C++20; false en caso de error.
     */
    fun applyBeautyFilter(bitmap: Bitmap, intensity: Float, isVulkan: Boolean): Boolean {
        if (!isLoaded) return false
        val backendInt = if (isVulkan) 0 else 1
        return try {
            applyBeautyFilter(bitmap, intensity.coerceIn(0.0f, 1.0f), backendInt)
        } catch (e: Exception) {
            Log.e(TAG, "Error al invocar applyBeautyFilter nativo: ${e.message}")
            false
        }
    }

    private external fun getEngineVersion(): String
    private external fun isNativeEngineAvailable(): Boolean
    private external fun applyBeautyFilter(bitmap: Bitmap, intensity: Float, backend: Int): Boolean
}
