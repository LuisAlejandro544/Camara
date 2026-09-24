package com.example.camera

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CameraBlack
import com.example.ui.theme.CameraControlBackground
import com.example.ui.theme.CameraTextPrimary
import com.example.ui.theme.CameraYellowAccent
import java.util.Locale

/**
 * Pantalla modular de Calibración de Color y Compensación de Exposición.
 * Resuelve el problema de fotos lavadas o pasteles en dispositivos donde el ISP del fabricante
 * sobreexpone y aplana el rango dinámico por defecto.
 */
@Composable
fun ColorCalibrationScreen(
    uiState: CameraUiState,
    onBack: () -> Unit,
    onToggleAntiWashedMode: () -> Unit,
    onSelectColorProfile: (ColorProfileOption) -> Unit,
    onSelectExposureEv: (Float) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CameraBlack)
            .statusBarsPadding()
    ) {
        // Barra Superior
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
                    .testTag("color_calibration_back_button")
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
                    text = stringResource(R.string.color_calibration_title),
                    color = CameraTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.color_calibration_subtitle),
                    color = CameraYellowAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Tarjeta Explicativa: Causa Técnica y Solución
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B232A))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier
                            .size(22.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.color_calibration_explanation),
                        color = Color(0xFFE1F5FE),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // 2. Switch Principal: Modo Antilavado Automático
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (uiState.isAntiWashedModeEnabled) CameraYellowAccent.copy(alpha = 0.6f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isAntiWashedModeEnabled) Color(0xFF242215) else CameraControlBackground
                )
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.isAntiWashedModeEnabled) CameraYellowAccent else Color.White.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (uiState.isAntiWashedModeEnabled) CameraBlack else CameraTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.anti_washed_mode_label),
                                    color = CameraTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.isAntiWashedModeEnabled) "Recomendado activo (-0.7 EV)" else "Desactivado",
                                    color = if (uiState.isAntiWashedModeEnabled) CameraYellowAccent else Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isAntiWashedModeEnabled,
                            onCheckedChange = { onToggleAntiWashedMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CameraYellowAccent,
                                checkedTrackColor = CameraYellowAccent.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("anti_washed_mode_switch")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.anti_washed_mode_desc),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // 3. Perfiles de Color y Tono
            CalibrationSectionHeader(title = stringResource(R.string.color_profile_section_title))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorProfileOption.entries.forEach { profile ->
                    val isSelected = uiState.colorProfile == profile
                    ColorProfileCard(
                        profile = profile,
                        isSelected = isSelected,
                        onClick = { onSelectColorProfile(profile) }
                    )
                }
            }

            // 4. Compensación de Exposición (EV)
            CalibrationSectionHeader(title = stringResource(R.string.exposure_compensation_section_title))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CameraControlBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = CameraYellowAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Valor EV actual",
                                color = CameraTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(
                            color = if (uiState.exposureCompensationEv < -0.1f) CameraYellowAccent else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%+.1f EV", uiState.exposureCompensationEv),
                                color = if (uiState.exposureCompensationEv < -0.1f) CameraBlack else CameraTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.exposure_compensation_desc),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Presets rápidos
                    Text(
                        text = "PRESETS RÁPIDOS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            -1.0f to "-1.0",
                            -0.7f to "-0.7 ★",
                            -0.3f to "-0.3",
                            0.0f to "0.0",
                            0.5f to "+0.5"
                        )
                        presets.forEach { (evValue, label) ->
                            val isChosen = kotlin.math.abs(uiState.exposureCompensationEv - evValue) < 0.15f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isChosen) CameraYellowAccent else Color.White.copy(alpha = 0.08f)
                                    )
                                    .clickable { onSelectExposureEv(evValue) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isChosen) CameraBlack else CameraTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Slider de ajuste continuo
                    val minEv = (uiState.minExposureIndex * uiState.exposureStep).coerceAtLeast(-2.5f)
                    val maxEv = (uiState.maxExposureIndex * uiState.exposureStep).coerceAtMost(2.5f)

                    Slider(
                        value = uiState.exposureCompensationEv.coerceIn(minEv, maxEv),
                        onValueChange = { onSelectExposureEv(it) },
                        valueRange = minEv..maxEv,
                        colors = SliderDefaults.colors(
                            thumbColor = CameraYellowAccent,
                            activeTrackColor = CameraYellowAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("exposure_ev_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f EV (Mín)", minEv),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = String.format(Locale.US, "+%.1f EV (Máx)", maxEv),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 5. Botón de Restablecer a valores óptimos
            Button(
                onClick = onResetDefaults,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = CameraTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reset_calibration_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = CameraYellowAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.reset_defaults_button),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColorProfileCard(
    profile: ColorProfileOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected) CameraYellowAccent else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("color_profile_${profile.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF262419) else CameraControlBackground
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) CameraYellowAccent else Color.White.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = CameraBlack,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.label,
                    color = if (isSelected) CameraYellowAccent else CameraTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = profile.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CalibrationSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = CameraYellowAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}
