package com.example.camera

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.CameraBlack
import com.example.ui.theme.CameraControlBackground
import com.example.ui.theme.CameraShutterInner
import com.example.ui.theme.CameraShutterOuter
import com.example.ui.theme.CameraTextPrimary
import com.example.ui.theme.CameraYellowAccent
import java.util.Locale

/**
 * Pantalla principal de la cámara con soporte integral para Fotografía y Grabación de Vídeo.
 *
 * Características:
 * - Visor reactivo con detección de capacidades de hardware (resoluciones 4K/FHD/HD y FPS).
 * - Selector fluido entre los modos FOTO y VÍDEO.
 * - Indicador dinámico de grabación (tiempo transcurrido y punto rojo pulsante).
 * - Botón de ajustes de vídeo para calibrar calidad y FPS según el hardware del teléfono.
 * - Obturador híbrido adaptativo según el modo activo.
 */
@Composable
fun CameraScreen(
    uiState: CameraUiState,
    onSetCaptureMode: (CaptureMode) -> Unit,
    onToggleFlash: () -> Unit,
    onToggleLens: () -> Unit,
    onToggleGrid: () -> Unit,
    onZoomChanged: (Float) -> Unit,
    onCaptureStarted: () -> Unit,
    onPhotoCaptured: (Uri, String) -> Unit,
    onCaptureError: (String) -> Unit,
    onVideoRecordingStarted: () -> Unit,
    onVideoDurationUpdate: (Long) -> Unit,
    onVideoSaved: (Uri, String) -> Unit,
    onVideoError: (String) -> Unit,
    onSelectQuality: (VideoQualityOption) -> Unit,
    onSelectFps: (Int) -> Unit,
    onToggleAudio: () -> Unit,
    onOpenVideoSettings: (Boolean) -> Unit,
    onCapabilitiesDetected: (VideoCapabilitiesInfo) -> Unit,
    onThumbnailClick: (Uri) -> Unit,
    onClearUserMessage: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var activeVideoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecordingSession by remember { mutableStateOf<Recording?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Notificación en pantalla cuando hay mensajes de estado
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearUserMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CameraBlack)
    ) {
        // 1. Visor en vivo con soporte para Fotos y Vídeo
        CameraPreviewView(
            lensFacing = uiState.lensFacing,
            captureMode = uiState.captureMode,
            selectedQuality = uiState.selectedVideoQuality,
            isGridEnabled = uiState.isGridEnabled,
            onImageCaptureReady = { capture -> activeImageCapture = capture },
            onVideoCaptureReady = { videoCapture -> activeVideoCapture = videoCapture },
            onCapabilitiesDetected = onCapabilitiesDetected,
            onZoomChanged = onZoomChanged,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Destello blanco del obturador visual al tomar una foto
        if (uiState.showShutterFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }

        // 3. Indicador de nivel de Zoom (aparece si zoom > 1.05x)
        if (uiState.zoomRatio > 1.05f) {
            Surface(
                color = CameraControlBackground,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 140.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.1fx", uiState.zoomRatio),
                    color = CameraYellowAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // 4. Barra Superior de Controles
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Control del Flash
                IconButton(
                    onClick = onToggleFlash,
                    enabled = !uiState.isRecordingVideo,
                    modifier = Modifier
                        .size(48.dp)
                        .background(CameraControlBackground, CircleShape)
                        .testTag("flash_toggle_button")
                ) {
                    val (flashIcon, tintColor) = when (uiState.flashMode) {
                        FlashMode.AUTO -> Icons.Default.FlashAuto to CameraTextPrimary
                        FlashMode.ON -> Icons.Default.FlashOn to CameraYellowAccent
                        FlashMode.OFF -> Icons.Default.FlashOff to Color.White.copy(alpha = 0.5f)
                    }
                    Icon(
                        imageVector = flashIcon,
                        contentDescription = stringResource(R.string.flash_mode_desc),
                        tint = tintColor
                    )
                }

                // Centro: Indicador de Grabación Activa O Badge de Resolución
                if (uiState.isRecordingVideo) {
                    // Cronómetro de Grabación con punto rojo pulsante
                    VideoRecordingBadge(durationSeconds = uiState.recordingDurationSeconds)
                } else if (uiState.captureMode == CaptureMode.VIDEO) {
                    // Botón para desplegar ajustes de vídeo (calidad y FPS detectados)
                    Surface(
                        color = CameraControlBackground,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clickable { onOpenVideoSettings(true) }
                            .testTag("video_settings_badge_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${uiState.selectedVideoQuality.label} • ${uiState.selectedFps} FPS",
                                color = CameraYellowAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes de vídeo",
                                tint = CameraTextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    // Modo Foto: Badge visual
                    Surface(
                        color = CameraControlBackground,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.mode_photo),
                            color = CameraYellowAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // Control de Cuadrícula
                IconButton(
                    onClick = onToggleGrid,
                    modifier = Modifier
                        .size(48.dp)
                        .background(CameraControlBackground, CircleShape)
                        .testTag("grid_toggle_button")
                ) {
                    val gridIcon = if (uiState.isGridEnabled) Icons.Default.GridOn else Icons.Default.GridOff
                    val tintColor = if (uiState.isGridEnabled) CameraYellowAccent else Color.White.copy(alpha = 0.6f)
                    Icon(
                        imageVector = gridIcon,
                        contentDescription = "Cuadrícula",
                        tint = tintColor
                    )
                }
            }
        }

        // 5. Barra Inferior de Controles y Selector de Modos
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Selector de Modos: FOTO | VÍDEO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modo FOTO
                Text(
                    text = stringResource(R.string.mode_photo),
                    color = if (uiState.captureMode == CaptureMode.PHOTO) CameraYellowAccent else Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = if (uiState.captureMode == CaptureMode.PHOTO) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !uiState.isRecordingVideo) {
                            onSetCaptureMode(CaptureMode.PHOTO)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("mode_photo_button")
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Modo VÍDEO
                Text(
                    text = stringResource(R.string.mode_video),
                    color = if (uiState.captureMode == CaptureMode.VIDEO) {
                        if (uiState.isRecordingVideo) Color(0xFFFF5252) else CameraYellowAccent
                    } else Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = if (uiState.captureMode == CaptureMode.VIDEO) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !uiState.isRecordingVideo) {
                            onRequestAudioPermission()
                            onSetCaptureMode(CaptureMode.VIDEO)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("mode_video_button")
                )
            }

            // Fila de Disparo: Miniatura, Obturador y Alternador de Lente
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniatura de la última captura
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CameraControlBackground)
                        .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        .clickable(enabled = uiState.lastPhotoUri != null && !uiState.isRecordingVideo) {
                            uiState.lastPhotoUri?.let { uri -> onThumbnailClick(uri) }
                        }
                        .testTag("gallery_thumbnail_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.lastPhotoUri != null) {
                        AsyncImage(
                            model = uiState.lastPhotoUri,
                            contentDescription = stringResource(R.string.gallery_thumbnail_desc),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = stringResource(R.string.gallery_thumbnail_desc),
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Botón Obturador Híbrido (Foto o Vídeo)
                CameraShutterButton(
                    captureMode = uiState.captureMode,
                    isCapturing = uiState.isCapturing,
                    isRecordingVideo = uiState.isRecordingVideo,
                    onClick = {
                        if (uiState.captureMode == CaptureMode.PHOTO) {
                            if (!uiState.isCapturing) {
                                CameraCaptureManager.takePhoto(
                                    context = context,
                                    imageCapture = activeImageCapture,
                                    flashMode = uiState.flashMode,
                                    onStart = onCaptureStarted,
                                    onSuccess = onPhotoCaptured,
                                    onError = onCaptureError
                                )
                            }
                        } else {
                            // Modo Vídeo: Iniciar o detener grabación
                            if (!uiState.isRecordingVideo) {
                                val recording = CameraVideoManager.startRecording(
                                    context = context,
                                    videoCapture = activeVideoCapture,
                                    isAudioEnabled = uiState.isAudioEnabled,
                                    onStarted = onVideoRecordingStarted,
                                    onDurationUpdate = onVideoDurationUpdate,
                                    onSuccess = { uri, path ->
                                        activeRecordingSession = null
                                        onVideoSaved(uri, path)
                                    },
                                    onError = { errorMsg ->
                                        activeRecordingSession = null
                                        onVideoError(errorMsg)
                                    }
                                )
                                activeRecordingSession = recording
                            } else {
                                // Detener grabación
                                activeRecordingSession?.stop()
                                activeRecordingSession = null
                            }
                        }
                    },
                    modifier = Modifier.testTag("shutter_button")
                )

                // Botón para alternar lente frontal/trasera
                var lensRotation by remember { mutableFloatStateOf(0f) }
                val animatedRotation by animateFloatAsState(
                    targetValue = lensRotation,
                    animationSpec = spring(dampingRatio = 0.7f),
                    label = "LensRotation"
                )

                IconButton(
                    onClick = {
                        lensRotation += 180f
                        onToggleLens()
                    },
                    enabled = !uiState.isRecordingVideo,
                    modifier = Modifier
                        .size(56.dp)
                        .background(CameraControlBackground, CircleShape)
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = stringResource(R.string.switch_camera_desc),
                        tint = if (uiState.isRecordingVideo) Color.White.copy(alpha = 0.3f) else CameraTextPrimary,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(animatedRotation)
                    )
                }
            }
        }

        // 6. Diálogo de Ajustes de Vídeo (Resolución y FPS detectados)
        if (uiState.isVideoSettingsOpen) {
            VideoSettingsSheet(
                uiState = uiState,
                onSelectQuality = { quality ->
                    onSelectQuality(quality)
                    onOpenVideoSettings(false)
                },
                onSelectFps = { fps ->
                    onSelectFps(fps)
                    onOpenVideoSettings(false)
                },
                onToggleAudio = onToggleAudio,
                onDismiss = { onOpenVideoSettings(false) }
            )
        }

        // Host para notificaciones Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp)
        )
    }
}

/**
 * Indicador visual superior animado durante la grabación de vídeo.
 */
@Composable
private fun VideoRecordingBadge(durationSeconds: Long) {
    val infiniteTransition = rememberInfiniteTransition(label = "RecPulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RecPulseAlpha"
    )

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Surface(
        color = Color(0xFF1E1E1E).copy(alpha = 0.9f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5252).copy(alpha = alphaAnim))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "REC $formattedTime",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Botón disparador del obturador adaptativo:
 * - Modo Foto: Círculo blanco que se reduce al disparar.
 * - Modo Vídeo en reposo: Anillo blanco con centro rojo.
 * - Modo Vídeo grabando: Anillo blanco con cuadrado rojo central (botón de parada).
 */
@Composable
private fun CameraShutterButton(
    captureMode: CaptureMode,
    isCapturing: Boolean,
    isRecordingVideo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed || isCapturing) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "ShutterScale"
    )

    Box(
        modifier = modifier
            .size(84.dp)
            .scale(scale)
            .border(
                width = 4.dp,
                color = if (isRecordingVideo) Color(0xFFFF5252) else CameraShutterOuter,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isCapturing,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (captureMode == CaptureMode.PHOTO) {
            // Disparador de Foto
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(if (isCapturing) CameraYellowAccent else CameraShutterInner)
            )
        } else {
            // Disparador de Vídeo
            if (isRecordingVideo) {
                // Cuadrado de parada de grabación
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF5252))
                )
            } else {
                // Círculo rojo de inicio de grabación
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5252))
                )
            }
        }
    }
}
