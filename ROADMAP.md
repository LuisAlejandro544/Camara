# Hoja de Ruta (Roadmap) — Aplicación de Cámara

Este documento establece el plan de evolución técnico y funcional para convertir la aplicación de cámara en una herramienta fotográfica y audiovisual de nivel profesional para Android, combinando Jetpack Compose con un motor nativo en C++20.

---

## 🗺️ Fases de Desarrollo

```
Fase 1 (MVP Estable) ──> Fase 2 (Motor C++ Integrado) ──> Fase 3 (Grabación de Vídeo & HW) ──> Fase 4 (Controles Pro) ──> Fase 5 (Fotografía C++) ──> Fase 6 (Distribución)
```

---

### 🟢 Fase 1: Base Estable y Visor Moderno *(Completada)*
- [x] Arquitectura MVVM desacoplada en Kotlin y Jetpack Compose.
- [x] Integración con CameraX 1.5.0 (`Preview` y `ImageCapture`).
- [x] Configuración de versión mínima en **Android 8.0 Oreo (API 26)**.
- [x] Eliminación completa de dependencia en archivos `.env`.
- [x] Bloqueo de escala tipográfica (`fontScale = 1.0f`) para prevenir descuadres de interfaz.
- [x] Enfoque táctil interactivo (Tap to Focus) con animación de anillo.
- [x] Zoom táctil mediante gesto de pellizco (Pinch to Zoom).
- [x] Controles de flash (Auto / On / Off) y conmutador de lentes (Trasera / Frontal).
- [x] Botón de obturador animado con destello visual blanco de pantalla.
- [x] Visor de fotos a pantalla completa con acciones de compartir y eliminar.
- [x] Guardado en `MediaStore` en el directorio `Pictures/Camara`.

---

### 🟢 Fase 2: Integración del Motor Nativo C++20 con CMake *(Completada)*
- [x] **Configuración de CMake y Android NDK:**
  - Script `CMakeLists.txt` con estándar **C++20** activado.
  - Bloque `externalNativeBuild` en `app/build.gradle.kts`.
- [x] **Soporte de Arquitecturas Multiplataforma (ABIs):**
  - Compilación automática para `arm64-v8a` (64 bits), `armeabi-v7a` (32 bits) y `x86_64`.
- [x] **Puente JNI y Carga de Biblioteca (.so):**
  - Implementación de `native-camera-engine.cpp` y wrapper `NativeCameraEngine.kt`.
  - Empaquetado directo de `libcamera_engine.so` dentro del APK final.

---

### 🟢 Fase 3: Grabación de Vídeo y Detección de Hardware *(Completada)*
- [x] **Canal de Grabación con CameraX Video:**
  - Incorporación de `androidx.camera:camera-video:1.5.0` (`VideoCapture<Recorder>`).
  - Guardado en `Movies/Camara` con formato MP4 y manejo seguro de estados `IS_PENDING`.
- [x] **Detección Dinámica de Capacidades del Sensor:**
  - Inspección de resoluciones admitidas por el hardware (4K UHD, 2K • QHD 2560×1440, 1080p Full HD, 720p HD, 480p SD).
  - Inspección de tasas de cuadros por segundo (FPS) reales mediante `Camera2CameraInfo` (30 FPS, 60 FPS).
  - Inspección de soporte de Alto Rango Dinámico (HDR 10 bits HLG/HDR10) mediante `CameraInfo.querySupportedDynamicRanges`.
- [x] **Soporte de Grabación de Vídeo en HDR (10-bit HLG):**
  - Grabación con más de 1.000 millones de colores y alto contraste dinámico en sensores compatibles con codificación de 10 bits.
  - Ocultación inteligente del interruptor si el sensor no lo soporta para no saturar al usuario.
  - Indicador visual `• HDR` en el distintivo superior de vídeo cuando está activado.
- [x] **Interfaz de Grabación Adaptativa:**
  - Selector de modo FOTO / VÍDEO.
  - Cronómetro de grabación animado con punto rojo pulsante (`REC 00:00`).
  - Obturador con transición a botón de parada cuadrado.
  - Diálogo modular e independiente de ajustes de vídeo (`VideoSettingsSheet`).
- [x] **Flujo de Permiso de Audio:**
  - Solicitud de `RECORD_AUDIO` integrada con opción de grabación silenciosa o con micrófono.

---

### 🟢 Fase 3.5: Megapíxeles Reales del Sensor, Restricciones OEM y Detección de Estabilidad *(Completada)*
- [x] **Detección de Megapíxeles Físicos del Sensor (50MP y Ultra Alta Resolución):**
  - Inspección simultánea de la matriz de silicio física (`SENSOR_INFO_PIXEL_ARRAY_SIZE`), el mapa de resolución máxima (`SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION` en Android 12+) y el mapa estándar (`SCALER_STREAM_CONFIGURATION_MAP`).
  - Detección y diagnóstico de restricciones de Android < 12 (falta de API de ultra alta resolución) y bloqueos de capas OEM (Xiaomi, Tecno, Infinix, Samsung) que reservan el flujo de 50MP para apps de fábrica.
  - Conmutador directo en la barra superior (`[50MP OFF] / [50MP]` o `[50MP (4en1)]`) con diálogo explicativo detallado sobre pixel binning.
  - Configuración de `ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE` y `CAPTURE_MODE_MAXIMIZE_QUALITY` para desbloquear el flujo nativo de alta resolución en CameraX.
- [x] **Detección de Estabilidad de la Mano con Acelerómetro:**
  - Clase `DeviceStabilityManager` que mide micro-movimientos para verificar si el usuario mantiene el teléfono quieto.
  - Banner reactivo en el visor ("Mantén el dispositivo quieto…") para evitar fotos movidas o trepidadas.
- [x] **Soporte y Control Rápido de Vídeo HDR (10-bit HLG):**
  - Botón de alternancia rápida de HDR en la barra superior en modo vídeo.
  - Tarjeta de HDR siempre visible en los ajustes con explicación detallada de disponibilidad en el hardware y la capa OEM.
- [x] **Iconografía Compacta y Elegante:**
  - Rediseño de botones superiores e inferiores con contenedores translúcidos compactos de 38 dp y áreas táctiles de 48 dp para un visor despejado y listo para nuevos controles.
- [x] **Soporte de Vídeo 2K (QHD 2560×1440):**
  - Incorporación de resolución 2K Quad HD junto a 4K UHD en la detección y selector de vídeo.
- [x] **Zoom Completo sin Límites de Hardware:**
  - Supresión de límites artificiales de zoom (5x) para permitir el alcance total que soporte el sensor (10x, 20x o más).
  - Selector rápido de zoom en píldoras dinámicas adaptadas al rango de la cámara (1x, 2x, 5x, 10x y gran angular si existe).

---

### 🟡 Fase 4: Controles Manuales Pro, Ajustes y Calibración *(En Curso)*
- [x] **Menú Independiente de Configuración (Acceso por Tuerca):**
  - Pantalla modular `SettingsScreen` accesible mediante icono de engranaje superior sin saturar el visor.
  - Resumen completo de capacidades de hardware (Megapíxeles reales, resoluciones de vídeo, HDR 10-bit y rangos EV del sensor).
  - Estado del motor nativo C++20 con `NativeCameraEngine.getVersion()`.
- [x] **Sistema de Calibración de Color y Contraste (Anti-Colores Lavados/Pasteles):**
  - Pantalla dedicada `ColorCalibrationScreen` para corregir la sobreexposición y aplanamiento de sombras común de fábrica.
  - Interruptor maestro de **Modo Antilavado Automático** (calibración recomendada -0.7 EV con perfil Vívido).
  - Selector de perfiles de color: *Vívido Antilavado*, *Alto Contraste Cinematográfico*, *Cálido Natural* y *Estándar*.
  - Indicador visual dinámico en el visor con distintivo activo (`Antilavado: -0.7 EV • Vívido`).
- [x] **Control de Compensación de Exposición por Hardware (EV):**
  - Presets rápidos (-1.0 EV, -0.7 EV, -0.3 EV, 0.0 EV, +0.5 EV) y slider continuo.
  - Sincronización directa con el ISP del sensor vía `CameraControl.setExposureCompensationIndex()`.
- [x] **Selector de Relación de Aspecto (Full, 16:9, 4:3, 1:1):**
  - Selector desplegable dinámico en la botonera superior y en Ajustes con opciones `[Full]`, `[16:9]`, `[4:3]` y `[1:1]`.
  - Configuración automática del `ResolutionSelector` en CameraX (`AspectRatioStrategy`).
  - Máscara visual de encuadre en tiempo real sobre el visor para previsualizar la toma exacta.
  - Procesamiento y recorte central automático de la foto capturada en segundo plano (`Dispatchers.IO`) sin congelar la app.
- [x] **Temporizador de Disparo (Hasta 10s):**
  - Opciones de cuenta regresiva: Desactivado (`OFF`), 3 segundos (`3s`), 5 segundos (`5s`) y 10 segundos (`10s`).
  - Botón de acceso directo en la botonera superior con barra desplegable rápida para cambiar en 1 toque.
  - Sección interactiva en `SettingsScreen` con botones independientes para cada opción.
  - Overlay animado central en el visor con número grande en amarillo (`CameraYellowAccent`), conteo regresivo y botón de cancelación inmediata.
  - Cancelación rápida al pulsar nuevamente el botón del obturador o al cambiar a modo vídeo.
  - Gestión fluida mediante corrutinas de Kotlin en `viewModelScope` sin bloquear el hilo principal.
- [ ] **Modo Pro / Manual Adicional:**
  - Bloqueo de enfoque y exposición (AE/AF Lock).
  - Ajuste manual de Balance de Blancos (Luz día, Nublado, Incandescente, Fluorescente).
- [ ] **Disparo en Ráfaga (Burst Mode):**
  - Mantener pulsado el obturador para capturar secuencias rápidas de fotos.
- [ ] **Nivel de Horizonte Virtual:**
  - Uso del acelerómetro del dispositivo para indicar si el encuadre está perfectamente nivelado.

---

### 🟢 Fase 5: Fotografía Computacional en C++20 y Aceleración de Hardware *(Iniciada)*
- [x] **Detección Automática de Vulkan 1.1 y OpenGL ES en Procesador:**
  - Inspección del flag de hardware `PackageManager.FEATURE_VULKAN_HARDWARE_VERSION` para detectar soporte de Vulkan 1.1+ (API >= 0x401000).
  - Selección inteligente por defecto: Vulkan 1.1 si está disponible en el silicio; OpenGL ES 3.2 automático si no es soportado.
- [x] **Selector Manual de Backend de Renderizado en Ajustes:**
  - Sección interactiva en `SettingsScreen` para elegir entre Vulkan 1.1 y OpenGL ES 3.2 con validación de incompatibilidad.
- [x] **Filtro de Belleza y Suavizado Nativo en C++20 (Anti-Lavado):**
  - Implementación con filtrado bilateral guiado y detección suave de piel humana en espacio YCbCr.
  - Mapeo tonal que preserva el contraste y el nivel de negro base: no lava la piel ni genera neblina pastosa.
  - Optimizado y compatible para ejecutarse idénticamente en Vulkan 1.1 y OpenGL ES 3.2.
  - Conmutador directo en la barra superior en modo foto (`[Belleza / Belleza OFF]`) y control de intensidad (slider 20% a 100%) en Ajustes.
- [ ] **Captura y Decodificación de Formato RAW (DNG):**
  - Integración en C++ para procesar datos de sensor sin compresión.
- [ ] **Histograma RGB en Tiempo Real:**
  - Cálculo acelerado de luminancia e histogramas por canal utilizando instrucciones vectoriales SIMD (ARM NEON) en C++20.
- [ ] **Conversión Rápida de Formatos:**
  - Transformación YUV420 a RGB en milisegundos con Zero-Copy mediante `AHardwareBuffer`.
- [ ] **Filtros Analógicos No Destructivos:**
  - Curvas de tonos y simulación de película fotográfica clásica aplicadas en el espacio de color nativo.

---

### 🚀 Fase 6: Optimización de Empaquetado y Distribución CI/CD
- [x] **Flujo CI/CD Automatizado con GitHub Actions:**
  - Workflow de activación manual (`workflow_dispatch`) con compilación de APK Debug.
  - Descarga e instalación automática de dependencias de C++ (CMake 3.22.1 y NDK).
  - Opción de activar/desactivar caché.
  - Script independiente `scripts/generate-debug-keystore.sh` para forzar generación de firma desde cero.
- [ ] **Publicación en Uptodown y Tiendas de APKs:**
  - Generación de APKs universales y divididos por arquitectura para descargas más ligeras.
  - Firma con claves de producción seguras sin telemetría de tiendas propietarias.
