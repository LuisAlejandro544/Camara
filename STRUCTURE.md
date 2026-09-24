# Estructura del Proyecto (STRUCTURE.md)

Este documento detalla la organización de archivos, las responsabilidades de cada componente y el flujo de datos dentro de la aplicación **Cámara**.

---

## 🏛️ Vista General de la Arquitectura

La aplicación adopta el patrón **MVVM (Model - View - ViewModel)** combinado con componentes reactivos de **Jetpack Compose**, el canal de hardware de **CameraX (Fotos y Vídeos)** y un motor nativo en **C++20**:

```
┌────────────────────────────────────────────────────────┐
│                   Capa de Presentación                │
│    MainActivity  ──>  CameraScreen / PhotoPreview      │
│         │                      │                       │
│         ▼                      ▼                       │
│    CameraPermissionScreen   CameraPreviewView          │
│                                └── VideoSettingsSheet │
└──────────────────────────┬─────────────────────────────┘
                           │ Observa StateFlow
                           ▼
┌────────────────────────────────────────────────────────┐
│                   Capa de Estado                       │
│                 CameraViewModel                        │
│         (Gestiona CameraUiState y Corrutinas)          │
└──────────────────────────┬─────────────────────────────┘
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
┌──────────────────────────┐┌────────────────────────────┐
│  Capa Hardware (Kotlin)  ││    Motor Nativo C++20      │
│   CameraCaptureManager   ││    NativeCameraEngine      │
│   CameraVideoManager     ││    libcamera_engine.so     │
│   CameraX (Preview, Rec) ││    CMakeLists.txt (NDK)    │
│   MediaStore (Pic/Movies)││                            │
└──────────────────────────┘└────────────────────────────┘
```

---

## 📁 Desglose de Archivos y Carpetas

### Raíz del Proyecto
| Archivo / Carpeta | Propósito |
| :--- | :--- |
| `build.gradle.kts` | Configuración Gradle de nivel de proyecto y declaración de plugins. |
| `settings.gradle.kts` | Repositorios (`google()`, `mavenCentral()`) y nombre raíz del proyecto ("Cámara"). |
| `gradle/libs.versions.toml` | Catálogo de versiones centralizado para dependencias y plugins de Gradle. |
| `metadata.json` | Metadatos de la plataforma y sincronización con el nombre visible de la app. |
| `gradlew` / `gradlew.bat` | Scripts del Gradle Wrapper para ejecución autónoma en Linux/macOS y Windows. |
| `gradle/wrapper/` | Binario `gradle-wrapper.jar` y configuración de versión de Gradle. |
| `README.md` | Descripción general del proyecto, características y compilación. |
| `ROADMAP.md` | Hoja de ruta técnica con las fases futuras de evolución. |
| `STRUCTURE.md` | Este archivo con la arquitectura y desglose del código. |
| `AI_CONTEXT.md` | Contexto técnico, restricciones y pautas para agentes de IA. |
| `AGENTS.md` | Protocolos de rol y flujos de trabajo para los roles de desarrollo. |
| `scripts/generate-debug-keystore.sh` | Script shell para obligar la generación limpia de `debug.keystore` sin esperas. |
| `.github/workflows/build-debug-apk.yml` | Flujo de GitHub Actions para compilar el APK Debug manualmente en la nube. |

---

### Módulo de Aplicación (`app/`)

#### 1. Motor Nativo C++ (`app/src/main/cpp/`)
* **`CMakeLists.txt`**:
  * Configuración de compilación para CMake 3.22.1+.
  * Estándar `CXX_STANDARD 20` obligatorio.
  * Compilación de la librería compartida `camera_engine` (.so).
  * Enlace con librerías del sistema Android: `log`, `android`, `camera2ndk`, `mediandk`.
* **`native-camera-engine.cpp`**:
  * Implementación del motor en C++20.
  * Funciones JNI para verificación de estado y versión del motor nativo.
  * Base para procesamiento de buffers YUV/RAW y aceleración vectorial.

#### 2. Configuración y Recursos Android
* **`app/build.gradle.kts`**:
  * `applicationId`: `com.aistudio.camera.phsnap`
  * `minSdk`: `26` (Android 8.0 Oreo)
  * `targetSdk`: `36` (Android 16)
  * Bloque `externalNativeBuild.cmake` apuntando a `src/main/cpp/CMakeLists.txt`.
  * Filtros de ABI: `arm64-v8a` (64 bits), `armeabi-v7a` (32 bits) y `x86_64`.
  * Dependencias completas de CameraX (`camera2`, `core`, `lifecycle`, `view`, `video`).
* **`app/src/main/AndroidManifest.xml`**:
  * Permisos declarados: `CAMERA` y `RECORD_AUDIO`. Orientación vertical fija (`portrait`).
* **`app/src/main/res/values/strings.xml`**:
  * Textos y descripciones de accesibilidad en español para foto y vídeo.

#### 3. Código Fuente Principal (`com.example`)
* **`MainActivity.kt`**:
  * Actividad principal única (`Single Activity`).
  * `enableEdgeToEdge()`.
  * Verificación de ciclo de vida para permisos de cámara y audio, y enrutamiento entre pantallas.

#### 4. Paquete de Cámara (`com.example.camera`)
* **`CameraState.kt`**: Modelos de datos inmutables (`CaptureMode`, `FlashMode`, `LensFacing`, `ColorProfileOption` con modos Vívido Antilavado y Alto Contraste, `VideoQualityOption` con 4K/2K QHD/FHD, `DynamicRangeOption`, `VideoCapabilitiesInfo` con flag `isHdrSupported`, `PhotoResolutionInfo`, `CameraUiState` con compensación EV, índices de hardware y flags de pantalla).
* **`CameraViewModel.kt`**: Gestión de estado reactivo, conmutación de modo foto/vídeo, selector de resolución nativa completa (Full MP), control de estabilidad, selección de resolución/FPS de vídeo, alternancia de HDR 10-bit HLG, calibración de color antilavado, compensación de exposición EV y corrutinas en `Dispatchers.IO`.
* **`CameraCaptureManager.kt`**: Captura fotográfica con CameraX y detección de megapíxeles reales del sensor vía `Camera2CameraInfo`, con guardado en `MediaStore` (`Pictures/Camara`).
* **`DeviceStabilityManager.kt`**: Monitorización en tiempo real del acelerómetro de hardware para detectar quietud/micro-movimientos de la mano durante disparos de alta resolución.
* **`CameraVideoManager.kt`**: Detección de resoluciones (4K UHD, 2K QHD, FHD, HD), FPS y soporte de HDR (10-bit HLG) del sensor físico vía `querySupportedDynamicRanges`, control de sesión de grabación y guardado en `MediaStore` (`Movies/Camara`).
* **`VideoSettingsSheet.kt`**: Diálogo modal independiente para elegir resolución (4K, 2K QHD, FHD, HD, SD), tasa de cuadros (FPS), conmutar audio y conmutar modo HDR 10-bit (ocultado automáticamente si el sensor no lo soporta).
* **`SettingsScreen.kt`**: Menú independiente de Configuración General accesible mediante el icono de tuerca superior. Proporciona resumen de capacidades de hardware, interruptores de cuadrícula/estabilidad, estado del motor C++20 y acceso directo a la calibración de color.
* **`ColorCalibrationScreen.kt`**: Pantalla modular de Calibración de Color y Contraste diseñada para eliminar el efecto de fotos pasteles o lavadas. Permite activar el Modo Antilavado Automático (-0.7 EV), calibrar compensación EV manual continua y seleccionar perfiles de color (Vívido, Alto Contraste, Cálido Natural, Estándar).
* **`CameraPreviewView.kt`**: Visor reactivo con soporte simultáneo o conmutable para `ImageCapture` (modo estándar o High-Res con `HIGHEST_AVAILABLE_STRATEGY`) y `VideoCapture` (con configuración de `DynamicRange.HLG_10_BIT` o `DynamicRange.SDR`), con enfoque táctil, rango dinámico de zoom reportado por el hardware, cuadrícula y control de compensación de exposición EV por hardware en tiempo real.
* **`CameraScreen.kt`**: Interfaz principal del visor con controles superiores e inferiores, selector de modo FOTO/VÍDEO, selector rápido de niveles de zoom (1x, 2x, 5x, 10x...), badge con los megapíxeles reales del teléfono, alerta de estabilidad de la mano, distintivo de calibración antilavado activa, botón de tuerca para configuración, obturador elástico adaptativo y cronómetro animado.
* **`PhotoPreviewScreen.kt`**: Visualizador de capturas a pantalla completa con zoom, compartir y eliminar.
* **`CameraPermissionScreen.kt`**: Solicitud amigable del permiso de cámara.
* **`nativeengine/NativeCameraEngine.kt`**: Puente Kotlin con `System.loadLibrary("camera_engine")` para acceder a las funciones C++20.

#### 5. Paquete de Tema y Estilos (`com.example.ui.theme`)
* **`Color.kt`**: Paleta oscura de alto contraste estilo visor profesional.
* **`Theme.kt`**: Tema Material 3 oscuro con **escala de fuente fija (`fontScale = 1.0f`)** para evitar deformaciones.
* **`Type.kt`**: Jerarquía tipográfica.
