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
}
