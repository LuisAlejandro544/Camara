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
6. **Gestión de Permisos:**
   - La app requiere `CAMERA` y opcionalmente `RECORD_AUDIO` para captura de sonido en grabaciones de vídeo. Ambos deben gestionarse de manera segura ante denegaciones en tiempo de ejecución.
7. **Comentarios Explicativos:**
   - Cada archivo fuente de Kotlin y C++ debe contener comentarios de encabezado y documentación sobre la lógica que resuelve.
8. **Sincronización de Metadatos de Plataforma:**
   - Si se modifica el nombre de la app en `res/values/strings.xml`, se debe actualizar en idéntica forma el campo `name` en `metadata.json`. Nunca elimines `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` de `metadata.json`.
9. **Generación de Claves Debug y CI:**
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
