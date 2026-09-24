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

### 🟢 Fase 3.5: Megapíxeles Reales del Sensor y Detección de Estabilidad *(Completada)*
- [x] **Detección de Megapíxeles Físicos del Sensor:**
  - Cálculo dinámico de la resolución física total del sensor fotográfico (48 MP, 50 MP, 64 MP, 108 MP, 12 MP) mediante `CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP`.
  - Conmutador directo en la barra superior (`[50MP OFF] / [50MP]`) adaptado a los MP reales del dispositivo.
  - Configuración de `ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY` y `ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY` para capturas de resolución ultra alta.
- [x] **Detección de Estabilidad de la Mano con Acelerómetro:**
  - Clase `DeviceStabilityManager` que mide micro-movimientos para verificar si el usuario mantiene el teléfono quieto.
  - Banner reactivo en el visor ("Mantén el dispositivo quieto…") para evitar fotos movidas o trepidadas.
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
- [ ] **Modo Pro / Manual Adicional:**
  - Bloqueo de enfoque y exposición (AE/AF Lock).
  - Ajuste manual de Balance de Blancos (Luz día, Nublado, Incandescente, Fluorescente).
- [ ] **Temporizador de Disparo:**
  - Opciones de cuenta regresiva: Desactivado, 3 segundos, 10 segundos, con señal visual guía.
- [ ] **Disparo en Ráfaga (Burst Mode):**
  - Mantener pulsado el obturador para capturar secuencias rápidas de fotos.
- [ ] **Nivel de Horizonte Virtual:**
  - Uso del acelerómetro del dispositivo para indicar si el encuadre está perfectamente nivelado.

---

### 🟠 Fase 5: Fotografía Computacional en C++20 y Aceleración de Hardware
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
