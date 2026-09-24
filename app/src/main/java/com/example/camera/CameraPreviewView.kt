package com.example.camera

import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.CameraFocusRing
import com.example.ui.theme.CameraGridLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Componente que renderiza el visor de la cámara en vivo utilizando CameraX y PreviewView.
 * Soporta alternancia fluida entre Modo Foto e Imagen y Modo Vídeo con VideoCapture,
 * inspección de capacidades de hardware (resolución y FPS), enfoque táctil animado,
 * zoom táctil y cuadrícula de regla de tercios.
 *
 * @param lensFacing Orientación actual de la cámara (trasera o frontal).
 * @param captureMode Modo de captura activo (FOTO o VÍDEO).
 * @param selectedQuality Calidad de vídeo seleccionada por el usuario.
 * @param isGridEnabled Indica si se debe dibujar la cuadrícula de regla de tercios.
 * @param onImageCaptureReady Callback que expone la instancia activa de [ImageCapture].
 * @param onVideoCaptureReady Callback que expone la instancia activa de [VideoCapture].
 * @param onCapabilitiesDetected Callback con las capacidades de vídeo detectadas físicamente en el sensor.
 * @param onPhotoCapabilitiesDetected Callback con los megapíxeles y dimensiones físicas del sensor.
 * @param zoomRatio Nivel actual de zoom establecido por el usuario o interfaz.
 * @param onZoomLimitsDetected Notifica el rango mínimo y máximo de zoom real del hardware de la cámara activa.
 * @param onZoomChanged Notifica cambios de zoom cuando el usuario pellizca la pantalla.
 */
@Composable
fun CameraPreviewView(
    lensFacing: LensFacing,
    captureMode: CaptureMode,
    selectedQuality: VideoQualityOption,
    isMaxMegapixelsEnabled: Boolean,
    isHdrVideoEnabled: Boolean,
    isGridEnabled: Boolean,
    zoomRatio: Float,
    targetExposureIndex: Int = 0,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onVideoCaptureReady: (VideoCapture<Recorder>) -> Unit,
    onCapabilitiesDetected: (VideoCapabilitiesInfo) -> Unit,
    onPhotoCapabilitiesDetected: (PhotoResolutionInfo) -> Unit,
    onZoomLimitsDetected: (min: Float, max: Float) -> Unit,
    onZoomChanged: (Float) -> Unit,
    onExposureLimitsDetected: ((min: Int, max: Int, step: Float, isSupported: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Estado del indicador visual de enfoque táctil
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusScale = remember { Animatable(1.4f) }
    val focusAlpha = remember { Animatable(1.0f) }

    // Zoom interactivo
    var currentZoomRatio by remember { mutableFloatStateOf(1.0f) }

    // Reacciona al cambio de orientación de cámara, modo de captura, resolución, HDR o ciclo de vida
    LaunchedEffect(lensFacing, captureMode, selectedQuality, isMaxMegapixelsEnabled, isHdrVideoEnabled, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector = if (lensFacing == LensFacing.FRONT) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            val preview = Preview.Builder().build()
            previewViewRef?.let { previewView ->
                preview.surfaceProvider = previewView.surfaceProvider
            }

            try {
                cameraProvider.unbindAll()

                if (captureMode == CaptureMode.PHOTO) {
                    val imageCaptureBuilder = ImageCapture.Builder()

                    if (isMaxMegapixelsEnabled) {
                        // Modo Máxima Resolución Nativa: Máxima calidad de ISP y máxima resolución disponible
                        val resolutionSelector = ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                            .build()
                        imageCaptureBuilder
                            .setResolutionSelector(resolutionSelector)
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    } else {
                        // Modo Estándar optimizado y rápido
                        imageCaptureBuilder
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    }

                    val imageCapture = imageCaptureBuilder.build()

                    val boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    cameraInstance = boundCamera
                    onImageCaptureReady(imageCapture)

                    // Notifica los límites de zoom reales del hardware sin límites artificiales
                    boundCamera.cameraInfo.zoomState.value?.let { state ->
                        onZoomLimitsDetected(state.minZoomRatio, state.maxZoomRatio)
                    }
                    boundCamera.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
                        if (state != null) {
                            onZoomLimitsDetected(state.minZoomRatio, state.maxZoomRatio)
                        }
                    }

                    // Detecta capacidades fotográficas reales del sensor físico
                    val photoCaps = CameraCaptureManager.detectPhotoCapabilities(boundCamera.cameraInfo)
                    onPhotoCapabilitiesDetected(photoCaps)

                    // Detecta capacidades de vídeo del sensor para tenerlas listas al cambiar de modo
                    val capabilities = CameraVideoManager.detectVideoCapabilities(boundCamera.cameraInfo)
                    onCapabilitiesDetected(capabilities)
                } else {
                    // Modo Vídeo: Configura el Recorder con la calidad seleccionada y rango dinámico (SDR u HDR 10-bit HLG)
                    val targetDynamicRange = if (isHdrVideoEnabled) {
                        DynamicRange.HLG_10_BIT
                    } else {
                        DynamicRange.SDR
                    }

                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(selectedQuality.quality))
                        .build()
                    val videoCapture = VideoCapture.Builder(recorder)
                        .setDynamicRange(targetDynamicRange)
                        .build()

                    val boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        videoCapture
                    )
                    cameraInstance = boundCamera
                    onVideoCaptureReady(videoCapture)

                    // Notifica los límites de zoom reales del hardware sin límites artificiales
                    boundCamera.cameraInfo.zoomState.value?.let { state ->
                        onZoomLimitsDetected(state.minZoomRatio, state.maxZoomRatio)
                    }
                    boundCamera.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
                        if (state != null) {
                            onZoomLimitsDetected(state.minZoomRatio, state.maxZoomRatio)
                        }
                    }

                    val capabilities = CameraVideoManager.detectVideoCapabilities(boundCamera.cameraInfo)
                    onCapabilitiesDetected(capabilities)
                }

                // Detecta soporte de compensación de exposición (EV) y límites del sensor
                cameraInstance?.let { boundCamera ->
                    val expState = boundCamera.cameraInfo.exposureState
                    onExposureLimitsDetected?.invoke(
                        expState.exposureCompensationRange.lower,
                        expState.exposureCompensationRange.upper,
                        expState.exposureCompensationStep.toFloat(),
                        expState.isExposureCompensationSupported
                    )

                    if (expState.isExposureCompensationSupported) {
                        val targetIndex = targetExposureIndex.coerceIn(
                            expState.exposureCompensationRange.lower,
                            expState.exposureCompensationRange.upper
                        )
                        boundCamera.cameraControl.setExposureCompensationIndex(targetIndex)
                    }
                }
            } catch (e: Exception) {
                // Manejo seguro si la combinación de casos de uso o cámara no está disponible
            }
        }, executor)
    }

    // Sincroniza la compensación de exposición EV cuando el usuario la modifica desde Ajustes o Calibración
    LaunchedEffect(targetExposureIndex, cameraInstance) {
        cameraInstance?.let { cam ->
            val expState = cam.cameraInfo.exposureState
            if (expState.isExposureCompensationSupported) {
                val clamped = targetExposureIndex.coerceIn(
                    expState.exposureCompensationRange.lower,
                    expState.exposureCompensationRange.upper
                )
                cam.cameraControl.setExposureCompensationIndex(clamped)
            }
        }
    }

    // Sincroniza el nivel de zoom solicitado externamente (ej. botones rápidos 1x, 2x, 5x, 10x)
    LaunchedEffect(zoomRatio) {
        cameraInstance?.let { cam ->
            val zoomState = cam.cameraInfo.zoomState.value
            val minRatio = zoomState?.minZoomRatio ?: 1f
            val maxRatio = zoomState?.maxZoomRatio ?: 10f
            val safeZoom = zoomRatio.coerceIn(minRatio, maxRatio)
            if (kotlin.math.abs(safeZoom - currentZoomRatio) > 0.02f) {
                currentZoomRatio = safeZoom
                cam.cameraControl.setZoomRatio(safeZoom)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Gesto de pellizco para zoom táctil continuo hasta el máximo del sensor (sin topes artificiales)
                detectTransformGestures { _, _, zoom, _ ->
                    cameraInstance?.let { cam ->
                        val zoomState = cam.cameraInfo.zoomState.value
                        val minRatio = zoomState?.minZoomRatio ?: 1f
                        val maxRatio = zoomState?.maxZoomRatio ?: 10f
                        val newRatio = (currentZoomRatio * zoom).coerceIn(minRatio, maxRatio)
                        currentZoomRatio = newRatio
                        cam.cameraControl.setZoomRatio(newRatio)
                        onZoomChanged(newRatio)
                    }
                }
            }
            .pointerInput(Unit) {
                // Toque en pantalla para enfocar (Tap to Focus)
                detectTapGestures { offset ->
                    val previewView = previewViewRef ?: return@detectTapGestures
                    val meteringPointFactory = previewView.meteringPointFactory
                    val meteringPoint = meteringPointFactory.createPoint(offset.x, offset.y)
                    val action = FocusMeteringAction.Builder(meteringPoint).build()

                    cameraInstance?.cameraControl?.startFocusAndMetering(action)

                    // Inicia la animación del anillo de enfoque
                    focusPoint = offset
                }
            }
    ) {
        // Vista nativa de Android PreviewView de CameraX
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Cuadrícula de tercios para composición fotográfica
        if (isGridEnabled) {
            CameraGridOverlay(modifier = Modifier.fillMaxSize())
        }

        // Anillo visual de enfoque automático cuando se pulsa la pantalla
        focusPoint?.let { pos ->
            LaunchedEffect(pos) {
                focusScale.snapTo(1.4f)
                focusAlpha.snapTo(1.0f)
                launch {
                    focusScale.animateTo(1.0f, tween(200))
                }
                delay(1200)
                focusAlpha.animateTo(0.0f, tween(300))
                focusPoint = null
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pos.x - 36.dp.toPx()).roundToInt(),
                            (pos.y - 36.dp.toPx()).roundToInt()
                        )
                    }
                    .size(72.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = CameraFocusRing.copy(alpha = focusAlpha.value),
                        radius = (size.minDimension / 2) * focusScale.value,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Cuadrícula de 3x3 estilo regla de tercios para guía visual al encuadrar fotos y vídeos.
 */
@Composable
private fun CameraGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = 1.dp.toPx()

        val x1 = width / 3f
        val x2 = width * 2f / 3f
        val y1 = height / 3f
        val y2 = height * 2f / 3f

        // Líneas verticales
        drawLine(CameraGridLine, Offset(x1, 0f), Offset(x1, height), strokeWidth)
        drawLine(CameraGridLine, Offset(x2, 0f), Offset(x2, height), strokeWidth)

        // Líneas horizontales
        drawLine(CameraGridLine, Offset(0f, y1), Offset(width, y1), strokeWidth)
        drawLine(CameraGridLine, Offset(0f, y2), Offset(width, y2), strokeWidth)
    }
}
