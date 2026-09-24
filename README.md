# Cámara — Aplicación de Fotografía y Grabación de Vídeo para Android

Aplicación nativa de cámara para Android diseñada con **Jetpack Compose**, **CameraX (Fotos y Vídeo)** y un motor nativo en **C++20 con CMake**, optimizada para un rendimiento fluido, respuesta táctil inmediata y distribución directa en tiendas de APKs de terceros como **Uptodown**.

---

## 📸 Descripción del Proyecto

**Cámara** ofrece una experiencia fotográfica y audiovisual inmersiva y moderna. Diseñada con una interfaz oscura tipo visor profesional, la aplicación prescinde de configuraciones innecesarias o dependencias de variables de entorno (`.env`), ofreciendo una compilación limpia e independiente lista para ejecutarse en cualquier dispositivo Android moderno (Android 8.0 Oreo hasta Android 16).

Además, cuenta con una base nativa en **C++20** compilada mediante **CMake** integrada en el APK final, preparando el terreno para el procesamiento de imágenes y computación de alto rendimiento a nivel de hardware.

### Características Principales

* **Visor Inmersivo en Vivo:** Integración nativa con CameraX `Preview`, `ImageCapture` y `VideoCapture<Recorder>` de latencia reducida.
* **Selector de Relación de Aspecto (Full, 16:9, 4:3, 1:1):** Selector rápido en la barra superior y en Ajustes que permite componer el encuadre exacto antes de disparar. Soporta modo **Full** (pantalla completa del dispositivo), **16:9** (panorámico estándar para historias y estados), **4:3** (sensor nativo al 100% sin recorte de megapíxeles) y **1:1** (cuadrado con máscara visual en vivo para perfiles y redes sociales), con procesamiento central en `Dispatchers.IO`.
* **Modo Megapíxeles Reales (50 MP / Ultra High-Res) y Diagnóstico de Sensor:** Detección de la matriz física real del sensor (`SENSOR_INFO_PIXEL_ARRAY_SIZE`) y mapas de máxima resolución (`SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION`). Al activarlo, configura `ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE` y `CAPTURE_MODE_MAXIMIZE_QUALITY`.
* **Detección Automática de Vulkan 1.1 y OpenGL ES en Procesador:** Inspección en tiempo real de capacidades gráficas del procesador y GPU (`FEATURE_VULKAN_HARDWARE_VERSION` >= 1.1). Si el procesador soporta Vulkan 1.1, la app lo activa automáticamente para aceleración por GPU de alto rendimiento; si no, conmuta sin problemas a OpenGL ES 3.2.
* **Filtro de Belleza y Suavizado en C++20 (Anti-Lavado):** Algoritmo bilateral nativo con detección cromática de tonos de piel humana en YCbCr y mapeo tonal preservador de contraste. Suaviza imperfecciones y poros sin efecto "fantasma", neblina blanca o colores lavados, manteniendo ojos, pestañas, labios y negros profundos 100% nítidos. Compatible y optimizado para ejecutarse tanto en Vulkan 1.1 como en OpenGL ES 3.2.
* **Control de Filtros e Intensidad en Ajustes y Barra Superior:** Botón de acceso directo (`[Belleza / Belleza OFF]`) en la barra superior en modo foto y sección dedicada en Ajustes para conmutar manualmente entre Vulkan 1.1 y OpenGL ES 3.2, y calibrar la intensidad del suavizado (20% al 100%).
* **Transparencia Técnica sobre Pixel Binning y Capas OEM:** Diálogo explicativo integrado y tarjeta en Ajustes que explica por qué Android < 12 entrega un flujo binned de 12 MP (4 en 1) y cómo capas de fabricantes (Xiaomi, Tecno, Infinix, Samsung, etc.) a veces restringen los 50 MP directos para apps de terceros.
* **Detección de Estabilidad y Aviso de Sujeción:** Monitorización en tiempo real mediante el acelerómetro del dispositivo (`DeviceStabilityManager`) con banner visual ("Mantén el dispositivo quieto…") para garantizar capturas nítidas sin trepidación durante disparos de alta resolución.
* **Grabación de Vídeo de Alto Rendimiento:** Soporte completo para captura de vídeo con guardado en `Movies/Camara` en formato MP4 vía `MediaStore`.
* **Control Rápido de Vídeo HDR en 10 bits (HLG):** Botón directo de HDR en la barra superior en modo vídeo y sección siempre visible en los ajustes con explicación detallada sobre soporte de hardware e incompatibilidades de capas OEM.
* **Iconos Compactos, Elegantes y Ergonómicos:** Rediseño de la botonera superior con círculos translúcidos compactos de 38 dp y áreas táctiles de 48 dp, optimizando el espacio visual de la pantalla para nuevos controles.
* **Detección Automática de Hardware (Resolución y FPS):** Inspección dinámica del sensor físico del teléfono para ofrecer las resoluciones reales soportadas (4K UHD, 2K • QHD 2560×1440, 1080p Full HD, 720p HD, 480p SD) y rangos de cuadros por segundo (30 FPS, 60 FPS).
* **Ajustes de Vídeo Modulares:** Diálogo independiente para calibrar resolución (4K, 2K QHD, FHD, HD), FPS, rango dinámico (HDR 10-bit HLG si está disponible) y activación de micrófono sin saturar la pantalla principal.
* **Menú Independiente de Configuración (Acceso por Tuerca):** Pantalla dedicada de configuración (`SettingsScreen`) accesible directamente mediante el icono de engranaje superior, con resumen de hardware del sensor, calibración y estado del motor nativo C++20.
* **Calibración de Color y Contraste (Anti-Colores Lavados/Pasteles):** Pantalla especializada (`ColorCalibrationScreen`) para solucionar la sobreexposición y aplanamiento de sombras común en sensores OEM. Permite aplicar **Modo Antilavado Automático (-0.7 EV)**, compensación manual de exposición EV continua y perfiles de color (*Vívido Antilavado*, *Alto Contraste*, *Cálido Natural*, *Estándar*), reflejados de inmediato en el visor con distintivo visual en vivo.
* **Cronómetro y Alerta de Grabación:** Indicador visual superior con cronómetro animado (`REC 00:00`) y punto rojo pulsante.
* **Motor Nativo en C++20:** Integración oficial de **CMake** y **Android NDK** con soporte multiplataforma para arquitecturas de 64 bits (`arm64-v8a`, `x86_64`) y 32 bits (`armeabi-v7a`).
* **Enfoque Táctil (Tap-to-Focus):** Toque interactivo en cualquier área de la pantalla con anillo visual animado para enfoque y medición de luz.
* **Zoom Sin Límites Artificiales (Hasta el Máximo del Sensor):** Desbloqueo del rango total de aumento que soporte el dispositivo (10x, 20x, 30x o más) sin límites arbitrarios fijados por software, complementado con un selector rápido dinámico (1x, 2x, 5x, 10x) y gesto fluido de pellizco (Pinch-to-Zoom).
* **Controles Rápidos de Flash:** Alternancia instantánea entre Automático (`Auto`), Encendido (`On`) y Apagado (`Off`).
* **Regla de Tercios:** Cuadrícula de composición 3x3 conmutable tanto para foto como para vídeo.
* **Alternancia de Lentes:** Cambio entre cámara trasera y frontal con animación de giro suave.
* **Obturador Adaptativo Fiel:** Disparador dinámico con respuesta elástica (círculo blanco para foto, círculo rojo para vídeo y cuadrado rojo pulsante para detener grabación).
* **Visor y Gestión de Multimedia:** Miniatura con acceso directo a la última captura, vista previa a pantalla completa con zoom, opción para compartir y eliminación segura.
* **Tipografía Fija Antideformación:** Escala de fuente bloqueada (`fontScale = 1.0f`) para garantizar que la configuración de accesibilidad del teléfono no descuadre los controles del visor.

---

## ⚙️ Especificaciones Técnicas

| Parámetro | Detalle |
| :--- | :--- |
| **Lenguajes** | Kotlin 2.2.x y C++ (Estándar C++20) |
| **Framework de Interfaz** | Jetpack Compose (Material Design 3) |
| **Motor de Cámara** | AndroidX CameraX 1.5.0 (`camera2`, `core`, `lifecycle`, `view`, `video`) |
| **Sistema de Compilación Nativo** | CMake 3.22.1 + Android NDK |
| **Arquitecturas Nativas (ABIs)** | `arm64-v8a` (64 bits), `armeabi-v7a` (32 bits), `x86_64` |
| **Carga de Imágenes** | Coil 2.7.0 |
| **Versión Mínima de Android** | **Android 8.0 Oreo (API 26)** |
| **Versión Objetivo (Target)** | **Android 16 (API 36)** |
| **Gestión de Hilos** | Kotlin Coroutines (`Dispatchers.IO` para I/O) y Flow |
| **Compilación sin .env** | Independiente de archivos de variables de entorno |
| **CI/CD Integrado** | GitHub Action con compilación manual, caché opcional y generación de firma desde cero |
| **Distribución** | APK optimizado para Uptodown y tiendas de terceros |

---

## 📂 Estructura del Código Fuente

```text
app/src/main/
├── cpp/
│   ├── CMakeLists.txt               # Configuración de compilación CMake (C++20, NDK)
│   └── native-camera-engine.cpp     # Motor nativo de procesamiento e interfaz JNI
├── java/com/example/
│   ├── MainActivity.kt              # Punto de entrada, permisos (Cámara/Audio) y navegación
│   ├── camera/
│   │   ├── CameraState.kt           # Modelos de datos inmutables, resoluciones y estados
│   │   ├── CameraViewModel.kt       # Lógica de negocio y corrutinas en segundo plano
│   │   ├── CameraCaptureManager.kt  # Canal de captura de fotos en MediaStore (Pictures)
│   │   ├── CameraVideoManager.kt    # Gestor de grabación y detección de hardware (Movies)
│   │   ├── VideoSettingsSheet.kt    # Diálogo de ajustes de resolución y FPS del sensor
│   │   ├── CameraPreviewView.kt     # Visor nativo reactivo con CameraX (Preview, ImageCapture, VideoCapture)
│   │   ├── CameraScreen.kt          # Pantalla principal con controles superior, inferior y selector de modo
│   │   ├── SettingsScreen.kt        # Menú independiente de configuración general (acceso por tuerca)
│   │   ├── ColorCalibrationScreen.kt# Menú de calibración de color, antilavado y compensación EV
│   │   ├── PhotoPreviewScreen.kt    # Visor de capturas a pantalla completa, compartir y eliminar
│   │   ├── CameraPermissionScreen.kt# Pantalla amigable para solicitud de permisos
│   │   └── nativeengine/
│   │       └── NativeCameraEngine.kt# Puente JNI que enlaza la librería C++ (.so)
│   └── ui/theme/
│       ├── Color.kt                 # Paleta oscura de alto contraste para visores de cámara
│       ├── Theme.kt                 # Tema Material 3 con densidad de fuente fija (fontScale = 1.0f)
│       └── Type.kt                  # Jerarquía tipográfica
scripts/
└── generate-debug-keystore.sh       # Generador de almacén de claves (debug.keystore) desde cero
.github/workflows/
└── build-debug-apk.yml              # GitHub Action para compilar APK Debug manualmente
```

---

## 🚀 Compilación y Ejecución

La aplicación no requiere archivos `.env`, tokens ni claves de API para compilar o ejecutarse.

### Requisitos Previos
* Android SDK con plataforma API 36 y NDK instalados.
* CMake 3.22.1+.
* JDK 17 o 21 configurado.
* Gradle con soporte para Kotlin DSL.

### Comandos de Compilación
```bash
# Generar o regenerar almacén de claves debug desde cero
bash scripts/generate-debug-keystore.sh

# Compilar el APK en modo depuración (incluyendo librerías .so nativas)
gradle :app:assembleDebug

# Ejecutar las pruebas unitarias y de Robolectric
gradle :app:testDebugUnitTest
```

El APK resultante se genera en:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🤖 GitHub Action (Compilación Automatizada en la Nube)

El proyecto incluye un flujo de trabajo de **GitHub Actions** en `.github/workflows/build-debug-apk.yml`:
* **Activación Manual (`workflow_dispatch`):** Se ejecuta solo cuando tú lo solicitas desde la pestaña *Actions* de GitHub.
* **Descarga de Dependencias C++:** Descarga e instala automáticamente CMake 3.22.1 y el Android NDK.
* **Caché Opcional:** Permite activar o desactivar la caché de Gradle mediante un selector antes de lanzar el build.
* **Generación de Firma desde Cero:** Ejecuta `scripts/generate-debug-keystore.sh` para forzar una firma debug limpia y nueva sin depender de archivos previos.
* **Artefacto Descargable:** Entrega el archivo `app-debug.apk` listo para descargar e instalar en el teléfono.

---

## 📜 Licencia
Proyecto libre de dependencias con licencias restrictivas (GPL/copyleft), apto para distribución binaria independiente.
