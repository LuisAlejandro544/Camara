package com.example.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pruebas unitarias en la JVM local para la gestión de estado de Cámara y modo HDR de vídeo.
 * Verifica los flujos felices y los casos de borde (ej. hardware sin soporte HDR) usando Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CameraViewModelTest {

    private lateinit var viewModel: CameraViewModel

    @Before
    fun setUp() {
        viewModel = CameraViewModel()
    }

    @Test
    fun `toggleHdrVideo no debe activarse si el hardware no soporta HDR`() {
        // Dado un estado donde el sensor no soporta HDR
        viewModel.setVideoCapabilities(
            VideoCapabilitiesInfo(
                supportedQualities = listOf(VideoQualityOption.FHD, VideoQualityOption.HD),
                supportedFps = listOf(30),
                isHdrSupported = false
            )
        )

        // Al intentar alternar HDR
        viewModel.toggleHdrVideo()

        // Entonces el estado debe permanecer apagado
        assertFalse(viewModel.uiState.value.isHdrVideoEnabled)
        assertFalse(viewModel.uiState.value.isHdrSupported)
    }

    @Test
    fun `toggleHdrVideo debe activarse y desactivarse si el hardware soporta HDR`() {
        // Dado un sensor de gama alta con soporte de HDR de 10 bits
        viewModel.setVideoCapabilities(
            VideoCapabilitiesInfo(
                supportedQualities = listOf(VideoQualityOption.UHD, VideoQualityOption.FHD),
                supportedFps = listOf(30, 60),
                isHdrSupported = true
            )
        )

        assertTrue(viewModel.uiState.value.isHdrSupported)
        assertFalse(viewModel.uiState.value.isHdrVideoEnabled)

        // Al activarlo
        viewModel.toggleHdrVideo()
        assertTrue(viewModel.uiState.value.isHdrVideoEnabled)
        assertEquals("Vídeo HDR de 10 bits activado (HLG)", viewModel.uiState.value.userMessage)

        // Al desactivarlo
        viewModel.toggleHdrVideo()
        assertFalse(viewModel.uiState.value.isHdrVideoEnabled)
        assertEquals("Vídeo estándar (SDR) activado", viewModel.uiState.value.userMessage)
    }

    @Test
    fun `cambiar a un sensor sin soporte HDR debe apagar automaticamente el modo HDR`() {
        // Primero cámara trasera con soporte HDR y activado
        viewModel.setVideoCapabilities(
            VideoCapabilitiesInfo(
                supportedQualities = listOf(VideoQualityOption.UHD),
                supportedFps = listOf(30),
                isHdrSupported = true
            )
        )
        viewModel.toggleHdrVideo()
        assertTrue(viewModel.uiState.value.isHdrVideoEnabled)

        // Cambiamos a cámara frontal que no soporta HDR
        viewModel.setVideoCapabilities(
            VideoCapabilitiesInfo(
                supportedQualities = listOf(VideoQualityOption.FHD),
                supportedFps = listOf(30),
                isHdrSupported = false
            )
        )

        // Se apaga de forma segura para evitar fallos de hardware
        assertFalse(viewModel.uiState.value.isHdrSupported)
        assertFalse(viewModel.uiState.value.isHdrVideoEnabled)
    }

    @Test
    fun `toggleAntiWashedMode alterna entre modo antilavado y modo estandar del sensor`() {
        // Inicialmente el modo antilavado está activado por defecto para proteger contra colores pasteles
        assertTrue(viewModel.uiState.value.isAntiWashedModeEnabled)
        assertEquals(ColorProfileOption.VIVID_ANTI_WASHED, viewModel.uiState.value.colorProfile)

        // Al alternarlo, debe cambiar a estándar y 0.0 EV
        viewModel.toggleAntiWashedMode()
        assertFalse(viewModel.uiState.value.isAntiWashedModeEnabled)
        assertEquals(ColorProfileOption.STANDARD, viewModel.uiState.value.colorProfile)
        assertEquals(0.0f, viewModel.uiState.value.exposureCompensationEv, 0.05f)
        assertEquals(0, viewModel.uiState.value.exposureCompensationIndex)

        // Al volver a activarlo, debe regresar a -0.7 EV y Vívido
        viewModel.toggleAntiWashedMode()
        assertTrue(viewModel.uiState.value.isAntiWashedModeEnabled)
        assertEquals(ColorProfileOption.VIVID_ANTI_WASHED, viewModel.uiState.value.colorProfile)
        assertEquals(-0.7f, viewModel.uiState.value.exposureCompensationEv, 0.05f)
    }

    @Test
    fun `setColorProfile y setExposureEv actualizan el estado de forma coherente`() {
        viewModel.setExposureLimits(min = -6, max = 6, step = 0.33333334f, isSupported = true)

        viewModel.setColorProfile(ColorProfileOption.DEEP_CONTRAST)
        assertEquals(ColorProfileOption.DEEP_CONTRAST, viewModel.uiState.value.colorProfile)
        assertTrue(viewModel.uiState.value.isAntiWashedModeEnabled)

        viewModel.setExposureEv(-1.0f)
        assertEquals(-3, viewModel.uiState.value.exposureCompensationIndex)
        assertEquals(-1.0f, viewModel.uiState.value.exposureCompensationEv, 0.05f)
    }

    @Test
    fun `navegacion a Settings y ColorCalibration actualiza los flags de pantalla`() {
        assertFalse(viewModel.uiState.value.isSettingsOpen)
        assertFalse(viewModel.uiState.value.isColorCalibrationOpen)

        viewModel.openSettings(true)
        assertTrue(viewModel.uiState.value.isSettingsOpen)
        assertFalse(viewModel.uiState.value.isColorCalibrationOpen)

        viewModel.openColorCalibration(true)
        assertTrue(viewModel.uiState.value.isColorCalibrationOpen)

        viewModel.openColorCalibration(false)
        assertFalse(viewModel.uiState.value.isColorCalibrationOpen)

        viewModel.openSettings(false)
        assertFalse(viewModel.uiState.value.isSettingsOpen)
    }

    @Test
    fun `toggleMaxMegapixels y setHighResInfoDialogOpen funcionan correctamente con sensor de 50MP`() {
        // Dado un sensor de 50MP con restricción detectada de capa de fabricante
        val photoInfo = PhotoResolutionInfo(
            maxMegaPixels = 50,
            maxResolutionString = "8160 × 6120",
            standardMegaPixels = 12,
            hasHighResMode = true,
            physicalSensorMegaPixels = 50,
            isRestrictedByOemOrOs = true,
            restrictionReason = "Limitado por capa OEM a 12MP"
        )
        viewModel.setPhotoCapabilities(photoInfo)
        assertFalse(viewModel.uiState.value.isMaxMegapixelsEnabled)
        assertFalse(viewModel.uiState.value.isHighResInfoDialogOpen)

        // Al alternar el modo de megapíxeles
        viewModel.toggleMaxMegapixels()
        assertTrue(viewModel.uiState.value.isMaxMegapixelsEnabled)
        assertTrue(viewModel.uiState.value.userMessage?.contains("50MP") == true)

        // Al abrir el diálogo explicativo
        viewModel.setHighResInfoDialogOpen(true)
        assertTrue(viewModel.uiState.value.isHighResInfoDialogOpen)

        viewModel.setHighResInfoDialogOpen(false)
        assertFalse(viewModel.uiState.value.isHighResInfoDialogOpen)
    }

    @Test
    fun `setSelectedGraphicsBackend respeta compatibilidad de Vulkan 1_1`() {
        // Por defecto sin Vulkan 1.1, debe permanecer o mantenerse en OpenGL ES
        viewModel.setSelectedGraphicsBackend(GraphicsFilterBackend.OPENGL_ES)
        assertEquals(GraphicsFilterBackend.OPENGL_ES, viewModel.uiState.value.selectedGraphicsBackend)

        // Intentar seleccionar Vulkan sin soporte
        viewModel.setSelectedGraphicsBackend(GraphicsFilterBackend.VULKAN)
        // Como isVulkan11Supported es false por defecto en el test, debe advertir y mantenerse en OpenGL ES
        assertEquals(GraphicsFilterBackend.OPENGL_ES, viewModel.uiState.value.selectedGraphicsBackend)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Vulkan 1.1") == true)
    }

    @Test
    fun `toggleBeautyFilter y setBeautyFilterIntensity controlan el filtro de belleza`() {
        assertFalse(viewModel.uiState.value.isBeautyFilterEnabled)

        // Al activarlo
        viewModel.toggleBeautyFilter()
        assertTrue(viewModel.uiState.value.isBeautyFilterEnabled)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Filtro de Belleza activado") == true)

        // Ajustar intensidad
        viewModel.setBeautyFilterIntensity(0.85f)
        assertEquals(0.85f, viewModel.uiState.value.beautyFilterIntensity, 0.01f)

        // Al desactivarlo
        viewModel.toggleBeautyFilter()
        assertFalse(viewModel.uiState.value.isBeautyFilterEnabled)
        assertEquals("Filtro de Belleza desactivado", viewModel.uiState.value.userMessage)
    }

    @Test
    fun `setAspectRatio actualiza correctamente la relacion de aspecto y emite mensaje`() {
        // Inicialmente el ratio es 4:3 (sensor completo)
        assertEquals(AspectRatioOption.RATIO_4_3, viewModel.uiState.value.aspectRatio)

        // Cambiar a 16:9
        viewModel.setAspectRatio(AspectRatioOption.RATIO_16_9)
        assertEquals(AspectRatioOption.RATIO_16_9, viewModel.uiState.value.aspectRatio)
        assertTrue(viewModel.uiState.value.userMessage?.contains("16:9") == true)

        // Cambiar a 1:1
        viewModel.setAspectRatio(AspectRatioOption.RATIO_1_1)
        assertEquals(AspectRatioOption.RATIO_1_1, viewModel.uiState.value.aspectRatio)
        assertTrue(viewModel.uiState.value.userMessage?.contains("1:1") == true)

        // Cambiar a Full
        viewModel.setAspectRatio(AspectRatioOption.FULL)
        assertEquals(AspectRatioOption.FULL, viewModel.uiState.value.aspectRatio)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Full") == true)
    }

    @Test
    fun `toggleAspectRatioSelector y closeAspectRatioSelector controlan la apertura de la barra`() {
        assertFalse(viewModel.uiState.value.isAspectRatioSelectorOpen)

        viewModel.toggleAspectRatioSelector()
        assertTrue(viewModel.uiState.value.isAspectRatioSelectorOpen)

        viewModel.toggleAspectRatioSelector()
        assertFalse(viewModel.uiState.value.isAspectRatioSelectorOpen)

        viewModel.toggleAspectRatioSelector()
        assertTrue(viewModel.uiState.value.isAspectRatioSelectorOpen)
        viewModel.closeAspectRatioSelector()
        assertFalse(viewModel.uiState.value.isAspectRatioSelectorOpen)
    }

    @Test
    fun `toggleTimerOption alterna ciclicamente entre OFF, 3s, 5s y 10s`() {
        assertEquals(TimerOption.OFF, viewModel.uiState.value.timerOption)

        viewModel.toggleTimerOption()
        assertEquals(TimerOption.SEC_3, viewModel.uiState.value.timerOption)

        viewModel.toggleTimerOption()
        assertEquals(TimerOption.SEC_5, viewModel.uiState.value.timerOption)

        viewModel.toggleTimerOption()
        assertEquals(TimerOption.SEC_10, viewModel.uiState.value.timerOption)

        viewModel.toggleTimerOption()
        assertEquals(TimerOption.OFF, viewModel.uiState.value.timerOption)
    }

    @Test
    fun `setTimerOption actualiza el temporizador y toggleTimerSelector abre y cierra la barra`() {
        viewModel.setTimerOption(TimerOption.SEC_5)
        assertEquals(TimerOption.SEC_5, viewModel.uiState.value.timerOption)
        assertFalse(viewModel.uiState.value.isTimerSelectorOpen)

        viewModel.toggleTimerSelector()
        assertTrue(viewModel.uiState.value.isTimerSelectorOpen)

        viewModel.closeTimerSelector()
        assertFalse(viewModel.uiState.value.isTimerSelectorOpen)
    }

    @Test
    fun `cambiar a modo video cancela la cuenta regresiva del temporizador`() {
        viewModel.setTimerOption(TimerOption.SEC_10)
        viewModel.toggleTimerSelector()
        assertTrue(viewModel.uiState.value.isTimerSelectorOpen)

        viewModel.setCaptureMode(CaptureMode.VIDEO)
        assertEquals(CaptureMode.VIDEO, viewModel.uiState.value.captureMode)
        assertFalse(viewModel.uiState.value.isTimerSelectorOpen)
        assertEquals(null, viewModel.uiState.value.activeTimerSecondsRemaining)
    }
}
