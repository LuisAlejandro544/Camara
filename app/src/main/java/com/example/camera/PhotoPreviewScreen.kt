package com.example.camera

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.theme.CameraTextPrimary
import com.example.ui.theme.CameraYellowAccent

/**
 * Pantalla para visualizar la foto recién capturada a pantalla completa.
 * Ofrece controles para compartir la imagen con otras aplicaciones o eliminarla.
 */
@Composable
fun PhotoPreviewScreen(
    photoUri: Uri,
    isWatermarkEnabled: Boolean = true,
    isAiModeEnabled: Boolean = true,
    onToggleWatermark: () -> Unit = {},
    onApplyWatermark: (Uri) -> Unit = {},
    onApplyAiEnhancement: (Uri) -> Unit = {},
    onBack: () -> Unit,
    onDeletePhoto: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var watermarkAppliedToCurrent by remember { mutableStateOf(false) }
    var aiAppliedToCurrent by remember { mutableStateOf(false) }

    // Estados para zoom y paneo táctil en la visualización
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CameraBlack)
    ) {
        // Imagen a pantalla completa con zoom interactivo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        offset = if (scale > 1f) {
                            Offset(offset.x + pan.x, offset.y + pan.y)
                        } else {
                            Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Foto capturada",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        }

        // Barra superior con botón de regreso
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(CameraControlBackground)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("close_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.close_preview_desc),
                        tint = CameraTextPrimary
                    )
                }

                Text(
                    text = "Confirmación de Foto • Apex Camera",
                    color = CameraTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Panel inferior de confirmación con apartado de Marca de Agua y acciones
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(CameraControlBackground)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // APARTADO: Mejora Inteligente con IA Pro (Zero-DCE Tone Mapping en C++20)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D22)),
                border = BorderStroke(
                    1.dp,
                    if (aiAppliedToCurrent) Color(0xFF81C784).copy(alpha = 0.6f) else CameraYellowAccent.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
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
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (aiAppliedToCurrent) Color(0xFF81C784) else CameraYellowAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Mejora Inteligente con IA Pro",
                                    color = CameraTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (aiAppliedToCurrent) "Foto mejorada: sombras y rango dinámico restaurados" else "Zero-DCE: rescata sombras y micro-contraste en C++",
                                    color = if (aiAppliedToCurrent) Color(0xFF81C784) else CameraYellowAccent.copy(alpha = 0.85f),
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onApplyAiEnhancement(photoUri)
                                aiAppliedToCurrent = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (aiAppliedToCurrent) Color(0xFF81C784) else CameraYellowAccent
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (aiAppliedToCurrent) Color(0xFF81C784).copy(alpha = 0.6f) else CameraYellowAccent.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("preview_ai_enhance_button")
                        ) {
                            Icon(
                                imageVector = if (aiAppliedToCurrent) Icons.Default.Check else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (aiAppliedToCurrent) "Mejorada" else "Mejora IA",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // APARTADO: Control y Activación de Marca de Agua Apex Camera
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F24)),
                border = BorderStroke(
                    1.dp,
                    if (isWatermarkEnabled) CameraYellowAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
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
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Marca de agua Apex Camera",
                                    color = CameraTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isWatermarkEnabled) "Activa: 'SHOT ON APEX CAMERA'" else "Desactivada para nuevas fotos",
                                    color = if (isWatermarkEnabled) CameraYellowAccent else Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Switch(
                            checked = isWatermarkEnabled,
                            onCheckedChange = { onToggleWatermark() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CameraYellowAccent,
                                checkedTrackColor = CameraYellowAccent.copy(alpha = 0.35f),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("preview_watermark_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distinguir de fotos de la cámara nativa",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = {
                                onApplyWatermark(photoUri)
                                watermarkAppliedToCurrent = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (watermarkAppliedToCurrent) Color(0xFF81C784) else CameraYellowAccent
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (watermarkAppliedToCurrent) Color(0xFF81C784).copy(alpha = 0.5f) else CameraYellowAccent.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("apply_watermark_to_photo_button")
                        ) {
                            Icon(
                                imageVector = if (watermarkAppliedToCurrent) Icons.Default.Check else Icons.Default.Camera,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (watermarkAppliedToCurrent) "Estampada" else "Estampar a esta foto",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Acciones: Compartir y Eliminar foto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compartir foto
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, photoUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Compartir foto")
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("share_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share_photo_desc),
                        tint = CameraTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Eliminar foto
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_photo_desc),
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Diálogo de confirmación para eliminar
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(text = "¿Eliminar foto?", color = CameraTextPrimary) },
                text = { Text(text = "Esta acción eliminará la foto permanentemente de tu dispositivo.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDeletePhoto(photoUri)
                        }
                    ) {
                        Text(text = "Eliminar", color = Color(0xFFFF5252))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(text = "Cancelar", color = CameraTextPrimary)
                    }
                }
            )
        }
    }
}
