package com.example.camera

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel que gestiona la lógica de estado y operaciones de la cámara (Fotografía y Vídeo).
 * Todas las operaciones de entrada/salida (I/O) y borrado de archivos se ejecutan
 * en corrutinas en el hilo secundario [Dispatchers.IO] para asegurar un rendimiento fluido.
 */
class CameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // Job para la cuenta regresiva del temporizador de disparo
    private var timerJob: Job? = null

    /**
     * Alterna el modo de captura entre Fotografía y Vídeo.
     */
    fun setCaptureMode(mode: CaptureMode) {
        // No permite cambiar de modo mientras se graba un vídeo
        if (_uiState.value.isRecordingVideo) return
        if (mode == CaptureMode.VIDEO) {
            cancelCountdown()
        }
        _uiState.update {
            it.copy(
                captureMode = mode,
                isTimerSelectorOpen = false,
                isAspectRatioSelectorOpen = false
            )
        }
    }

    /**
     * Registra las capacidades físicas detectadas en el sensor activo (resoluciones y FPS).
     */
    fun setVideoCapabilities(capabilities: VideoCapabilitiesInfo) {
        _uiState.update { currentState ->
            val validQuality = if (capabilities.supportedQualities.contains(currentState.selectedVideoQuality)) {
                currentState.selectedVideoQuality
            } else {
                capabilities.supportedQualities.firstOrNull() ?: VideoQualityOption.FHD
            }

            val validFps = if (capabilities.supportedFps.contains(currentState.selectedFps)) {
                currentState.selectedFps
            } else {
                capabilities.supportedFps.firstOrNull() ?: 30
            }

            currentState.copy(
                supportedVideoQualities = capabilities.supportedQualities,
                selectedVideoQuality = validQuality,
                supportedFps = capabilities.supportedFps,
                selectedFps = validFps,
                isHdrSupported = capabilities.isHdrSupported,
                isHdrVideoEnabled = currentState.isHdrVideoEnabled
            )
        }
    }

    /**
     * Establece la resolución deseada para la grabación de vídeo.
     */
    fun setVideoQuality(quality: VideoQualityOption) {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { it.copy(selectedVideoQuality = quality) }
    }

    /**
     * Establece la tasa de cuadros por segundo (FPS) deseada.
     */
    fun setVideoFps(fps: Int) {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { it.copy(selectedFps = fps) }
    }

    /**
     * Alterna la inclusión de pista de audio en la grabación de vídeo.
     */
    fun toggleAudioRecording() {
        _uiState.update { it.copy(isAudioEnabled = !it.isAudioEnabled) }
    }

    /**
     * Alterna la grabación de vídeo en Alto Rango Dinámico (HDR).
     * Aplica 10-bit HLG/HDR10 o modo de escena HDR por hardware según las capacidades del sensor.
     */
    fun toggleHdrVideo() {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { currentState ->
            val newHdrState = !currentState.isHdrVideoEnabled
            val message = if (newHdrState) {
                if (currentState.isHdrSupported) {
                    "Vídeo HDR activado (Alto Rango Dinámico • Hardware)"
                } else {
                    "Vídeo HDR activado (Modo HDR adaptativo)"
                }
            } else {
                "Vídeo estándar (SDR) activado"
            }
            currentState.copy(
                isHdrVideoEnabled = newHdrState,
                userMessage = message
            )
        }
    }

    /**
     * Actualiza el estado de concesión del permiso de micrófono.
     */
    fun setHasAudioPermission(hasPermission: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = hasPermission) }
    }

    /**
     * Abre o cierra el diálogo de ajustes de resolución y FPS de vídeo.
     */
    fun openVideoSettings(isOpen: Boolean) {
        _uiState.update { it.copy(isVideoSettingsOpen = isOpen) }
    }

    /**
     * Notifica el inicio activo de una sesión de grabación de vídeo.
     */
    fun onVideoRecordingStarted() {
        _uiState.update {
            it.copy(
                isRecordingVideo = true,
                recordingDurationSeconds = 0L
            )
        }
    }

    /**
     * Actualiza el cronómetro de la grabación de vídeo en curso.
     */
    fun onVideoDurationUpdate(seconds: Long) {
        _uiState.update { it.copy(recordingDurationSeconds = seconds) }
    }

    /**
     * Callback invocado al completar y guardar exitosamente una grabación de vídeo en MediaStore.
     */
    fun onVideoSaved(uri: Uri, path: String) {
        _uiState.update {
            it.copy(
                isRecordingVideo = false,
                recordingDurationSeconds = 0L,
                lastPhotoUri = uri,
                lastPhotoPath = path,
                userMessage = "Vídeo guardado con éxito"
            )
        }
    }

    /**
     * Callback invocado si ocurre un error durante la grabación de vídeo.
     */
    fun onVideoError(errorMessage: String) {
        _uiState.update {
            it.copy(
                isRecordingVideo = false,
                recordingDurationSeconds = 0L,
                userMessage = errorMessage
            )
        }
    }

    /**
     * Alterna el modo de máxima resolución de megapíxeles físicos del sensor.
     */
    fun toggleMaxMegapixels() {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { currentState ->
            val info = currentState.photoResolutionInfo
            val newState = !currentState.isMaxMegapixelsEnabled
            val mp = info.maxMegaPixels
            val message = if (newState) {
                if (info.isRestrictedByOemOrOs) {
                    "Sensor de ${mp}MP activo (modo binning 4en1 por capa del sistema). Mantén quieto el móvil."
                } else {
                    "Modo ${mp}MP nativo activado. Mantén el teléfono quieto al disparar."
                }
            } else {
                "Modo estándar optimizado activado."
            }
            currentState.copy(
                isMaxMegapixelsEnabled = newState,
                userMessage = message
            )
        }
    }

    /**
     * Controla la visualización del diálogo explicativo sobre 50MP, Pixel Binning y restricciones OEM.
     */
    fun setHighResInfoDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isHighResInfoDialogOpen = open) }
    }

    /**
     * Registra las capacidades de resolución física del sensor fotográfico detectadas por Camera2.
     */
    fun setPhotoCapabilities(info: PhotoResolutionInfo) {
        _uiState.update { it.copy(photoResolutionInfo = info) }
    }

    /**
     * Actualiza el estado de estabilidad del teléfono detectado por el acelerómetro.
     */
    fun setDeviceSteady(isSteady: Boolean) {
        _uiState.update { it.copy(isDeviceSteady = isSteady) }
    }

    /**
     * Alterna cíclicamente el modo del flash: Automático -> Activado -> Desactivado.
     */
    fun toggleFlashMode() {
        _uiState.update { currentState ->
            val nextFlash = when (currentState.flashMode) {
                FlashMode.AUTO -> FlashMode.ON
                FlashMode.ON -> FlashMode.OFF
                FlashMode.OFF -> FlashMode.AUTO
            }
            currentState.copy(flashMode = nextFlash)
        }
    }

    /**
     * Alterna entre la cámara trasera y la cámara frontal.
     */
    fun toggleLensFacing() {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { currentState ->
            val nextLens = when (currentState.lensFacing) {
                LensFacing.BACK -> LensFacing.FRONT
                LensFacing.FRONT -> LensFacing.BACK
            }
            currentState.copy(lensFacing = nextLens)
        }
    }

    /**
     * Activa o desactiva la cuadrícula visual de ayuda de composición.
     */
    fun toggleGrid() {
        _uiState.update { currentState ->
            currentState.copy(isGridEnabled = !currentState.isGridEnabled)
        }
    }

    /**
     * Selecciona una relación de aspecto para la cámara (Full, 16:9, 4:3, 1:1).
     * Muestra un mensaje amigable al usuario confirmando el cambio de proporción.
     */
    fun setAspectRatio(ratio: AspectRatioOption) {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { currentState ->
            currentState.copy(
                aspectRatio = ratio,
                isAspectRatioSelectorOpen = false,
                userMessage = "Relación de aspecto: ${ratio.label} (${ratio.description})"
            )
        }
    }

    /**
     * Abre o cierra la barra selectora rápida de relación de aspecto en la parte superior.
     */
    fun toggleAspectRatioSelector() {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { currentState ->
            currentState.copy(
                isAspectRatioSelectorOpen = !currentState.isAspectRatioSelectorOpen,
                isTimerSelectorOpen = false,
                isFilterSelectorOpen = false
            )
        }
    }

    /**
     * Cierra explícitamente el selector de relación de aspecto.
     */
     fun closeAspectRatioSelector() {
         _uiState.update { it.copy(isAspectRatioSelectorOpen = false) }
     }

    /**
     * Abre o cierra la barra selectora de filtros en vivo en la pantalla de la cámara.
     */
    fun toggleFilterSelector() {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { currentState ->
            currentState.copy(
                isFilterSelectorOpen = !currentState.isFilterSelectorOpen,
                isAspectRatioSelectorOpen = false,
                isTimerSelectorOpen = false
            )
        }
    }

    /**
     * Cierra explícitamente el selector de filtros.
     */
    fun closeFilterSelector() {
        _uiState.update { it.copy(isFilterSelectorOpen = false) }
    }

    /**
     * Establece la opción del temporizador de disparo (OFF, 3s, 5s, 10s).
     * Muestra un mensaje amigable al usuario confirmando el cambio.
     */
    fun setTimerOption(option: TimerOption) {
        cancelCountdown()
        _uiState.update { currentState ->
            currentState.copy(
                timerOption = option,
                isTimerSelectorOpen = false,
                userMessage = if (option == TimerOption.OFF) {
                    "Temporizador desactivado"
                } else {
                    "Temporizador fijado en ${option.description}"
                }
            )
        }
    }

    /**
     * Alterna cíclicamente el temporizador: OFF -> 3s -> 5s -> 10s -> OFF.
     */
    fun toggleTimerOption() {
        cancelCountdown()
        _uiState.update { currentState ->
            val nextOption = when (currentState.timerOption) {
                TimerOption.OFF -> TimerOption.SEC_3
                TimerOption.SEC_3 -> TimerOption.SEC_5
                TimerOption.SEC_5 -> TimerOption.SEC_10
                TimerOption.SEC_10 -> TimerOption.OFF
            }
            val message = if (nextOption == TimerOption.OFF) {
                "Temporizador desactivado"
            } else {
                "Temporizador: ${nextOption.description}"
            }
            currentState.copy(
                timerOption = nextOption,
                userMessage = message
            )
        }
    }

    /**
     * Abre o cierra la barra selectora de temporizador en la parte superior.
     */
    fun toggleTimerSelector() {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { currentState ->
            currentState.copy(
                isTimerSelectorOpen = !currentState.isTimerSelectorOpen,
                isAspectRatioSelectorOpen = false,
                isFilterSelectorOpen = false
            )
        }
    }

    /**
     * Cierra explícitamente el selector de temporizador.
     */
    fun closeTimerSelector() {
        _uiState.update { it.copy(isTimerSelectorOpen = false) }
    }

    /**
     * Inicia la cuenta regresiva del temporizador antes de disparar la fotografía.
     * Si el temporizador está desactivado (OFF), invoca [onFinished] inmediatamente.
     */
    fun startCountdown(onFinished: () -> Unit) {
        val duration = _uiState.value.timerOption.seconds
        if (duration <= 0) {
            onFinished()
            return
        }

        cancelCountdown()
        timerJob = viewModelScope.launch(Dispatchers.Main) {
            var remaining = duration
            while (remaining > 0) {
                _uiState.update { it.copy(activeTimerSecondsRemaining = remaining) }
                delay(1000L)
                remaining--
            }
            _uiState.update { it.copy(activeTimerSecondsRemaining = null) }
            onFinished()
        }
    }

    /**
     * Cancela la cuenta regresiva activa del temporizador si el usuario la interrumpe.
     */
    fun cancelCountdown() {
        timerJob?.cancel()
        timerJob = null
        if (_uiState.value.activeTimerSecondsRemaining != null) {
            _uiState.update {
                it.copy(
                    activeTimerSecondsRemaining = null,
                    userMessage = "Temporizador cancelado"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Registra los límites reales de zoom (mínimo y máximo) reportados por el hardware del sensor.
     */
    fun setZoomLimits(minRatio: Float, maxRatio: Float) {
        _uiState.update { currentState ->
            currentState.copy(
                minZoomRatio = minRatio,
                maxZoomRatio = maxRatio,
                zoomRatio = currentState.zoomRatio.coerceIn(minRatio, maxRatio)
            )
        }
    }

    /**
     * Actualiza el ratio de zoom de la cámara sin límites artificiales,
     * adaptándose al rango real soportado por el sensor del dispositivo (ej. 10x o superior).
     */
    fun setZoomRatio(ratio: Float) {
        _uiState.update { currentState ->
            val safeZoom = ratio.coerceIn(currentState.minZoomRatio, currentState.maxZoomRatio)
            currentState.copy(zoomRatio = safeZoom)
        }
    }

    /**
     * Notifica el inicio de la captura de foto y activa el destello de obturador visual.
     */
    fun onCaptureStarted() {
        _uiState.update { it.copy(isCapturing = true, showShutterFlash = true) }
        viewModelScope.launch {
            delay(120)
            _uiState.update { it.copy(showShutterFlash = false) }
        }
    }

    /**
     * Se llama cuando la foto ha sido guardada exitosamente en el almacenamiento.
     */
    fun onPhotoCaptured(uri: Uri, path: String) {
        _uiState.update {
            it.copy(
                isCapturing = false,
                lastPhotoUri = uri,
                lastPhotoPath = path,
                userMessage = "Foto guardada con éxito"
            )
        }
    }

    /**
     * Se llama si ocurre un error durante el proceso de captura o almacenamiento de fotos.
     */
    fun onCaptureError(message: String) {
        _uiState.update {
            it.copy(
                isCapturing = false,
                userMessage = message
            )
        }
    }

    /**
     * Abre el visor a pantalla completa de la foto o vídeo indicado.
     */
    fun openPhotoPreview(uri: Uri) {
        _uiState.update { it.copy(selectedPhotoForPreview = uri) }
    }

    /**
     * Cierra el visor a pantalla completa regresando al visor de la cámara.
     */
    fun closePhotoPreview() {
        _uiState.update { it.copy(selectedPhotoForPreview = null) }
    }

    /**
     * Elimina el archivo multimedia seleccionado utilizando una corrutina en [Dispatchers.IO].
     */
    fun deletePhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val rowsDeleted = context.contentResolver.delete(uri, null, null)
                    if (rowsDeleted > 0) {
                        true
                    } else {
                        uri.path?.let { path ->
                            val file = File(path)
                            if (file.exists()) file.delete() else false
                        } ?: false
                    }
                } catch (e: Exception) {
                    false
                }
            }

            _uiState.update { currentState ->
                val newLastUri = if (currentState.lastPhotoUri == uri) null else currentState.lastPhotoUri
                val newLastPath = if (currentState.lastPhotoUri == uri) null else currentState.lastPhotoPath
                currentState.copy(
                    selectedPhotoForPreview = null,
                    lastPhotoUri = newLastUri,
                    lastPhotoPath = newLastPath,
                    userMessage = if (success) "Elemento eliminado" else "No se pudo eliminar el elemento"
                )
            }
        }
    }

    /**
     * Limpia el mensaje informativo tras ser mostrado.
     */
    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    /**
     * Abre o cierra la pantalla independiente de configuración general.
     */
    fun openSettings(isOpen: Boolean) {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { it.copy(isSettingsOpen = isOpen, isColorCalibrationOpen = false) }
    }

    /**
     * Abre o cierra la pantalla independiente de calibración de color y contraste.
     */
    fun openColorCalibration(isOpen: Boolean) {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { it.copy(isColorCalibrationOpen = isOpen) }
    }

    /**
     * Alterna el modo Antilavado Automático.
     * Al activarse, aplica -0.7 EV de compensación (o el equivalente soportado)
     * y el perfil de color Vívido para eliminar de golpe la neblina lechosa.
     */
    fun toggleAntiWashedMode() {
        _uiState.update { currentState ->
            val newState = !currentState.isAntiWashedModeEnabled
            val newEv = if (newState) -0.7f else 0.0f
            val newProfile = if (newState) ColorProfileOption.VIVID_ANTI_WASHED else ColorProfileOption.STANDARD
            val step = if (currentState.exposureStep > 0f) currentState.exposureStep else 0.33333334f
            val calculatedIndex = kotlin.math.round(newEv / step).toInt()
                .coerceIn(currentState.minExposureIndex, currentState.maxExposureIndex)

            val msg = if (newState) {
                "Modo Antilavado activado (-0.7 EV • Vívido)"
            } else {
                "Modo estándar del sensor activado (0.0 EV)"
            }

            currentState.copy(
                isAntiWashedModeEnabled = newState,
                colorProfile = newProfile,
                exposureCompensationEv = newEv,
                exposureCompensationIndex = calculatedIndex,
                userMessage = msg
            )
        }
    }

    /**
     * Selecciona un perfil de color (Vívido Antilavado, Alto Contraste, Cálido Natural o Estándar).
     */
    fun setColorProfile(profile: ColorProfileOption) {
        _uiState.update { currentState ->
            val isAntiWashed = profile != ColorProfileOption.STANDARD
            currentState.copy(
                colorProfile = profile,
                isAntiWashedModeEnabled = isAntiWashed,
                userMessage = "Perfil aplicado: ${profile.label}"
            )
        }
    }

    /**
     * Ajusta manualmente la compensación de exposición en unidades EV (ej. -0.7 EV, -0.3 EV, 0.0 EV).
     */
    fun setExposureEv(ev: Float) {
        _uiState.update { currentState ->
            val step = if (currentState.exposureStep > 0f) currentState.exposureStep else 0.33333334f
            val targetIndex = kotlin.math.round(ev / step).toInt()
                .coerceIn(currentState.minExposureIndex, currentState.maxExposureIndex)
            val actualEv = targetIndex * step

            currentState.copy(
                exposureCompensationEv = actualEv,
                exposureCompensationIndex = targetIndex,
                isAntiWashedModeEnabled = actualEv < -0.1f || currentState.colorProfile != ColorProfileOption.STANDARD
            )
        }
    }

    /**
     * Registra los límites y el paso de compensación de exposición reportados por el hardware de la cámara activa.
     */
    fun setExposureLimits(min: Int, max: Int, step: Float, isSupported: Boolean) {
        _uiState.update { currentState ->
            val validStep = if (step > 0f) step else 0.33333334f
            val clampedIndex = currentState.exposureCompensationIndex.coerceIn(min, max)
            val computedEv = clampedIndex * validStep

            currentState.copy(
                minExposureIndex = min,
                maxExposureIndex = max,
                exposureStep = validStep,
                isExposureCompensationSupported = isSupported,
                exposureCompensationIndex = clampedIndex,
                exposureCompensationEv = computedEv
            )
        }
    }

    /**
     * Restablece la calibración a los valores óptimos recomendados contra colores lavados.
     */
    fun resetColorCalibrationToDefaults() {
        _uiState.update { currentState ->
            val step = if (currentState.exposureStep > 0f) currentState.exposureStep else 0.33333334f
            val recIndex = kotlin.math.round(-0.7f / step).toInt()
                .coerceIn(currentState.minExposureIndex, currentState.maxExposureIndex)

            currentState.copy(
                isAntiWashedModeEnabled = true,
                colorProfile = ColorProfileOption.VIVID_ANTI_WASHED,
                exposureCompensationEv = recIndex * step,
                exposureCompensationIndex = recIndex,
                userMessage = "Restablecido a calibración antilavado recomendada"
            )
        }
    }

    /**
     * Inspecciona si el hardware (procesador y GPU) soporta Vulkan 1.1 y detecta la versión de OpenGL ES.
     * Selecciona automáticamente Vulkan 1.1 como backend de filtros si está disponible; de lo contrario OpenGL ES.
     */
    fun detectGraphicsHardware(context: Context) {
        val (isVulkan11, vulkanStr) = CameraCaptureManager.detectVulkan11Support(context)
        val openGlStr = CameraCaptureManager.detectOpenGlVersion(context)

        _uiState.update { current ->
            current.copy(
                isVulkan11Supported = isVulkan11,
                vulkanVersionString = vulkanStr,
                openGlVersionString = openGlStr,
                selectedGraphicsBackend = if (isVulkan11) GraphicsFilterBackend.VULKAN else GraphicsFilterBackend.OPENGL_ES
            )
        }
    }

    /**
     * Permite al usuario cambiar manualmente el backend de renderizado gráfico de los filtros.
     */
    fun setSelectedGraphicsBackend(backend: GraphicsFilterBackend) {
        _uiState.update { current ->
            if (backend == GraphicsFilterBackend.VULKAN && !current.isVulkan11Supported) {
                current.copy(userMessage = "El procesador no cuenta con Vulkan 1.1. Se mantendrá OpenGL ES.")
            } else {
                current.copy(
                    selectedGraphicsBackend = backend,
                    userMessage = "Motor de filtros: ${backend.label} (${backend.apiName})"
                )
            }
        }
    }

    /**
     * Alterna la activación del filtro de belleza y suavizado con preservación de contraste.
     */
    fun toggleBeautyFilter() {
        if (_uiState.value.isRecordingVideo) return
        _uiState.update { current ->
            val newState = !current.isBeautyFilterEnabled
            val backendName = current.selectedGraphicsBackend.label
            val msg = if (newState) {
                "Filtro de Belleza activado ($backendName • Antilavado)"
            } else {
                "Filtro de Belleza desactivado"
            }
            current.copy(
                isBeautyFilterEnabled = newState,
                userMessage = msg
            )
        }
    }

    /**
     * Ajusta la intensidad del filtro de belleza (0.0f a 1.0f).
     */
    fun setBeautyFilterIntensity(intensity: Float) {
        _uiState.update { it.copy(beautyFilterIntensity = intensity.coerceIn(0.0f, 1.0f)) }
    }

    /**
     * Alterna la activación de la marca de agua con el nombre de la app (Apex Camera).
     */
    fun toggleWatermark() {
        _uiState.update { current ->
            val newState = !current.isWatermarkEnabled
            val msg = if (newState) {
                "Marca de agua 'Apex Camera' activada"
            } else {
                "Marca de agua desactivada"
            }
            current.copy(
                isWatermarkEnabled = newState,
                userMessage = msg
            )
        }
    }

    /**
     * Establece explícitamente el estado de la marca de agua.
     */
    fun setWatermarkEnabled(enabled: Boolean) {
        _uiState.update { current ->
            current.copy(
                isWatermarkEnabled = enabled,
                userMessage = if (enabled) "Marca de agua 'Apex Camera' activada" else "Marca de agua desactivada"
            )
        }
    }

    /**
     * Estampa la marca de agua en una foto existente desde el panel de confirmación / visualización.
     */
    fun applyWatermarkToCurrentPhoto(context: Context, photoUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = CameraCaptureManager.applyWatermarkToExistingPhoto(
                    context = context,
                    photoUri = photoUri,
                    aspectRatio = _uiState.value.aspectRatio,
                    colorProfile = _uiState.value.colorProfile
                )
                withContext(Dispatchers.Main) {
                    if (success) {
                        _uiState.update { current ->
                            current.copy(
                                userMessage = "Marca de agua Apex Camera estampada en la foto"
                            )
                        }
                    } else {
                        _uiState.update { current ->
                            current.copy(
                                userMessage = "No se pudo estampar la marca de agua"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { current ->
                        current.copy(userMessage = "Error al procesar marca de agua: ${e.message}")
                    }
                }
            }
        }
    }
}
