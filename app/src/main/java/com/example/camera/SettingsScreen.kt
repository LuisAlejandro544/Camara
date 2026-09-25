package com.example.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.camera.nativeengine.NativeCameraEngine
import com.example.ui.theme.CameraBlack
import com.example.ui.theme.CameraControlBackground
import com.example.ui.theme.CameraTextPrimary
import com.example.ui.theme.CameraYellowAccent
import kotlin.math.roundToInt

/**
 * Pantalla modular e independiente de Configuración General de la Cámara.
 * Permite acceder a la calibración de color/exposición, ajustar ayudas de captura,
 * y revisar las capacidades físicas reales detectadas en el hardware del teléfono.
 */
@Composable
fun SettingsScreen(
    uiState: CameraUiState,
    onBack: () -> Unit,
    onOpenColorCalibration: () -> Unit,
    onToggleGrid: () -> Unit,
    onSelectAspectRatio: (AspectRatioOption) -> Unit = {},
    onSelectTimer: (TimerOption) -> Unit = {},
    onSelectGraphicsBackend: (GraphicsFilterBackend) -> Unit = {},
    onSetBeautyIntensity: (Float) -> Unit = {},
    onToggleWatermark: () -> Unit = {},
    onToggleAiMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CameraBlack)
            .statusBarsPadding()
    ) {
        // Barra Superior de Configuración
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back_desc),
                    tint = CameraTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = CameraTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_desc),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Contenido scrolleable organizado en tarjetas temáticas
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TARJETA DESTACADA: Calibración de Color y Contraste (Anti-Colores Lavados)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = CameraYellowAccent.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenColorCalibration() }
                    .testTag("open_color_calibration_card"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CameraYellowAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = CameraYellowAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_color_calibration_title),
                                color = CameraTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val activeBadge = if (uiState.isAntiWashedModeEnabled) {
                                "Antilavado Activo (${String.format(java.util.Locale.US, "%.1f", uiState.exposureCompensationEv)} EV)"
                            } else {
                                "Modo Estándar (0.0 EV)"
                            }
                            Text(
                                text = activeBadge,
                                color = if (uiState.isAntiWashedModeEnabled) CameraYellowAccent else Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.settings_color_calibration_desc),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // TARJETA: Inteligencia Artificial Local (IA Pro) - Zero-DCE Tone Mapping
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (uiState.isAiModeEnabled) CameraYellowAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isAiModeEnabled) CameraYellowAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (uiState.isAiModeEnabled) CameraYellowAccent else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Modo IA Pro (Mejora Inteligente)",
                                    color = CameraTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.isAiModeEnabled) "Activo: Zero-DCE Neural Tone Mapping" else "Desactivado (procesado estándar)",
                                    color = if (uiState.isAiModeEnabled) CameraYellowAccent else Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Switch(
                            checked = uiState.isAiModeEnabled,
                            onCheckedChange = { onToggleAiMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CameraYellowAccent,
                                checkedTrackColor = CameraYellowAccent.copy(alpha = 0.35f),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("settings_ai_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Analiza el rango dinámico de la foto en milisegundos mediante el motor nativo en C++20. Rescata detalles en sombras oscuras sin ruido y comprime altas luces para cielos definidos.",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            // 2. SECCIÓN: Captura y Composición
            SettingsSectionHeader(title = stringResource(R.string.settings_capture_hardware_title))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CameraControlBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Selector de Relación de Aspecto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            tint = CameraTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.aspect_ratio_label),
                                color = CameraTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${uiState.aspectRatio.label} • ${uiState.aspectRatio.description}",
                                color = CameraYellowAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botones de relación de aspecto en Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AspectRatioOption.entries.forEach { option ->
                            val isSelected = uiState.aspectRatio == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) CameraYellowAccent else Color.Black.copy(alpha = 0.4f))
                                    .border(
                                        BorderStroke(1.dp, if (isSelected) CameraYellowAccent else Color.White.copy(alpha = 0.2f)),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectAspectRatio(option) }
                                    .testTag("settings_aspect_ratio_${option.label}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = option.label,
                                        color = if (isSelected) CameraBlack else CameraTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = option.description.take(6),
                                        color = if (isSelected) CameraBlack.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.45f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Selector de Temporizador de Disparo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = CameraTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.timer_title),
                                color = CameraTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${uiState.timerOption.label} • ${uiState.timerOption.description}",
                                color = CameraYellowAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botones de Temporizador en Settings (OFF, 3s, 5s, 10s)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimerOption.entries.forEach { option ->
                            val isSelected = uiState.timerOption == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) CameraYellowAccent else Color.Black.copy(alpha = 0.4f))
                                    .border(
                                        BorderStroke(1.dp, if (isSelected) CameraYellowAccent else Color.White.copy(alpha = 0.2f)),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectTimer(option) }
                                    .testTag("settings_timer_${option.label}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = option.label,
                                        color = if (isSelected) CameraBlack else CameraTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = option.description.take(6),
                                        color = if (isSelected) CameraBlack.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.45f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Switch de Cuadrícula 3x3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = null,
                                tint = CameraTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_grid_title),
                                    color = CameraTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.settings_grid_desc),
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isGridEnabled,
                            onCheckedChange = { onToggleGrid() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CameraYellowAccent,
                                checkedTrackColor = CameraYellowAccent.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("settings_grid_switch")
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Switch de Marca de Agua Apex Camera
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = null,
                                tint = CameraYellowAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_watermark_title),
                                    color = CameraTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.settings_watermark_desc),
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isWatermarkEnabled,
                            onCheckedChange = { onToggleWatermark() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CameraYellowAccent,
                                checkedTrackColor = CameraYellowAccent.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("settings_watermark_switch")
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Aviso de Estabilidad de Mano
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = CameraTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_stability_title),
                                color = CameraTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.settings_stability_desc),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Activo",
                                color = Color(0xFF81C784),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. SECCIÓN: Capacidades de Hardware del Teléfono
            SettingsSectionHeader(title = stringResource(R.string.settings_hardware_info_title))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CameraControlBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    HardwareInfoRow(
                        icon = Icons.Default.Camera,
                        label = "Sensor de Foto Físico",
                        value = "${uiState.photoResolutionInfo.maxMegaPixels} MP (${uiState.photoResolutionInfo.maxResolutionString})"
                    )
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    HardwareInfoRow(
                        icon = Icons.Default.CheckCircle,
                        label = "Modo Alta Resolución (50MP)",
                        value = if (uiState.photoResolutionInfo.isRestrictedByOemOrOs) {
                            "Limitado a 12MP (Pixel Binning)"
                        } else {
                            "Activo (${uiState.photoResolutionInfo.maxMegaPixels}MP Nativo)"
                        }
                    )
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    HardwareInfoRow(
                        icon = Icons.Default.Videocam,
                        label = "Vídeo Máximo",
                        value = "${uiState.supportedVideoQualities.firstOrNull()?.label ?: "Full HD"} (${uiState.supportedFps.maxOrNull() ?: 30} FPS)"
                    )
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    HardwareInfoRow(
                        icon = Icons.Default.CheckCircle,
                        label = "Vídeo HDR 10-bit HLG",
                        value = if (uiState.isHdrSupported) "Compatible por Hardware" else "No soportado en este sensor"
                    )
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    HardwareInfoRow(
                        icon = Icons.Default.Palette,
                        label = "Control EV de Hardware",
                        value = if (uiState.isExposureCompensationSupported) {
                            "Rango: [${uiState.minExposureIndex} a ${uiState.maxExposureIndex}] (Paso: ${(uiState.exposureStep * 100).roundToInt() / 100f} EV)"
                        } else {
                            "Fijado por ISP"
                        }
                    )
                }
            }

            // TARJETA EDUCATIVA: Explicación de 50MP, Pixel Binning y Restricciones OEM / Android
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ℹ️ ¿Por qué 50MP vs 12MP en Android?",
                            color = CameraYellowAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. Pixel Binning (4 en 1): Los sensores modernos de 48MP o 50MP combinan 4 píxeles en 1 para capturar 4 veces más luz, generando fotos estándar de 12.5MP con colores más vivos y menos ruido en sombras.",
                        color = CameraTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "2. Requisito de Android 12 (API 31): Google no permitía a apps de terceros acceder al flujo de 50MP en versiones anteriores a Android 12. En Android 8 a 11, el sistema operativo siempre reporta 12MP.",
                        color = CameraTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val mfg = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                    Text(
                        text = "3. Bloqueo de Capas OEM: Fabricantes como $mfg (MIUI/HyperOS, HiOS, XOS, ColorOS, etc.) con frecuencia bloquean el acceso al modo de 50MP para apps de terceros y lo reservan exclusivamente para su propia app de cámara de fábrica.",
                        color = CameraTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "4. Grabación HDR 10-bit: Requiere que el procesador del móvil y el sensor tengan habilitado el perfil de color HLG de 10 bits en CameraX/Camera2.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // 4. SECCIÓN: Motor Gráfico de Filtros (Vulkan 1.1 / OpenGL ES)
            SettingsSectionHeader(title = "Motor Gráfico de Filtros")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CameraControlBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Aceleración de Filtros en Tiempo Real",
                        color = CameraTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isVulkan11Supported) {
                            "✅ Procesador compatible con Vulkan 1.1 (${uiState.vulkanVersionString}). Se utiliza Vulkan por defecto."
                        } else {
                            "ℹ️ Vulkan 1.1 no detectado (${uiState.vulkanVersionString}). Se utiliza OpenGL ES 3.2 optimizado."
                        },
                        color = if (uiState.isVulkan11Supported) CameraYellowAccent else Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Opciones de backend: Vulkan 1.1 vs OpenGL ES 3.2
                    GraphicsFilterBackend.entries.forEach { backend ->
                        val isSelected = uiState.selectedGraphicsBackend == backend
                        val isSupported = backend != GraphicsFilterBackend.VULKAN || uiState.isVulkan11Supported

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CameraYellowAccent.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, CameraYellowAccent)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = isSupported) {
                                    onSelectGraphicsBackend(backend)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = backend.label,
                                            color = if (isSupported) CameraTextPrimary else Color.White.copy(alpha = 0.4f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "• ACTIVO",
                                                color = CameraYellowAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = backend.description,
                                        color = if (isSupported) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.3f),
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                                if (!isSupported) {
                                    Text(
                                        text = "No compatible",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Calibración de intensidad del Filtro de Belleza
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intensidad Filtro Belleza",
                            color = CameraTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(uiState.beautyFilterIntensity * 100).toInt()}%",
                            color = CameraYellowAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = uiState.beautyFilterIntensity,
                        onValueChange = { onSetBeautyIntensity(it) },
                        valueRange = 0.2f..1.0f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = CameraYellowAccent,
                            activeTrackColor = CameraYellowAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    Text(
                        text = "Algoritmo bilateral en C++20 con preservación de bordes y niveles de negro. Suaviza imperfecciones y poros sin aplanar ni lavar el contraste de la foto.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // 5. SECCIÓN: Motor Nativo C++20
            SettingsSectionHeader(title = stringResource(R.string.settings_native_engine_title))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CameraControlBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = CameraYellowAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Librería libcamera_engine.so",
                                color = CameraTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = NativeCameraEngine.getVersion(),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Motor nativo C++20 compilado con CMake y empaquetado para arm64-v8a (64-bit) y armeabi-v7a (32-bit).",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = CameraYellowAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun HardwareInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CameraYellowAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = CameraTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}
