package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Esquema de color oscuro optimizado para interfaz de cámara.
 */
private val CameraColorScheme = darkColorScheme(
    primary = CameraYellowAccent,
    onPrimary = Color.Black,
    surface = CameraDarkSurface,
    onSurface = CameraTextPrimary,
    background = CameraBlack,
    onBackground = CameraTextPrimary
)

/**
 * Tema principal de la aplicación de cámara.
 * Proporciona un estilo inmersivo oscuro acorde a las apps de cámara modernas
 * y asegura un tamaño de letra fijo (fontScale = 1.0f) para evitar que la configuración
 * del sistema del usuario altere el diseño, proporciones o superposiciones del visor.
 */
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val currentDensity = LocalDensity.current
    // Forzar fontScale = 1.0f mientras se mantiene la densidad de píxeles nativa del dispositivo
    val fixedFontDensity = Density(
        density = currentDensity.density,
        fontScale = 1.0f
    )

    CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
        MaterialTheme(
            colorScheme = CameraColorScheme,
            typography = Typography,
            content = content
        )
    }
}
