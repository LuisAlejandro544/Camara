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
* **`CameraState.kt`**: Modelos de datos inmutables (`CaptureMode`, `FlashMode`, `LensFacing`, `AspectRatioOption` con Full/16:9/4:3/1:1, `TimerOption` con OFF/3s/5s/10s, `ColorProfileOption`, `VideoQualityOption`, `DynamicRangeOption`, `VideoCapabilitiesInfo`, `PhotoResolutionInfo` con matriz física de 50MP y flags de restricción OEM/OS, `GraphicsFilterBackend` con Vulkan 1.1 y OpenGL ES 3.2, `CameraUiState` con estado de temporizador activo, aspecto de cámara, Vulkan/OpenGL, filtro de belleza e intensidad, diálogo explicativo y controles de exposición).
* **`CameraViewModel.kt`**: Gestión de estado reactivo, selector y cuenta regresiva de temporizador de disparo (`setTimerOption`, `toggleTimerOption`, `startCountdown`, `cancelCountdown`), selector de relación de aspecto (`setAspectRatio`, `toggleAspectRatioSelector`), conmutación de modo foto/vídeo, selector de resolución nativa de 50MP, detección de Vulkan 1.1 y OpenGL ES en procesador (`detectGraphicsHardware`), selector de backend de renderizado, alternancia de filtro de belleza y ajuste de intensidad, control de diálogo explicativo, selección de resolución/FPS de vídeo, alternancia de HDR 10-bit HLG, calibración de color antilavado, compensación de exposición EV y corrutinas en `Dispatchers.IO`.
* **`CameraCaptureManager.kt`**: Captura fotográfica con CameraX, encuadre y recorte central a la relación de aspecto exacta (`cropBitmapToAspectRatio`), detección de soporte de Vulkan 1.1 (`detectVulkan11Support`) y OpenGL ES (`detectOpenGlVersion`), procesamiento nativo en C++20 con `NativeCameraEngine.applyBeautyFilter` en hilo secundario IO, inspección de matriz física de hardware (`SENSOR_INFO_PIXEL_ARRAY_SIZE`), mapas de máxima resolución (`SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION`) y diagnóstico de restricciones de Android y capas OEM.
* **`DeviceStabilityManager.kt`**: Monitorización en tiempo real del acelerómetro de hardware para detectar quietud/micro-movimientos de la mano durante disparos de alta resolución.
* **`CameraVideoManager.kt`**: Detección de resoluciones (4K UHD, 2K QHD, FHD, HD), FPS y soporte de HDR (10-bit HLG) del sensor físico vía `querySupportedDynamicRanges`, control de sesión de grabación y guardado en `MediaStore` (`Movies/Camara`).
* **`VideoSettingsSheet.kt`**: Diálogo modal independiente para elegir resolución (4K, 2K QHD, FHD, HD, SD), tasa de cuadros (FPS), conmutar audio y sección permanente de modo HDR 10-bit con estado activo o diagnóstico si la capa OEM no lo soporta.
* **`SettingsScreen.kt`**: Menú independiente de Configuración General accesible mediante el icono de tuerca superior. Proporciona resumen de capacidades de hardware (50MP y restricciones), selector de Relación de Aspecto (Full, 16:9, 4:3, 1:1), tarjeta dedicada de **Motor Gráfico de Filtros** con detección de Vulkan 1.1 / OpenGL ES y selector de backend, control deslizante de intensidad del Filtro de Belleza (20% al 100%), tarjeta educativa sobre Pixel Binning y HDR, estado del motor C++20 y acceso a calibración de color.
* **`ColorCalibrationScreen.kt`**: Pantalla modular de Calibración de Color y Contraste diseñada para eliminar el efecto de fotos pasteles o lavadas. Permite activar el Modo Antilavado Automático (-0.7 EV), calibrar compensación EV manual continua y seleccionar perfiles de color (Vívido, Alto Contraste, Cálido Natural, Estándar).
* **`CameraPreviewView.kt`**: Visor reactivo con soporte para `ImageCapture` y `VideoCapture`, `ResolutionSelector` con `AspectRatioStrategy` (16:9, 4:3), máscara visual de encuadre en tiempo real para 1:1, enfoque táctil, zoom sin límites artificiales, cuadrícula y compensación EV por hardware en tiempo real.
* **`CameraScreen.kt`**: Interfaz principal del visor con botonera superior compacta y elegante, botón interactivo de Relación de Aspecto (`[Full]`, `[16:9]`, `[4:3]`, `[1:1]`) con barra desplegable rápida, botón directo de Filtro de Belleza (`[Belleza / Belleza OFF]`), botón directo de HDR para vídeo, badge interactivo de 50MP con diálogo explicativo, obturador adaptativo y cronómetro animado.
* **`PhotoPreviewScreen.kt`**: Visualizador de capturas a pantalla completa con zoom, compartir y eliminar.
* **`CameraPermissionScreen.kt`**: Solicitud amigable del permiso de cámara.
* **`nativeengine/NativeCameraEngine.kt`**: Puente Kotlin con `System.loadLibrary("camera_engine")` para acceder a las funciones C++20 (`getEngineVersion`, `isNativeEngineAvailable`, `applyBeautyFilter`).

#### 5. Paquete de Tema y Estilos (`com.example.ui.theme`)
* **`Color.kt`**: Paleta oscura de alto contraste estilo visor profesional.
* **`Theme.kt`**: Tema Material 3 oscuro con **escala de fuente fija (`fontScale = 1.0f`)** para evitar deformaciones.
* **`Type.kt`**: Jerarquía tipográfica.
