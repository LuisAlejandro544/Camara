package com.example.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.CameraBlack
import com.example.ui.theme.CameraControlBackground
import com.example.ui.theme.CameraDarkSurface
import com.example.ui.theme.CameraTextPrimary
import com.example.ui.theme.CameraTextSecondary
import com.example.ui.theme.CameraYellowAccent

/**
 * Diálogo de configuración avanzada para los parámetros de grabación de vídeo.
 * Permite seleccionar entre las resoluciones y tasas de cuadros (FPS) detectadas
 * físicamente en el sensor del teléfono, además de activar o desactivar el micrófono
 * y el modo HDR (10 bits) si el hardware del sensor lo admite.
 *
 * @param uiState Estado actual de la cámara con las capacidades detectadas.
 * @param onSelectQuality Callback invocado al seleccionar una resolución.
 * @param onSelectFps Callback invocado al seleccionar una tasa de FPS.
 * @param onToggleAudio Callback invocado al alternar la grabación de audio.
 * @param onToggleHdr Callback invocado al alternar la grabación en HDR de 10 bits.
 * @param onDismiss Callback para cerrar el diálogo.
 */
@Composable
fun VideoSettingsSheet(
    uiState: CameraUiState,
    onSelectQuality: (VideoQualityOption) -> Unit,
    onSelectFps: (Int) -> Unit,
    onToggleAudio: () -> Unit,
    onToggleHdr: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(CameraDarkSurface)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Cabecera con título e icono de cierre
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(CameraYellowAccent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = CameraYellowAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.video_settings_title),
                            color = CameraTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(CameraControlBackground, CircleShape)
                            .testTag("close_video_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = CameraTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sección 1: Resoluciones detectadas en el hardware
                Text(
                    text = stringResource(R.string.video_resolution_label).uppercase(),
                    color = CameraYellowAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.supportedVideoQualities.forEach { qualityOption ->
                        val isSelected = uiState.selectedVideoQuality == qualityOption
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CameraYellowAccent.copy(alpha = 0.18f) else CameraControlBackground
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, CameraYellowAccent)
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectQuality(qualityOption) }
                                .testTag("quality_option_${qualityOption.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = qualityOption.label,
                                        color = if (isSelected) CameraYellowAccent else CameraTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = qualityOption.resolutionHint,
                                        color = CameraTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(CameraYellowAccent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seleccionado",
                                            tint = CameraBlack,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sección 2: Tasas de cuadros (FPS) detectadas en el sensor
                Text(
                    text = stringResource(R.string.video_fps_label).uppercase(),
                    color = CameraYellowAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    uiState.supportedFps.forEach { fpsValue ->
                        val isSelected = uiState.selectedFps == fpsValue
                        Surface(
                            color = if (isSelected) CameraYellowAccent.copy(alpha = 0.2f) else CameraControlBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, CameraYellowAccent)
                            } else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectFps(fpsValue) }
                                .testTag("fps_option_$fpsValue")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (isSelected) CameraYellowAccent else CameraTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$fpsValue FPS",
                                    color = if (isSelected) CameraYellowAccent else CameraTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sección 3: Modo HDR (10-bit HLG) - Siempre visible para el usuario con estado y explicación
                val manufacturerName = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isHdrVideoEnabled) CameraYellowAccent.copy(alpha = 0.15f) else CameraControlBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = if (uiState.isHdrVideoEnabled) {
                        androidx.compose.foundation.BorderStroke(1.5.dp, CameraYellowAccent)
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HdrOn,
                                    contentDescription = null,
                                    tint = if (uiState.isHdrVideoEnabled) CameraYellowAccent else if (uiState.isHdrSupported) CameraTextPrimary else CameraTextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.video_hdr_label),
                                        color = if (uiState.isHdrVideoEnabled) CameraYellowAccent else CameraTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (uiState.isHdrSupported) {
                                            if (uiState.isHdrVideoEnabled) stringResource(R.string.video_hdr_desc) else stringResource(R.string.video_sdr_desc)
                                        } else {
                                            "No disponible en este sensor"
                                        },
                                        color = CameraTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (uiState.isHdrSupported) {
                                Switch(
                                    checked = uiState.isHdrVideoEnabled,
                                    onCheckedChange = { onToggleHdr() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CameraBlack,
                                        checkedTrackColor = CameraYellowAccent,
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.testTag("hdr_video_toggle_switch")
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "No Soportado",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (!uiState.isHdrSupported) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "El sensor físico o la capa de personalización de $manufacturerName no exponen captura en 10 bits (HLG/HDR10) a través de Camera2. El vídeo se graba en SDR 8-bit estándar.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sección 4: Conmutador de Grabación de Audio
                Card(
                    colors = CardDefaults.cardColors(containerColor = CameraControlBackground),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = null,
                                tint = if (uiState.isAudioEnabled) CameraYellowAccent else CameraTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.video_audio_label),
                                    color = CameraTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (uiState.isAudioEnabled) "Audio activado" else "Vídeo silencioso",
                                    color = CameraTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = uiState.isAudioEnabled,
                            onCheckedChange = { onToggleAudio() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CameraBlack,
                                checkedTrackColor = CameraYellowAccent,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("audio_toggle_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
