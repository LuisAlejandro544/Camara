package com.example.camera

import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
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
 * @param onZoomChanged Notifica cambios de zoom cuando el usuario pellizca la pantalla.
 */
@Composable
fun CameraPreviewView(
    lensFacing: LensFacing,
    captureMode: CaptureMode,
    selectedQuality: VideoQualityOption,
    isGridEnabled: Boolean,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onVideoCaptureReady: (VideoCapture<Recorder>) -> Unit,
    onCapabilitiesDetected: (VideoCapabilitiesInfo) -> Unit,
    onZoomChanged: (Float) -> Unit,
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

    // Reacciona al cambio de orientación de cámara, modo de captura, resolución o ciclo de vida
    LaunchedEffect(lensFacing, captureMode, selectedQuality, lifecycleOwner) {
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
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    val boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    cameraInstance = boundCamera
                    onImageCaptureReady(imageCapture)

                    // Detecta capacidades de vídeo del sensor para tenerlas listas al cambiar de modo
                    val capabilities = CameraVideoManager.detectVideoCapabilities(boundCamera.cameraInfo)
                    onCapabilitiesDetected(capabilities)
                } else {
                    // Modo Vídeo: Configura el Recorder con la calidad seleccionada
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(selectedQuality.quality))
                        .build()
                    val videoCapture = VideoCapture.withOutput(recorder)

                    val boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        videoCapture
                    )
                    cameraInstance = boundCamera
                    onVideoCaptureReady(videoCapture)

                    val capabilities = CameraVideoManager.detectVideoCapabilities(boundCamera.cameraInfo)
                    onCapabilitiesDetected(capabilities)
                }
            } catch (e: Exception) {
                // Manejo seguro si la combinación de casos de uso o cámara no está disponible
            }
        }, executor)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Gesto de pellizco para zoom táctil (Pinch to Zoom)
                detectTransformGestures { _, _, zoom, _ ->
                    cameraInstance?.let { cam ->
                        val zoomState = cam.cameraInfo.zoomState.value
                        val minRatio = zoomState?.minZoomRatio ?: 1f
                        val maxRatio = zoomState?.maxZoomRatio ?: 5f
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
