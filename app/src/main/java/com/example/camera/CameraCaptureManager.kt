package com.example.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Gestor de captura de imágenes utilizando CameraX y MediaStore.
 * Almacena las fotos en el directorio público Pictures/Camara para que
 * sean accesibles inmediatamente y persistentes en el dispositivo.
 * Detecta los megapíxeles reales soportados físicamente por el sensor.
 */
object CameraCaptureManager {

    private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss_SSS"

    /**
     * Inspecciona los metadatos físicos del sensor de la cámara activa mediante Camera2
     * para calcular los megapíxeles máximos reales soportados por el hardware.
     *
     * @param cameraInfo Instancia de [CameraInfo] provista por CameraX.
     * @return [PhotoResolutionInfo] con los megapíxeles y dimensiones reales del sensor.
     */
    fun detectPhotoCapabilities(cameraInfo: CameraInfo): PhotoResolutionInfo {
        return try {
            val camera2Info = Camera2CameraInfo.from(cameraInfo)
            val streamMap = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )
            val jpegSizes: Array<Size>? = streamMap?.getOutputSizes(ImageFormat.JPEG)

            if (!jpegSizes.isNullOrEmpty()) {
                // Ordenar por área total (ancho * alto) descendente
                val sortedSizes = jpegSizes.sortedByDescending { it.width.toLong() * it.height.toLong() }
                val largestSize = sortedSizes.first()
                val totalPixels = largestSize.width.toLong() * largestSize.height.toLong()
                val maxMp = (totalPixels / 1_000_000.0).roundToInt()

                // Si hay tamaños más pequeños estándar (alrededor de 12MP), detectamos si hay salto de resolución
                val hasHighRes = maxMp > 16 || sortedSizes.size > 1
                val standardMp = if (maxMp > 16) 12 else maxMp

                PhotoResolutionInfo(
                    maxMegaPixels = maxMp.coerceAtLeast(1),
                    maxResolutionString = "${largestSize.width} × ${largestSize.height}",
                    standardMegaPixels = standardMp,
                    hasHighResMode = hasHighRes
                )
            } else {
                PhotoResolutionInfo(
                    maxMegaPixels = 12,
                    maxResolutionString = "4000 × 3000",
                    standardMegaPixels = 12,
                    hasHighResMode = false
                )
            }
        } catch (_: Exception) {
            PhotoResolutionInfo(
                maxMegaPixels = 12,
                maxResolutionString = "4000 × 3000",
                standardMegaPixels = 12,
                hasHighResMode = false
            )
        }
    }

    /**
     * Realiza la toma de una foto utilizando el caso de uso [ImageCapture] configurado.
     *
     * @param context Contexto de la aplicación.
     * @param imageCapture Instancia activa de [ImageCapture] de CameraX.
     * @param flashMode Modo de flash seleccionado por el usuario.
     * @param onStart Callback invocado justo al disparar el obturador.
     * @param onSuccess Callback invocado al completar el guardado con éxito.
     * @param onError Callback invocado si ocurre una excepción de captura.
     */
    fun takePhoto(
        context: Context,
        imageCapture: ImageCapture?,
        flashMode: FlashMode,
        onStart: () -> Unit,
        onSuccess: (savedUri: Uri, path: String) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        if (imageCapture == null) {
            onError("La cámara no está lista para capturar")
            return
        }

        // Aplica el modo de flash en el caso de uso ImageCapture
        imageCapture.flashMode = when (flashMode) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }

        val name = "IMG_" + SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Camara")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val contentResolver = context.contentResolver
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        onStart()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    if (savedUri != null) {
                        // Liberar marca IS_PENDING en Android 10+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val releaseValues = ContentValues().apply {
                                put(MediaStore.Images.Media.IS_PENDING, 0)
                            }
                            try {
                                contentResolver.update(savedUri, releaseValues, null, null)
                            } catch (_: Exception) {
                            }
                        }
                        onSuccess(savedUri, savedUri.toString())
                    } else {
                        // Fallback con archivo local si la URI no estuviera disponible
                        saveToLocalFileFallback(context, imageCapture, onStart, onSuccess, onError)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    // Intento de fallback en almacenamiento privado en caso de error en MediaStore
                    saveToLocalFileFallback(context, imageCapture, onStart, onSuccess, onError)
                }
            }
        )
    }

    /**
     * Método de respaldo si MediaStore falla o no está disponible en el entorno.
     */
    private fun saveToLocalFileFallback(
        context: Context,
        imageCapture: ImageCapture,
        onStart: () -> Unit,
        onSuccess: (savedUri: Uri, path: String) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        try {
            val outputDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.filesDir
            val photoFile = File(
                outputDirectory,
                "IMG_" + SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date()) + ".jpg"
            )

            val fallbackOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                fallbackOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val uri = Uri.fromFile(photoFile)
                        onSuccess(uri, photoFile.absolutePath)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        onError("Error al guardar la foto: ${exception.message}")
                    }
                }
            )
        } catch (e: Exception) {
            onError("Error al inicializar archivo de foto: ${e.message}")
        }
    }
}
