package com.example.camera

import android.net.Uri
import androidx.camera.video.Quality

/**
 * Modos de captura disponibles en la cámara: Fotografía o Vídeo.
 */
enum class CaptureMode {
    PHOTO,
    VIDEO
}

/**
 * Modos de flash disponibles para la captura de fotos o luz de relleno de vídeo.
 */
enum class FlashMode {
    AUTO,
    ON,
    OFF
}

/**
 * Orientación de la lente seleccionada.
 */
enum class LensFacing {
    BACK,
    FRONT
}

/**
 * Opciones de resolución de vídeo basadas en las calidades estándar de CameraX.
 * Cada opción encapsula la calidad del framework, la etiqueta amigable y la resolución nominal.
 */
enum class VideoQualityOption(
    val quality: Quality,
    val label: String,
    val resolutionHint: String
) {
    UHD(Quality.UHD, "4K UHD", "3840 × 2160"),
    FHD(Quality.FHD, "Full HD", "1920 × 1080"),
    HD(Quality.HD, "HD 720p", "1280 × 720"),
    SD(Quality.SD, "SD 480p", "720 × 480");

    companion object {
        /**
         * Mapea un objeto [Quality] de CameraX a su respectivo [VideoQualityOption].
         */
        fun fromQuality(q: Quality): VideoQualityOption? {
            return entries.firstOrNull { it.quality == q }
        }
    }
}

/**
 * Información de capacidades de hardware detectadas para grabación de vídeo.
 *
 * @param supportedQualities Lista ordenada de resoluciones soportadas por la cámara activa.
 * @param supportedFps Lista de velocidades de cuadros por segundo detectadas para el sensor.
 */
data class VideoCapabilitiesInfo(
    val supportedQualities: List<VideoQualityOption> = listOf(VideoQualityOption.FHD, VideoQualityOption.HD),
    val supportedFps: List<Int> = listOf(30)
)

/**
 * Estado inmutable de la interfaz de la cámara.
 *
 * @param captureMode Modo de captura actual (FOTO o VÍDEO).
 * @param lensFacing Lente actual (trasera o frontal).
 * @param flashMode Modo de flash seleccionado.
 * @param isCapturing Indica si actualmente se está procesando la captura de una foto.
 * @param isRecordingVideo Indica si se está grabando activamente un vídeo.
 * @param recordingDurationSeconds Segundos transcurridos en la grabación del vídeo actual.
 * @param showShutterFlash Dispara la animación de destello visual en pantalla al tomar la foto.
 * @param lastPhotoUri URI del último archivo multimedia capturado para mostrar en la miniatura.
 * @param lastPhotoPath Ruta local del archivo de la última foto o vídeo tomado.
 * @param isGridEnabled Indica si la cuadrícula de regla de tercios está visible.
 * @param zoomRatio Nivel actual de zoom de la cámara.
 * @param selectedPhotoForPreview URI de la foto seleccionada para visualizar a pantalla completa.
 * @param userMessage Mensaje informativo o de error para mostrar al usuario.
 * @param supportedVideoQualities Resoluciones de vídeo detectadas para el sensor activo.
 * @param selectedVideoQuality Resolución seleccionada actualmente por el usuario.
 * @param supportedFps Tasas de FPS soportadas por el sensor (ej. 30, 60).
 * @param selectedFps Tasa de FPS seleccionada para la grabación.
 * @param isAudioEnabled Indica si el usuario desea grabar vídeo con audio.
 * @param hasAudioPermission Indica si la app cuenta con el permiso de micrófono otorgado.
 * @param isVideoSettingsOpen Controla la apertura del diálogo de ajustes de resolución y FPS de vídeo.
 */
data class CameraUiState(
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val lensFacing: LensFacing = LensFacing.BACK,
    val flashMode: FlashMode = FlashMode.AUTO,
    val isCapturing: Boolean = false,
    val isRecordingVideo: Boolean = false,
    val recordingDurationSeconds: Long = 0L,
    val showShutterFlash: Boolean = false,
    val lastPhotoUri: Uri? = null,
    val lastPhotoPath: String? = null,
    val isGridEnabled: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val selectedPhotoForPreview: Uri? = null,
    val userMessage: String? = null,
    val supportedVideoQualities: List<VideoQualityOption> = listOf(VideoQualityOption.FHD, VideoQualityOption.HD),
    val selectedVideoQuality: VideoQualityOption = VideoQualityOption.FHD,
    val supportedFps: List<Int> = listOf(30),
    val selectedFps: Int = 30,
    val isAudioEnabled: Boolean = true,
    val hasAudioPermission: Boolean = false,
    val isVideoSettingsOpen: Boolean = false
)
