package com.example.camera

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.view.ViewGroup
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.CameraBlack
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
    aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3,
    colorProfile: ColorProfileOption = ColorProfileOption.STANDARD,
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

    // Reacciona al cambio de orientación de cámara, modo de captura, resolución, HDR, aspect ratio o ciclo de vida
    LaunchedEffect(lensFacing, captureMode, selectedQuality, aspectRatio, isMaxMegapixelsEnabled, isHdrVideoEnabled, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector = if (lensFacing == LensFacing.FRONT) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            // Seleccionamos la estrategia de aspecto para CameraX: 16:9 o 4:3 (1:1 y Full parten de su base óptima)
            val cameraXAspectRatio = when (aspectRatio) {
                AspectRatioOption.RATIO_16_9, AspectRatioOption.FULL -> AspectRatio.RATIO_16_9
                AspectRatioOption.RATIO_4_3, AspectRatioOption.RATIO_1_1 -> AspectRatio.RATIO_4_3
            }

            val previewResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                    AspectRatioStrategy(cameraXAspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                )
                .build()

            val preview = Preview.Builder()
                .setResolutionSelector(previewResolutionSelector)
                .build()

            previewViewRef?.let { previewView ->
                preview.surfaceProvider = previewView.surfaceProvider
            }

            try {
                cameraProvider.unbindAll()

                if (captureMode == CaptureMode.PHOTO) {
                    val imageCaptureBuilder = ImageCapture.Builder()

                    if (isMaxMegapixelsEnabled) {
                        // Modo Máxima Resolución Nativa: Máxima calidad de ISP y máxima resolución disponible
                        val resolutionSelectorBuilder = ResolutionSelector.Builder()
                            .setAspectRatioStrategy(
                                AspectRatioStrategy(cameraXAspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                            )
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        try {
                            resolutionSelectorBuilder.setAllowedResolutionMode(
                                ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
                            )
                        } catch (_: Throwable) {
                        }
                        imageCaptureBuilder
                            .setResolutionSelector(resolutionSelectorBuilder.build())
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    } else {
                        // Modo Estándar optimizado con selección de Aspect Ratio
                        val resolutionSelector = ResolutionSelector.Builder()
                            .setAspectRatioStrategy(
                                AspectRatioStrategy(cameraXAspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                            )
                            .build()
                        imageCaptureBuilder
                            .setResolutionSelector(resolutionSelector)
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
                    // Modo Vídeo: Configura el Recorder con la calidad seleccionada y rango dinámico adaptativo
                    val targetDynamicRange = if (isHdrVideoEnabled) {
                        try {
                            val queried = cameraProvider.availableCameraInfos.find { 
                                (if (lensFacing == LensFacing.FRONT) it.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_FRONT else it.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK)
                            }?.querySupportedDynamicRanges(setOf(DynamicRange.HLG_10_BIT, DynamicRange.HDR10_10_BIT))
                            if (!queried.isNullOrEmpty()) queried.first() else DynamicRange.SDR
                        } catch (_: Exception) {
                            DynamicRange.SDR
                        }
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

                    // Si HDR está activo y no se usa 10-bit en CameraX, activar modo de escena HDR de Camera2
                    if (isHdrVideoEnabled) {
                        try {
                            val c2Control = Camera2CameraControl.from(boundCamera.cameraControl)
                            val options = CaptureRequestOptions.Builder()
                                .setCaptureRequestOption(
                                    CaptureRequest.CONTROL_MODE,
                                    CameraMetadata.CONTROL_MODE_USE_SCENE_MODE
                                )
                                .setCaptureRequestOption(
                                    CaptureRequest.CONTROL_SCENE_MODE,
                                    CameraMetadata.CONTROL_SCENE_MODE_HDR
                                )
                                .build()
                            c2Control.setCaptureRequestOptions(options)
                        } catch (_: Exception) {
                        }
                    }

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

        // Capa de previsualización en vivo del perfil de color seleccionado
        if (colorProfile != ColorProfileOption.STANDARD) {
            LiveColorProfileOverlay(colorProfile = colorProfile)
        }

        // Máscara de recorte visual para relación de aspecto 1:1 (cuadrado)
        if (aspectRatio == AspectRatioOption.RATIO_1_1) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight
                val squareSize = if (boxWidth < boxHeight) boxWidth else boxHeight
                val verticalMargin = (boxHeight - squareSize) / 2
                val horizontalMargin = (boxWidth - squareSize) / 2

                if (verticalMargin > 0.dp) {
                    // Banda superior e inferior oscura para encuadre 1:1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(verticalMargin)
                            .align(Alignment.TopCenter)
                            .background(CameraBlack.copy(alpha = 0.88f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(verticalMargin)
                            .align(Alignment.BottomCenter)
                            .background(CameraBlack.copy(alpha = 0.88f))
                    )
                } else if (horizontalMargin > 0.dp) {
                    // Banda lateral izquierda y derecha
                    Box(
                        modifier = Modifier
                            .width(horizontalMargin)
                            .fillMaxSize()
                            .align(Alignment.CenterStart)
                            .background(CameraBlack.copy(alpha = 0.88f))
                    )
                    Box(
                        modifier = Modifier
                            .width(horizontalMargin)
                            .fillMaxSize()
                            .align(Alignment.CenterEnd)
                            .background(CameraBlack.copy(alpha = 0.88f))
                    )
                }
            }
        }

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

/**
 * Capa de visualización en tiempo real de los perfiles de color en el visor de la cámara.
 * Permite al usuario ver en directo los colores vívidos antilavado, alto contraste,
 * tonos cálidos o blanco y negro artístico antes de capturar la foto.
 */
@Composable
private fun LiveColorProfileOverlay(
    colorProfile: ColorProfileOption,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        when (colorProfile) {
            ColorProfileOption.STANDARD -> {}
            ColorProfileOption.VIVID_ANTI_WASHED -> {
                // Realce de saturación y contraste visual en vivo
                drawRect(
                    color = Color(0xFFFF9800).copy(alpha = 0.08f),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Overlay
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.06f),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Darken
                )
            }
            ColorProfileOption.DEEP_CONTRAST -> {
                // Contraste profundo con sombras ricas
                drawRect(
                    color = Color.Black.copy(alpha = 0.12f),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Overlay
                )
            }
            ColorProfileOption.WARM_NATURAL -> {
                // Calidez sutil dorada orgánica
                drawRect(
                    color = Color(0xFFFFB300).copy(alpha = 0.10f),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Color
                )
            }
            ColorProfileOption.MONOCHROME -> {
                // Blanco y negro nítido en vivo con alta fidelidad
                drawRect(
                    color = Color.Black,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Saturation
                )
            }
        }
    }
}
