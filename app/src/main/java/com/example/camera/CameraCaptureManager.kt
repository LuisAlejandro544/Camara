package com.example.camera

import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.example.camera.nativeengine.NativeCameraEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Gestor de captura de imágenes utilizando CameraX y MediaStore.
 * Almacena las fotos en el directorio público Pictures/Camara para que
 * sean accesibles inmediatamente y persistentes en el dispositivo.
 * Detecta los megapíxeles reales soportados físicamente por el sensor,
 * analiza soporte para Vulkan 1.1 y aplica el filtro nativo de belleza
 * con preservación de contraste en C++20.
 */
object CameraCaptureManager {

    private const val TAG = "CameraCaptureManager"
    private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss_SSS"

    /**
     * Detecta si el procesador o GPU del teléfono soporta Vulkan 1.1 y obtiene su versión.
     * En Android, la versión se codifica como: (major shl 22) or (minor shl 12) or patch.
     * Vulkan 1.1 requiere major >= 1 y minor >= 1 (valor numérico >= 0x401000).
     */
    fun detectVulkan11Support(context: Context): Pair<Boolean, String> {
        return try {
            val pm = context.packageManager
            val features = pm.systemAvailableFeatures
            var isVulkan11 = false
            var vulkanString = "No soportado"

            for (feature in features) {
                if (feature.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION) {
                    val version = feature.version
                    val major = version shr 22
                    val minor = (version shr 12) and 0x3FF
                    val patch = version and 0xFFF
                    vulkanString = "Vulkan $major.$minor.$patch"
                    if (major > 1 || (major == 1 && minor >= 1)) {
                        isVulkan11 = true
                    }
                    break
                }
            }
            Pair(isVulkan11, vulkanString)
        } catch (e: Exception) {
            Log.e(TAG, "Error detectando Vulkan: ${e.message}")
            Pair(false, "No detectado")
        }
    }

    /**
     * Detecta la versión de OpenGL ES reportada por el sistema.
     */
    fun detectOpenGlVersion(context: Context): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val glInfo = am?.deviceConfigurationInfo?.glEsVersion
            if (!glInfo.isNullOrEmpty()) "OpenGL ES $glInfo" else "OpenGL ES 3.2"
        } catch (_: Exception) {
            "OpenGL ES 3.2"
        }
    }

    /**
     * Inspecciona los metadatos físicos del sensor de la cámara activa mediante Camera2
     * para calcular los megapíxeles máximos reales soportados por el hardware (incluyendo
     * sensores Quad Bayer de 48MP, 50MP, 64MP, 108MP) y verificar posibles restricciones
     * impuestas por la versión de Android o la capa de personalización del fabricante.
     *
     * @param cameraInfo Instancia de [CameraInfo] provista por CameraX.
     * @return [PhotoResolutionInfo] con los megapíxeles y dimensiones reales del sensor.
     */
    fun detectPhotoCapabilities(cameraInfo: CameraInfo): PhotoResolutionInfo {
        return try {
            val camera2Info = Camera2CameraInfo.from(cameraInfo)

            // 1. Tamaño del sensor físico de silicio (Pixel Array Size de la matriz de hardware)
            val pixelArraySize: Size? = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
            )
            val rawPhysicalPixels = pixelArraySize?.let { it.width.toLong() * it.height.toLong() } ?: 0L
            val physicalSensorMp = (rawPhysicalPixels / 1_000_000.0).roundToInt()

            // 2. Mapa estándar de tamaños de flujo JPEG (SCALER_STREAM_CONFIGURATION_MAP)
            val streamMap = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )
            val standardSizes = streamMap?.getOutputSizes(ImageFormat.JPEG)
            val largestStandard = standardSizes?.maxByOrNull { it.width.toLong() * it.height.toLong() }
            val standardPixels = largestStandard?.let { it.width.toLong() * it.height.toLong() } ?: 12_000_000L
            val standardMp = (standardPixels / 1_000_000.0).roundToInt().coerceAtLeast(1)

            // 3. Mapa de ultra-alta resolución (SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION en Android 12+)
            var maxResJpegSizes: Array<Size>? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val maxResMap = camera2Info.getCameraCharacteristic(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION
                    )
                    maxResJpegSizes = maxResMap?.getOutputSizes(ImageFormat.JPEG)
                } catch (_: Exception) {
                }
            }
            val largestMaxRes = maxResJpegSizes?.maxByOrNull { it.width.toLong() * it.height.toLong() }
            val maxResPixels = largestMaxRes?.let { it.width.toLong() * it.height.toLong() } ?: 0L
            val maxResMp = (maxResPixels / 1_000_000.0).roundToInt()

            // Identificar si físicamente el sensor es de ultra-alta resolución (48MP, 50MP, 64MP, 108MP+)
            val isHighResPhysicalSensor = physicalSensorMp >= 36 || maxResMp >= 36 || standardMp >= 36

            val effectiveMaxMp = when {
                maxResMp >= 36 -> maxResMp
                physicalSensorMp >= 36 -> physicalSensorMp
                standardMp >= 36 -> standardMp
                physicalSensorMp > standardMp -> physicalSensorMp
                else -> standardMp
            }.coerceAtLeast(1)

            val effectiveResolutionString = when {
                largestMaxRes != null -> "${largestMaxRes.width} × ${largestMaxRes.height}"
                pixelArraySize != null && physicalSensorMp >= 36 -> "${pixelArraySize.width} × ${pixelArraySize.height}"
                largestStandard != null -> "${largestStandard.width} × ${largestStandard.height}"
                else -> "4000 × 3000"
            }

            // Diagnóstico de restricciones de Android y capas OEM
            val isAndroidVersionRestricted = isHighResPhysicalSensor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            val isOemLayerRestricted = isHighResPhysicalSensor &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (maxResJpegSizes.isNullOrEmpty() || maxResMp < 36) &&
                    standardMp < 36

            val isRestricted = isAndroidVersionRestricted || isOemLayerRestricted

            val restrictionReason = when {
                isAndroidVersionRestricted -> {
                    "Tu dispositivo ejecuta Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}). La API oficial de Ultra Alta Resolución (50MP) fue introducida en Android 12 (API 31). En esta versión, Android entrega el flujo estándar de 12MP optimizado con pixel binning (4 en 1)."
                }
                isOemLayerRestricted -> {
                    val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                    "Tu sensor físico es de ${effectiveMaxMp}MP ($effectiveResolutionString), pero la capa de personalización de $manufacturer bloquea el acceso directo a 50MP para apps de terceros y lo reserva para su app de cámara de fábrica, entregando 12MP (4 en 1) a la API de Android."
                }
                else -> null
            }

            PhotoResolutionInfo(
                maxMegaPixels = effectiveMaxMp,
                maxResolutionString = effectiveResolutionString,
                standardMegaPixels = standardMp,
                hasHighResMode = isHighResPhysicalSensor || standardMp > 16,
                physicalSensorMegaPixels = physicalSensorMp.coerceAtLeast(standardMp),
                isRestrictedByOemOrOs = isRestricted,
                restrictionReason = restrictionReason
            )
        } catch (_: Exception) {
            PhotoResolutionInfo(
                maxMegaPixels = 12,
                maxResolutionString = "4000 × 3000",
                standardMegaPixels = 12,
                hasHighResMode = false,
                physicalSensorMegaPixels = 12,
                isRestrictedByOemOrOs = false,
                restrictionReason = null
            )
        }
    }

    /**
     * Recorta un mapa de bits centralmente para que coincida exactamente con la relación de aspecto deseada.
     * Para 4:3 y 16:9 que ya vienen encuadrados por CameraX en la captura nativa, no hace modificaciones innecesarias.
     * Para 1:1 recorta al cuadrado central.
     * Para FULL recorta según la relación de aspecto de la pantalla del dispositivo.
     */
    fun cropBitmapToAspectRatio(
        source: Bitmap,
        aspectRatio: AspectRatioOption,
        screenWidth: Int = 0,
        screenHeight: Int = 0
    ): Bitmap {
        val srcW = source.width
        val srcH = source.height
        if (srcW <= 0 || srcH <= 0) return source

        val targetRatio: Float = when (aspectRatio) {
            AspectRatioOption.RATIO_1_1 -> 1.0f
            AspectRatioOption.RATIO_4_3 -> 4.0f / 3.0f
            AspectRatioOption.RATIO_16_9 -> 16.0f / 9.0f
            AspectRatioOption.FULL -> {
                if (screenWidth > 0 && screenHeight > 0) {
                    val maxDim = maxOf(screenWidth, screenHeight).toFloat()
                    val minDim = minOf(screenWidth, screenHeight).toFloat()
                    maxDim / minDim
                } else {
                    20.0f / 9.0f
                }
            }
        }

        // Determinamos la orientación de la foto (mayor dimensión)
        val isPortrait = srcH > srcW
        val longerDim = if (isPortrait) srcH else srcW
        val shorterDim = if (isPortrait) srcW else srcH

        val currentRatio = longerDim.toFloat() / shorterDim.toFloat()
        // Si la relación ya es muy cercana (diferencia < 0.03), no recortamos
        if (kotlin.math.abs(currentRatio - targetRatio) < 0.03f) {
            return source
        }

        val targetW: Int
        val targetH: Int
        if (isPortrait) {
            // El alto es targetRatio * ancho deseado
            if (currentRatio > targetRatio) {
                // La imagen es más alargada de lo necesario -> recortar alto
                targetW = srcW
                targetH = (srcW * targetRatio).roundToInt().coerceAtMost(srcH)
            } else {
                // La imagen es más ancha de lo necesario -> recortar ancho
                targetH = srcH
                targetW = (srcH / targetRatio).roundToInt().coerceAtMost(srcW)
            }
        } else {
            // Paisaje
            if (currentRatio > targetRatio) {
                targetH = srcH
                targetW = (srcH * targetRatio).roundToInt().coerceAtMost(srcW)
            } else {
                targetW = srcW
                targetH = (srcW / targetRatio).roundToInt().coerceAtMost(srcH)
            }
        }

        val cropX = ((srcW - targetW) / 2).coerceAtLeast(0)
        val cropY = ((srcH - targetH) / 2).coerceAtLeast(0)

        return try {
            val cropped = Bitmap.createBitmap(source, cropX, cropY, targetW, targetH)
            if (cropped != source) {
                source.recycle()
            }
            cropped
        } catch (e: Exception) {
            Log.e(TAG, "Error recortando a ratio: ${e.message}")
            source
        }
    }

    /**
     * Realiza la toma de una foto utilizando el caso de uso [ImageCapture] configurado.
     * Si el filtro de belleza está activo o la relación de aspecto requiere recorte central (ej. 1:1 o Full),
     * procesa el mapa de bits en C++20 / IO coroutines antes de finalizar el archivo.
     *
     * @param context Contexto de la aplicación.
     * @param imageCapture Instancia activa de [ImageCapture] de CameraX.
     * @param flashMode Modo de flash seleccionado por el usuario.
     * @param aspectRatio Relación de aspecto seleccionada (Full, 16:9, 4:3, 1:1).
     * @param isBeautyFilterEnabled Indica si se debe aplicar el filtro de belleza en C++20.
     * @param beautyIntensity Intensidad del filtro (0.0f a 1.0f).
     * @param isVulkanBackend Si es true ejecuta el pipeline Vulkan 1.1; de lo contrario OpenGL ES 3.2.
     * @param onStart Callback invocado justo al disparar el obturador.
     * @param onSuccess Callback invocado al completar el guardado con éxito.
     * @param onError Callback invocado si ocurre una excepción de captura.
     */
    fun takePhoto(
        context: Context,
        imageCapture: ImageCapture?,
        flashMode: FlashMode,
        aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3,
        isBeautyFilterEnabled: Boolean = false,
        beautyIntensity: Float = 0.6f,
        isVulkanBackend: Boolean = false,
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
                        val needsCrop = aspectRatio == AspectRatioOption.RATIO_1_1 || aspectRatio == AspectRatioOption.FULL
                        val needsBeauty = isBeautyFilterEnabled && NativeCameraEngine.isAvailable()

                        if (needsCrop || needsBeauty) {
                            // Procesamiento en segundo plano en Dispatchers.IO para no bloquear el hilo de interfaz
                            val displayMetrics = context.resources.displayMetrics
                            val screenWidth = displayMetrics.widthPixels
                            val screenHeight = displayMetrics.heightPixels

                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    var bitmap = contentResolver.openInputStream(savedUri)?.use { stream ->
                                        val options = BitmapFactory.Options().apply {
                                            inMutable = true
                                            inPreferredConfig = Bitmap.Config.ARGB_8888
                                        }
                                        BitmapFactory.decodeStream(stream, null, options)
                                    }

                                    if (bitmap != null) {
                                        // 1. Recorte central a la relación de aspecto exacta elegida por el usuario
                                        if (needsCrop) {
                                            bitmap = cropBitmapToAspectRatio(
                                                source = bitmap,
                                                aspectRatio = aspectRatio,
                                                screenWidth = screenWidth,
                                                screenHeight = screenHeight
                                            )
                                        }

                                        // 2. Filtro nativo de belleza en C++20 si fue activado
                                        if (needsBeauty) {
                                            NativeCameraEngine.applyBeautyFilter(
                                                bitmap = bitmap,
                                                intensity = beautyIntensity,
                                                isVulkan = isVulkanBackend
                                            )
                                        }

                                        contentResolver.openOutputStream(savedUri, "wt")?.use { out ->
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
                                        }
                                        bitmap.recycle()
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error procesando imagen guardada: ${e.message}")
                                } finally {
                                    releasePendingAndFinish(contentResolver, savedUri)
                                    withContext(Dispatchers.Main) {
                                        onSuccess(savedUri, savedUri.toString())
                                    }
                                }
                            }
                        } else {
                            releasePendingAndFinish(contentResolver, savedUri)
                            onSuccess(savedUri, savedUri.toString())
                        }
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

    private fun releasePendingAndFinish(contentResolver: android.content.ContentResolver, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val releaseValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            try {
                contentResolver.update(uri, releaseValues, null, null)
            } catch (_: Exception) {
            }
        }
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
