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
 * Opciones de resolución de vídeo basadas en las calidades estándar de CameraX y Camera2.
 * Cada opción encapsula la calidad del framework, la etiqueta amigable y la resolución nominal.
 * Se evita nombrar Full HD como único tope, dando soporte a resoluciones superiores como 4K UHD y 2K / QHD.
 */
enum class VideoQualityOption(
    val quality: Quality,
    val label: String,
    val resolutionHint: String
) {
    UHD(Quality.UHD, "4K UHD", "3840 × 2160"),
    QHD(Quality.HIGHEST, "2K • QHD", "2560 × 1440"),
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
 * Información de capacidades de resolución fotográfica física del sensor.
 *
 * @param maxMegaPixels Cantidad de megapíxeles reales soportados por el sensor de hardware (ej. 48, 50, 64, 12, etc.).
 * @param maxResolutionString Cadena con la resolución en píxeles (ej. "8192 × 6144").
 * @param standardMegaPixels Megapíxeles por defecto en modo agrupado/binning (ej. 12).
 * @param hasHighResMode Indica si el hardware ofrece un modo de resolución nativa completa.
 */
data class PhotoResolutionInfo(
    val maxMegaPixels: Int = 12,
    val maxResolutionString: String = "4000 × 3000",
    val standardMegaPixels: Int = 12,
    val hasHighResMode: Boolean = false
)

/**
 * Perfiles de color y contraste para eliminar el efecto de colores lavados o pasteles.
 */
enum class ColorProfileOption(
    val label: String,
    val description: String
) {
    VIVID_ANTI_WASHED("Vívido Antilavado", "Colores ricos, cielos saturados y negros profundos"),
    DEEP_CONTRAST("Alto Contraste", "Curva S pronunciada con sombras densas"),
    WARM_NATURAL("Cálido Natural", "Tonos cálidos y balance natural orgánico"),
    STANDARD("Estándar", "Calibración original del sensor sin modificaciones")
}

/**
 * Rango dinámico para grabación de vídeo: Estándar (SDR 8-bit) o Alto Rango Dinámico (HDR 10-bit HLG).
 */
enum class DynamicRangeOption(
    val label: String,
    val description: String
) {
    SDR("SDR", "Estándar 8-bit"),
    HDR_HLG("HDR (10-bit)", "Alto Rango Dinámico HLG 10-bit")
}

/**
 * Información de capacidades de hardware detectadas para grabación de vídeo.
 *
 * @param supportedQualities Lista ordenada de resoluciones soportadas por la cámara activa.
 * @param supportedFps Lista de velocidades de cuadros por segundo detectadas para el sensor.
 * @param isHdrSupported Indica si el hardware del sensor admite grabación de vídeo en HDR de 10 bits.
 */
data class VideoCapabilitiesInfo(
    val supportedQualities: List<VideoQualityOption> = listOf(VideoQualityOption.FHD, VideoQualityOption.HD),
    val supportedFps: List<Int> = listOf(30),
    val isHdrSupported: Boolean = false
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
 * @param minZoomRatio Nivel de zoom mínimo soportado por el hardware de la cámara activa.
 * @param maxZoomRatio Nivel de zoom máximo soportado por el hardware de la cámara activa (sin límites artificiales).
 * @param selectedPhotoForPreview URI de la foto seleccionada para visualizar a pantalla completa.
 * @param userMessage Mensaje informativo o de error para mostrar al usuario.
 * @param supportedVideoQualities Resoluciones de vídeo detectadas para el sensor activo.
 * @param selectedVideoQuality Resolución seleccionada actualmente por el usuario.
 * @param supportedFps Tasas de FPS soportadas por el sensor (ej. 30, 60).
 * @param selectedFps Tasa de FPS seleccionada para la grabación.
 * @param isAudioEnabled Indica si el usuario desea grabar vídeo con audio.
 * @param hasAudioPermission Indica si la app cuenta con el permiso de micrófono otorgado.
 * @param isVideoSettingsOpen Controla la apertura del diálogo de ajustes de resolución y FPS de vídeo.
 * @param isMaxMegapixelsEnabled Indica si el modo de máxima resolución del sensor de hardware está activo.
 * @param photoResolutionInfo Capacidades de resolución física detectadas en el sensor de la cámara activa.
 * @param isDeviceSteady Indica en tiempo real si el usuario mantiene el teléfono quieto.
 * @param isHdrSupported Indica si el hardware del sensor admite grabación de vídeo en HDR de 10 bits.
 * @param isHdrVideoEnabled Indica si el modo de grabación HDR de 10 bits está activado por el usuario.
 * @param isSettingsOpen Controla si el menú independiente de configuración está abierto.
 * @param isColorCalibrationOpen Controla si la pantalla de calibración de color está abierta.
 * @param isAntiWashedModeEnabled Indica si el modo de corrección antilavado está activo.
 * @param colorProfile Perfil de color seleccionado actualmente.
 * @param exposureCompensationEv Valor de compensación de exposición en EV (ej: -0.7f, 0.0f).
 * @param exposureCompensationIndex Índice de paso de compensación enviado a CameraControl.
 * @param minExposureIndex Índice mínimo de compensación soportado por el hardware.
 * @param maxExposureIndex Índice máximo de compensación soportado por el hardware.
 * @param exposureStep Paso de exposición reportado por el hardware (típicamente 0.333f o 0.5f).
 * @param isExposureCompensationSupported Indica si el hardware del sensor admite compensación de exposición.
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
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 10.0f,
    val selectedPhotoForPreview: Uri? = null,
    val userMessage: String? = null,
    val supportedVideoQualities: List<VideoQualityOption> = listOf(VideoQualityOption.FHD, VideoQualityOption.HD),
    val selectedVideoQuality: VideoQualityOption = VideoQualityOption.FHD,
    val supportedFps: List<Int> = listOf(30),
    val selectedFps: Int = 30,
    val isAudioEnabled: Boolean = true,
    val hasAudioPermission: Boolean = false,
    val isVideoSettingsOpen: Boolean = false,
    val isMaxMegapixelsEnabled: Boolean = false,
    val photoResolutionInfo: PhotoResolutionInfo = PhotoResolutionInfo(),
    val isDeviceSteady: Boolean = true,
    val isHdrSupported: Boolean = false,
    val isHdrVideoEnabled: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isColorCalibrationOpen: Boolean = false,
    val isAntiWashedModeEnabled: Boolean = true,
    val colorProfile: ColorProfileOption = ColorProfileOption.VIVID_ANTI_WASHED,
    val exposureCompensationEv: Float = -0.7f,
    val exposureCompensationIndex: Int = -2,
    val minExposureIndex: Int = -6,
    val maxExposureIndex: Int = 6,
    val exposureStep: Float = 0.33333334f,
    val isExposureCompensationSupported: Boolean = true
)
