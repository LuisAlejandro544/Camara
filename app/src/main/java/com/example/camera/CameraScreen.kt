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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    onToggleMaxMegapixels: () -> Unit,
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
    onToggleHdr: () -> Unit,
    onOpenVideoSettings: (Boolean) -> Unit,
    onCapabilitiesDetected: (VideoCapabilitiesInfo) -> Unit,
    onPhotoCapabilitiesDetected: (PhotoResolutionInfo) -> Unit,
    onThumbnailClick: (Uri) -> Unit,
    onClearUserMessage: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onZoomLimitsDetected: (Float, Float) -> Unit,
    onOpenSettings: () -> Unit,
    onExposureLimitsDetected: (min: Int, max: Int, step: Float, isSupported: Boolean) -> Unit,
    onOpenHighResInfoDialog: (Boolean) -> Unit = {},
    onToggleBeautyFilter: () -> Unit = {},
    onSelectAspectRatio: (AspectRatioOption) -> Unit = {},
    onToggleAspectRatioSelector: () -> Unit = {},
    onSetTimerOption: (TimerOption) -> Unit = {},
    onToggleTimerSelector: () -> Unit = {},
    onToggleFilterSelector: () -> Unit = {},
    onSelectColorProfile: (ColorProfileOption) -> Unit = {},
    onStartCountdown: ((() -> Unit)) -> Unit = {},
    onCancelCountdown: () -> Unit = {},
    onToggleWatermark: () -> Unit = {},
    onToggleAiMode: () -> Unit = {},
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
        // 1. Visor en vivo con soporte para Fotos (High-Res MP) y Vídeo (4K/2K/HD) sin límite artificial de zoom
        CameraPreviewView(
            lensFacing = uiState.lensFacing,
            captureMode = uiState.captureMode,
            selectedQuality = uiState.selectedVideoQuality,
            aspectRatio = uiState.aspectRatio,
            colorProfile = uiState.colorProfile,
            isMaxMegapixelsEnabled = uiState.isMaxMegapixelsEnabled,
            isHdrVideoEnabled = uiState.isHdrVideoEnabled,
            isGridEnabled = uiState.isGridEnabled,
            zoomRatio = uiState.zoomRatio,
            targetExposureIndex = uiState.exposureCompensationIndex,
            onImageCaptureReady = { capture -> activeImageCapture = capture },
            onVideoCaptureReady = { videoCapture -> activeVideoCapture = videoCapture },
            onCapabilitiesDetected = onCapabilitiesDetected,
            onPhotoCapabilitiesDetected = onPhotoCapabilitiesDetected,
            onZoomLimitsDetected = onZoomLimitsDetected,
            onZoomChanged = onZoomChanged,
            onExposureLimitsDetected = onExposureLimitsDetected,
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

        // 3. Indicador flotante de nivel de Zoom (aparece si el zoom difiere de 1.0x)
        if (uiState.zoomRatio > 1.05f || uiState.zoomRatio < 0.95f) {
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

        // 3.1. Aviso visual dinámico en modo Alta Resolución: "Mantén el dispositivo quieto…"
        if (uiState.captureMode == CaptureMode.PHOTO && uiState.isMaxMegapixelsEnabled) {
            Surface(
                color = if (uiState.isDeviceSteady) Color(0xFF1E2B1E).copy(alpha = 0.85f) else Color(0xFF332014).copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.isDeviceSteady) Color(0xFF4CAF50).copy(alpha = 0.8f) else Color(0xFFFFB300).copy(alpha = 0.8f)
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 92.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isDeviceSteady) Color(0xFF4CAF50) else Color(0xFFFFB300))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isDeviceSteady) {
                            "${uiState.photoResolutionInfo.maxMegaPixels}MP • " + stringResource(R.string.hold_steady_message)
                        } else {
                            "${uiState.photoResolutionInfo.maxMegaPixels}MP • Estabiliza la mano…"
                        },
                        color = if (uiState.isDeviceSteady) Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3.2. Distintivo sutil de Calibración Antilavado activa
        if (uiState.captureMode == CaptureMode.PHOTO && uiState.isAntiWashedModeEnabled && !uiState.isMaxMegapixelsEnabled) {
            Surface(
                color = CameraControlBackground.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    CameraYellowAccent.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 92.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(CameraYellowAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Antilavado: ${String.format(Locale.US, "%.1f", uiState.exposureCompensationEv)} EV • ${uiState.colorProfile.label}",
                        color = CameraYellowAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 3.3. Overlay Central de Cuenta Regresiva del Temporizador de Disparo
        if (uiState.activeTimerSecondsRemaining != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Círculo translúcido de cuenta regresiva con número grande en amarillo
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(3.dp, CameraYellowAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${uiState.activeTimerSecondsRemaining}",
                            color = CameraYellowAccent,
                            fontSize = 68.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.timer_countdown_label) + " ${uiState.activeTimerSecondsRemaining}s…",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botón de Cancelación del Temporizador
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onCancelCountdown() }
                            .testTag("cancel_timer_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.timer_cancel),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.timer_cancel),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 4. Barra Superior de Controles (Diseño compacto, elegante y espacioso)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Control del Flash (Compacto y elegante)
                ElegantTopIconButton(
                    onClick = onToggleFlash,
                    enabled = !uiState.isRecordingVideo,
                    icon = when (uiState.flashMode) {
                        FlashMode.AUTO -> Icons.Default.FlashAuto
                        FlashMode.ON -> Icons.Default.FlashOn
                        FlashMode.OFF -> Icons.Default.FlashOff
                    },
                    contentDescription = stringResource(R.string.flash_mode_desc),
                    tint = when (uiState.flashMode) {
                        FlashMode.AUTO -> CameraTextPrimary
                        FlashMode.ON -> CameraYellowAccent
                        FlashMode.OFF -> Color.White.copy(alpha = 0.45f)
                    },
                    testTag = "flash_toggle_button"
                )

                // Centro: Indicador de Grabación, Ajustes de Vídeo con HDR Rápido, O Badge de 50MP
                if (uiState.isRecordingVideo) {
                    // Cronómetro de Grabación con punto rojo pulsante
                    VideoRecordingBadge(durationSeconds = uiState.recordingDurationSeconds)
                } else if (uiState.captureMode == CaptureMode.VIDEO) {
                    // Fila de controles de vídeo: Resolución/FPS y botón directo de HDR
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge con Calidad y FPS de vídeo
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenVideoSettings(true) }
                                .testTag("video_settings_badge_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${uiState.selectedVideoQuality.label} • ${uiState.selectedFps} FPS",
                                    color = CameraYellowAccent,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Ajustes de vídeo",
                                    tint = CameraTextPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // Botón de acceso directo a HDR para vídeo
                        val isHdrOn = uiState.isHdrVideoEnabled
                        Surface(
                            color = if (isHdrOn) CameraYellowAccent else Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, if (isHdrOn) CameraYellowAccent else Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onToggleHdr() }
                                .testTag("video_hdr_quick_toggle")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HdrOn,
                                    contentDescription = "HDR Vídeo",
                                    tint = if (isHdrOn) CameraBlack else CameraYellowAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isHdrOn) "HDR" else "SDR",
                                    color = if (isHdrOn) CameraBlack else CameraTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Modo Foto: Controles centrales compactos y equilibrados (MP, Filtros en Vivo, Belleza)
                    val maxMp = uiState.photoResolutionInfo.maxMegaPixels
                    val isRestricted = uiState.photoResolutionInfo.isRestrictedByOemOrOs
                    val isHighResOn = uiState.isMaxMegapixelsEnabled
                    val isBeautyOn = uiState.isBeautyFilterEnabled
                    val isFilterActive = uiState.colorProfile != ColorProfileOption.STANDARD

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Selector de Megapíxeles Reales (Diseño compacto sin palabras desbordantes)
                        Surface(
                            color = if (isHighResOn) CameraYellowAccent else Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, if (isHighResOn) CameraYellowAccent else Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    if (isRestricted) {
                                        onOpenHighResInfoDialog(true)
                                    }
                                    onToggleMaxMegapixels()
                                }
                                .testTag("photo_megapixels_toggle_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${maxMp}MP",
                                    color = if (isHighResOn) CameraBlack else CameraTextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                                if (isRestricted) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(if (isHighResOn) CameraBlack.copy(alpha = 0.25f) else CameraYellowAccent.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "i",
                                            color = if (isHighResOn) CameraBlack else CameraYellowAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Acceso directo a Filtros de Color en Vivo (Vívido Antilavado, Contraste, Cálido, B&N)
                        Surface(
                            color = if (uiState.isFilterSelectorOpen || isFilterActive) CameraYellowAccent else Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, if (uiState.isFilterSelectorOpen || isFilterActive) CameraYellowAccent else Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onToggleFilterSelector() }
                                .testTag("filter_selector_toggle_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Filtros en vivo",
                                    tint = if (uiState.isFilterSelectorOpen || isFilterActive) CameraBlack else CameraYellowAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = when (uiState.colorProfile) {
                                        ColorProfileOption.STANDARD -> "Filtro"
                                        ColorProfileOption.VIVID_ANTI_WASHED -> "Vívido"
                                        ColorProfileOption.DEEP_CONTRAST -> "Contraste"
                                        ColorProfileOption.WARM_NATURAL -> "Cálido"
                                        ColorProfileOption.MONOCHROME -> "B&N"
                                    },
                                    color = if (uiState.isFilterSelectorOpen || isFilterActive) CameraBlack else CameraTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 3. Filtro de Belleza (Vulkan 1.1 / OpenGL ES 3.2 en C++20) compacto
                        Surface(
                            color = if (isBeautyOn) CameraYellowAccent else Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, if (isBeautyOn) CameraYellowAccent else Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onToggleBeautyFilter() }
                                .testTag("beauty_filter_toggle_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Filtro de Belleza",
                                    tint = if (isBeautyOn) CameraBlack else CameraYellowAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                if (isBeautyOn) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Belleza",
                                        color = CameraBlack,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 4. Modo IA Pro (Estimación de Curvas y Mejora Inteligente en C++20)
                        val isAiOn = uiState.isAiModeEnabled
                        Surface(
                            color = if (isAiOn) CameraYellowAccent else Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, if (isAiOn) CameraYellowAccent else Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onToggleAiMode() }
                                .testTag("ai_mode_toggle_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Modo IA Pro",
                                    tint = if (isAiOn) CameraBlack else CameraYellowAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isAiOn) "IA Pro" else "IA OFF",
                                    color = if (isAiOn) CameraBlack else CameraTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Derecha: Selector de Temporizador, Selector de Aspect Ratio, Control de Cuadrícula y Botón de Configuración
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Botón de Temporizador de Disparo (OFF, 3s, 5s, 10s)
                    if (uiState.captureMode == CaptureMode.PHOTO) {
                        val isTimerActive = uiState.timerOption != TimerOption.OFF
                        val isTimerBarOpen = uiState.isTimerSelectorOpen
                        Surface(
                            color = if (isTimerBarOpen || isTimerActive) CameraYellowAccent else Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, if (isTimerBarOpen || isTimerActive) CameraYellowAccent else Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(enabled = !uiState.isRecordingVideo) { onToggleTimerSelector() }
                                .testTag("timer_toggle_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTimerActive) Icons.Default.Timer else Icons.Default.TimerOff,
                                    contentDescription = stringResource(R.string.timer_title),
                                    tint = if (isTimerBarOpen || isTimerActive) CameraBlack else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                if (isTimerActive) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = uiState.timerOption.label,
                                        color = CameraBlack,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    // 2. Botón de Selector de Relación de Aspecto (Full, 16:9, 4:3, 1:1)
                    Surface(
                        color = if (uiState.isAspectRatioSelectorOpen) CameraYellowAccent else Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, if (uiState.isAspectRatioSelectorOpen) CameraYellowAccent else Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = !uiState.isRecordingVideo) { onToggleAspectRatioSelector() }
                            .testTag("aspect_ratio_toggle_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = uiState.aspectRatio.label,
                                color = if (uiState.isAspectRatioSelectorOpen) CameraBlack else CameraYellowAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // 3. Control de Cuadrícula (Compacto y elegante)
                    ElegantTopIconButton(
                        onClick = onToggleGrid,
                        icon = if (uiState.isGridEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Cuadrícula",
                        tint = if (uiState.isGridEnabled) CameraYellowAccent else Color.White.copy(alpha = 0.6f),
                        testTag = "grid_toggle_button"
                    )

                    // 4. Botón de Configuración (Tuerca independiente)
                    ElegantTopIconButton(
                        onClick = onOpenSettings,
                        enabled = !uiState.isRecordingVideo,
                        icon = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_desc),
                        tint = CameraTextPrimary,
                        testTag = "settings_button"
                    )
                }
            }

            // Barra horizontal desplegable con las 4 opciones de Temporizador: [OFF] [3s] [5s] [10s]
            if (uiState.isTimerSelectorOpen && !uiState.isRecordingVideo) {
                Surface(
                    color = Color.Black.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("timer_selector_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimerOption.entries.forEach { option ->
                            val isSelected = uiState.timerOption == option
                            Surface(
                                color = if (isSelected) CameraYellowAccent else Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp),
                                border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSetTimerOption(option) }
                                    .testTag("timer_option_${option.label}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = option.label,
                                        color = if (isSelected) CameraBlack else CameraTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = option.description,
                                        color = if (isSelected) CameraBlack.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                                        fontSize = 9.5.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Barra horizontal desplegable con las 4 opciones de Aspect Ratio: [Full] [16:9] [4:3] [1:1]
            // Altura uniforme, simétrica y textos de 1 sola línea para evitar deformidades
            if (uiState.isAspectRatioSelectorOpen && !uiState.isRecordingVideo) {
                Surface(
                    color = Color.Black.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("aspect_ratio_selector_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AspectRatioOption.entries.forEach { option ->
                            val isSelected = uiState.aspectRatio == option
                            Surface(
                                color = if (isSelected) CameraYellowAccent else Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp),
                                border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSelectAspectRatio(option) }
                                    .testTag("aspect_ratio_option_${option.label}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = option.label,
                                        color = if (isSelected) CameraBlack else CameraTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    val shortDesc = when (option) {
                                        AspectRatioOption.FULL -> "Pantalla"
                                        AspectRatioOption.RATIO_16_9 -> "16:9"
                                        AspectRatioOption.RATIO_4_3 -> "Sensor"
                                        AspectRatioOption.RATIO_1_1 -> "1:1"
                                    }
                                    Text(
                                        text = shortDesc,
                                        color = if (isSelected) CameraBlack.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                                        fontSize = 9.5.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Barra horizontal desplegable con las 5 opciones de Filtros de Color en Vivo:
            // [Vívido] [Contraste] [Cálido] [B&N] [Estándar]
            if (uiState.isFilterSelectorOpen && !uiState.isRecordingVideo) {
                Surface(
                    color = Color.Black.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, CameraYellowAccent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("filter_selector_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorProfileOption.entries.forEach { option ->
                            val isSelected = uiState.colorProfile == option
                            Surface(
                                color = if (isSelected) CameraYellowAccent else Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp),
                                border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSelectColorProfile(option) }
                                    .testTag("filter_option_${option.name}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp)
                                ) {
                                    val (nameText, descText) = when (option) {
                                        ColorProfileOption.VIVID_ANTI_WASHED -> "Vívido" to "Antilavado"
                                        ColorProfileOption.DEEP_CONTRAST -> "Contraste" to "Curva S"
                                        ColorProfileOption.WARM_NATURAL -> "Cálido" to "Natural"
                                        ColorProfileOption.MONOCHROME -> "B&N" to "Artístico"
                                        ColorProfileOption.STANDARD -> "Estándar" to "Sensor"
                                    }
                                    Text(
                                        text = nameText,
                                        color = if (isSelected) CameraBlack else CameraTextPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = descText,
                                        color = if (isSelected) CameraBlack.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                                        fontSize = 8.5.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
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
            // Selector rápido de niveles de Zoom dinámicos (se adapta a los límites reales del hardware: 1x, 2x, 5x, 10x...)
            val maxZ = uiState.maxZoomRatio
            val minZ = uiState.minZoomRatio
            val zoomSteps = remember(minZ, maxZ) {
                val list = mutableListOf<Float>()
                if (minZ < 0.95f) list.add(minZ) // Gran angular (0.5x o 0.6x si el sensor lo tiene)
                list.add(1.0f)
                if (maxZ >= 2.0f) list.add(2.0f)
                if (maxZ >= 5.0f) list.add(5.0f)
                if (maxZ >= 10.0f) list.add(10.0f)
                // Si el dispositivo soporta más de 10x (ej. 20x, 30x, 50x o 100x), agregamos su tope máximo real
                if (maxZ > 10.5f && !list.contains(maxZ)) {
                    list.add(maxZ)
                }
                list.sorted()
            }

            if (zoomSteps.size > 1 && !uiState.isRecordingVideo) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            zoomSteps.forEach { step ->
                                val isSelected = kotlin.math.abs(uiState.zoomRatio - step) < 0.15f
                                val label = if (step >= 1.0f && step % 1.0f == 0f) {
                                    "${step.toInt()}x"
                                } else {
                                    String.format(Locale.US, "%.1fx", step)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) CameraYellowAccent else Color.Transparent)
                                        .clickable { onZoomChanged(step) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) CameraBlack else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
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
                            modifier = Modifier.size(22.dp)
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
                            if (uiState.activeTimerSecondsRemaining != null) {
                                // Tocar el obturador durante la cuenta regresiva cancela el temporizador
                                onCancelCountdown()
                            } else if (!uiState.isCapturing) {
                                val performPhotoCapture = {
                                    CameraCaptureManager.takePhoto(
                                        context = context,
                                        imageCapture = activeImageCapture,
                                        flashMode = uiState.flashMode,
                                        aspectRatio = uiState.aspectRatio,
                                        colorProfile = uiState.colorProfile,
                                        isBeautyFilterEnabled = uiState.isBeautyFilterEnabled,
                                        beautyIntensity = uiState.beautyFilterIntensity,
                                        isVulkanBackend = uiState.selectedGraphicsBackend == GraphicsFilterBackend.VULKAN,
                                        isWatermarkEnabled = uiState.isWatermarkEnabled,
                                        isAiModeEnabled = uiState.isAiModeEnabled,
                                        onStart = onCaptureStarted,
                                        onSuccess = onPhotoCaptured,
                                        onError = onCaptureError
                                    )
                                }

                                if (uiState.timerOption == TimerOption.OFF) {
                                    performPhotoCapture()
                                } else {
                                    onStartCountdown(performPhotoCapture)
                                }
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = stringResource(R.string.switch_camera_desc),
                        tint = if (uiState.isRecordingVideo) Color.White.copy(alpha = 0.3f) else CameraTextPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(animatedRotation)
                    )
                }
            }
        }

        // 6. Diálogo de Ajustes de Vídeo (Resolución, FPS y HDR si el sensor lo soporta)
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
                onToggleHdr = onToggleHdr,
                onDismiss = { onOpenVideoSettings(false) }
            )
        }

        // 7. Diálogo Explicativo de 50MP, Pixel Binning y Restricciones del Sistema
        if (uiState.isHighResInfoDialogOpen) {
            HighResInfoDialog(
                photoInfo = uiState.photoResolutionInfo,
                onDismiss = { onOpenHighResInfoDialog(false) }
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

/**
 * Botón superior compacto, elegante y táctilmente preciso para la barra de control.
 */
@Composable
private fun ElegantTopIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(38.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Diálogo explicativo sobre sensores de 50MP, Pixel Binning y restricciones de Android / fabricantes.
 */
@Composable
private fun HighResInfoDialog(
    photoInfo: PhotoResolutionInfo,
    onDismiss: () -> Unit
) {
    val manufacturer = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF1E1E1E))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sensor de ${photoInfo.maxMegaPixels}MP y Pixel Binning",
                        color = CameraYellowAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "• Sensor Físico Detectado: ${photoInfo.maxResolutionString} (${photoInfo.maxMegaPixels} MP)",
                    color = CameraTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (photoInfo.isRestrictedByOemOrOs) {
                        photoInfo.restrictionReason
                            ?: "El sistema o la capa de personalización de $manufacturer limitan el acceso directo al flujo de 50MP para apps de terceros, entregando el modo estándar de 12MP optimizado mediante pixel binning (4 en 1)."
                    } else {
                        "Tu dispositivo permite captura a resolución completa de ${photoInfo.maxMegaPixels}MP sin restricciones artificiales. Mantén el teléfono firme al tomar la foto para máxima nitidez."
                    },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "💡 El Pixel Binning es una técnica de los fabricantes que combina 4 píxeles en 1 para capturar mucha más luz y colores más vivos, evitando ruido visual en fotos cotidianas.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = CameraYellowAccent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDismiss() }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Entendido",
                            color = CameraBlack,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
