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
 * @param maxMegaPixels Cantidad de megapíxeles detectados o seleccionables (ej. 48, 50, 64, 12, etc.).
 * @param maxResolutionString Cadena con la resolución en píxeles (ej. "8192 × 6144" o "4000 × 3000").
 * @param standardMegaPixels Megapíxeles por defecto en modo agrupado/binning (ej. 12).
 * @param hasHighResMode Indica si el hardware ofrece un modo de resolución nativa completa.
 * @param physicalSensorMegaPixels Megapíxeles reales de la matriz de silicio física (ej. 50MP).
 * @param isRestrictedByOemOrOs Indica si la capa de personalización (OEM) o la versión de Android limitan el acceso directo al flujo de 50MP para apps de terceros.
 * @param restrictionReason Explicación técnica detallada sobre la restricción de Android o de la capa del fabricante.
 */
data class PhotoResolutionInfo(
    val maxMegaPixels: Int = 12,
    val maxResolutionString: String = "4000 × 3000",
    val standardMegaPixels: Int = 12,
    val hasHighResMode: Boolean = false,
    val physicalSensorMegaPixels: Int = 12,
    val isRestrictedByOemOrOs: Boolean = false,
    val restrictionReason: String? = null
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
    MONOCHROME("B&N Artístico", "Blanco y negro con sombras profundas y nitidez"),
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
 * Relaciones de aspecto (Aspect Ratio) disponibles para la captura y visor de la cámara.
 * - FULL: Ocupa toda la pantalla del teléfono (relación nativa del display como 20:9 o 19.5:9).
 * - RATIO_16_9: Formato panorámico estándar para vídeo, historias y estados.
 * - RATIO_4_3: Formato nativo del sensor físico de la cámara (máxima resolución sin recorte).
 * - RATIO_1_1: Formato cuadrado ideal para retratos de perfil y redes sociales.
 */
enum class AspectRatioOption(
    val label: String,
    val description: String
) {
    FULL("Full", "Pantalla Completa"),
    RATIO_16_9("16:9", "Panorámico 16:9"),
    RATIO_4_3("4:3", "Sensor Completo 4:3"),
    RATIO_1_1("1:1", "Cuadrado 1:1")
}

/**
 * Opciones de temporizador de disparo para fotografía (hasta un máximo de 10 segundos).
 * - OFF: Disparo instantáneo sin cuenta regresiva.
 * - SEC_3: Cuenta regresiva de 3 segundos (ideal para evitar trepidación y autorretratos rápidos).
 * - SEC_5: Cuenta regresiva de 5 segundos (tiempo equilibrado para preparar la pose).
 * - SEC_10: Cuenta regresiva de 10 segundos (tiempo máximo estándar para fotos grupales o con trípode).
 */
enum class TimerOption(
    val seconds: Int,
    val label: String,
    val description: String
) {
    OFF(0, "OFF", "Desactivado"),
    SEC_3(3, "3s", "3 segundos"),
    SEC_5(5, "5s", "5 segundos"),
    SEC_10(10, "10s", "10 segundos")
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
 * Backend de renderizado gráfico disponible para la ejecución de filtros en C++20.
 *
 * @param id Identificador técnico interno.
 * @param label Nombre legible para la interfaz de usuario.
 * @param apiName Nombre formal de la API de renderizado.
 * @param description Descripción técnica del pipeline.
 */
enum class GraphicsFilterBackend(
    val id: String,
    val label: String,
    val apiName: String,
    val description: String
) {
    VULKAN(
        "vulkan",
        "Vulkan 1.1",
        "Vulkan Compute Pipeline",
        "Cálculo paralelo de alto rendimiento y baja sobrecarga en GPU mediante la API Vulkan 1.1"
    ),
    OPENGL_ES(
        "opengl_es",
        "OpenGL ES 3.2",
        "OpenGL ES Shaders Pipeline",
        "Canal de shaders acelerados por GPU compatible con el 100% de dispositivos Android"
    )
}

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
 * @param isVulkan11Supported Indica si la GPU y procesador del teléfono soportan la especificación Vulkan 1.1+.
 * @param vulkanVersionCode Código de versión numérica reportado por la plataforma Vulkan.
 * @param vulkanVersionString Cadena legible de la versión de Vulkan soportada.
 * @param openGlVersionString Cadena legible de la versión de OpenGL ES del dispositivo.
 * @param selectedGraphicsBackend Backend de renderizado gráfico seleccionado para los filtros (Vulkan o OpenGL ES).
 * @param isBeautyFilterEnabled Indica si el filtro de belleza y suavizado con preservación de contraste está activo.
 * @param beautyFilterIntensity Intensidad del filtro de belleza (0.0f a 1.0f).
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
    val isHighResInfoDialogOpen: Boolean = false,
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
    val isExposureCompensationSupported: Boolean = true,
    val isVulkan11Supported: Boolean = false,
    val vulkanVersionCode: Int = 0,
    val vulkanVersionString: String = "No detectado",
    val openGlVersionString: String = "OpenGL ES 3.2",
    val selectedGraphicsBackend: GraphicsFilterBackend = GraphicsFilterBackend.OPENGL_ES,
    val isBeautyFilterEnabled: Boolean = false,
    val beautyFilterIntensity: Float = 0.6f,
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3,
    val isAspectRatioSelectorOpen: Boolean = false,
    val timerOption: TimerOption = TimerOption.OFF,
    val activeTimerSecondsRemaining: Int? = null,
    val isTimerSelectorOpen: Boolean = false,
    val isFilterSelectorOpen: Boolean = false,
    val isWatermarkEnabled: Boolean = true,
    val isAiModeEnabled: Boolean = true
)
