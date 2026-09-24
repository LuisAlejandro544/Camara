package com.example.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.core.DynamicRange
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestor modular de grabación de vídeo con CameraX y MediaStore.
 *
 * Responsabilidades:
 * - Detección de capacidades de hardware del sensor activo (resoluciones soportadas y rangos de FPS).
 * - Inicio, control y detención de la grabación de vídeo.
 * - Integración con MediaStore en el directorio público Movies/Camara.
 * - Manejo seguro de audio con verificación previa de permisos.
 */
object CameraVideoManager {

    private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss_SSS"

    /**
     * Inspecciona la cámara activa para determinar qué calidades de vídeo y velocidades de
     * cuadros por segundo (FPS) admite físicamente el sensor del teléfono.
     *
     * @param cameraInfo Información de hardware provista por CameraX.
     * @return [VideoCapabilitiesInfo] con las resoluciones y FPS detectados.
     */
    fun detectVideoCapabilities(cameraInfo: CameraInfo): VideoCapabilitiesInfo {
        // 1. Detección de calidades y resoluciones mediante QualitySelector
        val supportedQualities = try {
            val rawQualities = QualitySelector.getSupportedQualities(cameraInfo)
            val mapped = rawQualities.mapNotNull { VideoQualityOption.fromQuality(it) }.toMutableList()

            // Consultar si el sensor físico soporta resolución 2K/QHD o 4K mediante CameraCharacteristics
            val camera2Info = Camera2CameraInfo.from(cameraInfo)
            val streamMap = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )
            val outputSizes = streamMap?.getOutputSizes(android.media.MediaRecorder::class.java)
            val has2KOrHigher = outputSizes?.any { it.width >= 2560 || it.height >= 1440 } == true

            // Si el hardware soporta 2K (2560x1440) o superior y no está en la lista, incluirlo
            if (has2KOrHigher && !mapped.contains(VideoQualityOption.QHD)) {
                val insertIndex = if (mapped.contains(VideoQualityOption.UHD)) 1 else 0
                mapped.add(insertIndex, VideoQualityOption.QHD)
            }

            if (mapped.isNotEmpty()) mapped else listOf(VideoQualityOption.FHD, VideoQualityOption.HD)
        } catch (_: Exception) {
            listOf(VideoQualityOption.QHD, VideoQualityOption.FHD, VideoQualityOption.HD)
        }

        // 2. Detección de FPS disponibles en el hardware mediante Camera2CameraInfo
        val supportedFps = try {
            val camera2Info = Camera2CameraInfo.from(cameraInfo)
            val fpsRanges = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
            )
            val fpsSet = mutableSetOf<Int>()
            fpsRanges?.forEach { range ->
                val upper = range.upper
                if (upper in 24..120) {
                    fpsSet.add(upper)
                }
            }
            if (fpsSet.isNotEmpty()) fpsSet.sorted() else listOf(30)
        } catch (_: Exception) {
            listOf(30)
        }

        // 3. Detección de soporte de Alto Rango Dinámico (HDR de 10 bits) en el sensor
        val isHdrSupported = try {
            val supportedRanges = cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.HLG_10_BIT, DynamicRange.HDR10_10_BIT))
            supportedRanges.any { it == DynamicRange.HLG_10_BIT || it == DynamicRange.HDR10_10_BIT }
        } catch (_: Exception) {
            false
        }

        return VideoCapabilitiesInfo(
            supportedQualities = supportedQualities,
            supportedFps = supportedFps,
            isHdrSupported = isHdrSupported
        )
    }

    /**
     * Inicia una sesión de grabación de vídeo utilizando el caso de uso [VideoCapture].
     *
     * @param context Contexto de la aplicación.
     * @param videoCapture Instancia activa de [VideoCapture] de CameraX.
     * @param isAudioEnabled Indica si se debe incluir pista de audio.
     * @param onStarted Callback al comenzar la grabación.
     * @param onDurationUpdate Callback periódico con los segundos grabados.
     * @param onSuccess Callback al finalizar y persistir el vídeo exitosamente en el dispositivo.
     * @param onError Callback invocado si ocurre un error durante la grabación.
     * @return Manejador [Recording] para pausar o detener la sesión.
     */
    fun startRecording(
        context: Context,
        videoCapture: VideoCapture<Recorder>?,
        isAudioEnabled: Boolean,
        onStarted: () -> Unit,
        onDurationUpdate: (Long) -> Unit,
        onSuccess: (savedUri: Uri, path: String) -> Unit,
        onError: (errorMessage: String) -> Unit
    ): Recording? {
        if (videoCapture == null) {
            onError("El módulo de vídeo no está inicializado")
            return null
        }

        val name = "VID_" + SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Camara")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        var pendingRecording = videoCapture.output.prepareRecording(context, mediaStoreOutput)

        // Verificación de permiso de audio antes de adjuntar el canal de sonido
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (isAudioEnabled && hasAudioPermission) {
            try {
                pendingRecording = pendingRecording.withAudioEnabled()
            } catch (_: SecurityException) {
                // Si el permiso fuera revocado concurrentemente, continúa sin audio de manera segura
            }
        }

        return try {
            pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        onStarted()
                    }
                    is VideoRecordEvent.Status -> {
                        val durationSeconds = event.recordingStats.recordedDurationNanos / 1_000_000_000L
                        onDurationUpdate(durationSeconds)
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            val outputUri = event.outputResults.outputUri
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val releaseValues = ContentValues().apply {
                                    put(MediaStore.Video.Media.IS_PENDING, 0)
                                }
                                try {
                                    context.contentResolver.update(outputUri, releaseValues, null, null)
                                } catch (_: Exception) {
                                }
                            }
                            onSuccess(outputUri, outputUri.toString())
                        } else {
                            // Si el error ocurrió al guardar en MediaStore, intentamos informar claramente
                            val errorCause = event.cause?.message ?: "Código de error: ${event.error}"
                            onError("Error al grabar vídeo: $errorCause")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            onError("No se pudo iniciar la grabación: ${e.message}")
            null
        }
    }
}
