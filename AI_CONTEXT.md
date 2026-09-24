# Contexto para Agentes de Inteligencia Artificial (AI_CONTEXT.md)

Este documento contiene las directrices, restricciones operativas y reglas técnicas que cualquier modelo o agente de IA debe cumplir al modificar o extender esta base de código.

---

## 🎯 Propósito del Proyecto

Construir y mantener una **aplicación de cámara y grabación de vídeo moderna, rápida y confiable para Android**, desarrollada con **Kotlin** y **Jetpack Compose**, con **CameraX** (módulos de foto y vídeo) como motor de visor y un **motor nativo en C++20 con CMake** empaquetado directamente en el APK final para procesamiento de alto rendimiento, diseñada para distribución independiente en tiendas de APKs de terceros (como **Uptodown**).

---

## 🔒 Restricciones Operativas Críticas

1. **Sin Archivos `.env` ni Secretos Externos:**
   - La aplicación está diseñada para compilar de manera 100% autónoma. **No** reintroduzcas el plugin `secrets-gradle-plugin` ni obligues al usuario a crear archivos `.env`.
2. **Versión Mínima de Android:**
   - `minSdk` está fijado en **26 (Android 8.0 Oreo)**.
3. **Escala de Fuentes Fija:**
   - En `Theme.kt`, `fontScale` está deliberadamente configurado en `1.0f` mediante `LocalDensity`. Esto previene que la configuración de accesibilidad del teléfono desplace o recorte los iconos y controles del visor de la cámara. **No elimines este proveedor.**
4. **Motor Nativo C++20 Activo:**
   - El proyecto incluye CMake (`app/src/main/cpp/CMakeLists.txt`) y genera `libcamera_engine.so`. **No elimines** el bloque `externalNativeBuild` de `app/build.gradle.kts`.
   - Soporte obligatorio para arquitecturas: `arm64-v8a` (64 bits), `armeabi-v7a` (32 bits) y `x86_64`.
5. **Almacenamiento e I/O en Hilos Secundarios:**
   - Toda operación de lectura, escritura, compresión o eliminación de fotos y vídeos debe ejecutarse dentro de corrutinas en `Dispatchers.IO`. **Nunca** bloquees el hilo principal (`Main thread`).
6. **Gestión de Permisos y Sensores:**
   - La app requiere `CAMERA` y opcionalmente `RECORD_AUDIO` para captura de sonido en grabaciones de vídeo. Ambos deben gestionarse de manera segura ante denegaciones en tiempo de ejecución.
   - El sensor acelerómetro se utiliza a través de `DeviceStabilityManager` para orientar al usuario sobre la quietud del dispositivo durante capturas de alta resolución (High-Res / Full MP).
7. **Resolución, Hardware Real y Zoom del Dispositivo:**
   - No se hardcodean topes de resolución ni de zoom arbitrarios. Se inspecciona físicamente el sensor mediante `CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP` tanto para detectar los megapíxeles reales soportados por el teléfono (ej. 48 MP, 50 MP, 64 MP, 108 MP, 12 MP) como para vídeo (admitiendo 4K UHD y 2K • QHD 2560×1440 si el hardware lo soporta).
   - Para vídeo HDR, se consulta dinámicamente `CameraInfo.querySupportedDynamicRanges`. Si el sensor no soporta 10 bits (HLG/HDR10), la opción permanece oculta en la UI para no hacer sentir limitado al usuario ni generar errores. Si se cambia a un sensor sin soporte, se desactiva de forma segura.
   - El rango de zoom debe consultar directamente el `zoomState` del hardware (`minZoomRatio` y `maxZoomRatio`), permitiendo al usuario alcanzar 10x o el límite máximo real soportado por su dispositivo sin restricciones artificiales impuestas en código.
8. **Sistema de Calibración de Color y Compensación de Exposición (Anti-Colores Lavados):**
   - Para corregir el problema de colores pasteles o lavados provocado por los sesgos de sobreexposición del ISP de fábrica (+0.3/+0.7 EV), la app integra control directo sobre `CameraControl.setExposureCompensationIndex()`.
   - El modo antilavado aplica por defecto un offset calibrado (-0.7 EV) y perfil de color *Vívido*, garantizando negros profundos, cielos saturados y contraste natural tanto en el visor como en capturas finales.
   - Las pantallas de configuración general (`SettingsScreen`) y calibración (`ColorCalibrationScreen`) son modulares e independientes, desacopladas del visor principal para no sobrecargar la vista.
9. **Comentarios Explicativos:**
   - Cada archivo fuente de Kotlin y C++ debe contener comentarios de encabezado y documentación sobre la lógica que resuelve.
10. **Sincronización de Metadatos de Plataforma:**
   - Si se modifica el nombre de la app en `res/values/strings.xml`, se debe actualizar en idéntica forma el campo `name` en `metadata.json`. Nunca elimines `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` de `metadata.json`.
11. **Generación de Claves Debug y CI:**
   - `scripts/generate-debug-keystore.sh` permite regenerar de forma no interactiva `debug.keystore` para flujos de CI (`.github/workflows/build-debug-apk.yml`).

---

## 📦 Stack Tecnológico Actual

* **Android Gradle Plugin (AGP):** 9.1.1
* **Kotlin:** 2.2.10
* **Lenguaje Nativo:** C++20
* **Herramienta de Compilación Nativa:** CMake 3.22.1 + Android NDK r28c
* **Jetpack Compose BOM:** 2024.09.00
* **CameraX:** 1.5.0 (`camera2`, `core`, `lifecycle`, `view`, `video`)
* **Coil:** 2.7.0 (Carga reactiva de miniaturas)
* **Corrutinas:** 1.10.2 (`core`, `android`)
