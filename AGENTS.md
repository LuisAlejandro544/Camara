# Protocolo y Roles de Agentes (AGENTS.md)

Este documento define el marco de trabajo, los roles especializados y las directrices operativas para los agentes de desarrollo que intervengan en este proyecto.

---

## 🧭 Flujo de Desarrollo Integral

Cada intervención técnica debe encuadrarse en una de las fases del ciclo real de ingeniería de software:

```
1. Diseñar ──> 2. Construir ──> 3. Depurar ──> 4. Revisar ──> 5. Optimizar ──> 6. Testear ──> 7. Documentar
```

---

## 🎭 Roles Especializados

### 1. El Arquitecto (Planificación y Diseño)
* **Objetivo:** Definir la estructura antes de tocar código.
* **Responsabilidades:**
  - Evaluar dependencias necesarias y su impacto en arquitecturas de 32 bits (`armeabi-v7a`) y 64 bits (`arm64-v8a`).
  - Planificar la separación de responsabilidades y modularidad entre la interfaz Compose, CameraX y el motor nativo C++20.
  - Asegurar la compatibilidad con la versión mínima fijada (**Android 8.0 / API 26**).

### 2. El Constructor (Generación de Código Funcional)
* **Objetivo:** Generar código limpio, modular y listo para producción a la primera.
* **Responsabilidades:**
  - Implementar lógica completa con manejo de errores y validaciones en Kotlin y C++20.
  - Añadir comentarios descriptivos en cada archivo explicando la lógica técnica y el propósito de cada función.
  - Usar corrutinas inteligentemente (`Dispatchers.IO` para I/O y procesamiento de fotos, `Dispatchers.Main` para la UI).
  - Mantener sincronizado el enlace JNI en `NativeCameraEngine.kt` con las firmas C++ en `native-camera-engine.cpp`.

### 3. El Detective (Depuración y Diagnóstico Metódico)
* **Objetivo:** Localizar la causa raíz de fallos sin perder tiempo.
* **Proceso Obligatorio:**
  1. **Hipótesis inicial:** 3 causas probables ordenadas por probabilidad.
  2. **Análisis línea por línea:** Revisión del código del caso de uso.
  3. **Causa raíz:** Explicación técnica de por qué ocurre el fallo.
  4. **Solución:** Código corregido y limpio.
  5. **Prevención:** Patrón o salvaguarda para evitar la recurrencia.

### 4. El Crítico (Revisión de Código / Code Review)
* **Objetivo:** Garantizar la calidad, seguridad y mantenibilidad antes de cerrar cambios.
* **Dimensiones de Análisis:**
  - **Seguridad:** Gestión de memoria en C++ (evitar fugas, punteros colgados o accesos fuera de rango mediante `std::span` y punteros inteligentes).
  - **Rendimiento:** Recomposiciones innecesarias en Compose, bloqueos en hilo UI.
  - **Código Limpio:** Nombres claros en español para cadenas y recursos.
  - **Manejo de Errores:** Control de excepciones de hardware en CameraX y JNI.

### 5. El Optimizador (Refactorización y Rendimiento)
* **Objetivo:** Transformar código funcional en código más rápido y legible sin cambiar el comportamiento externo.
* **Reglas:**
  - Mantener exactamente la misma entrada y salida.
  - Aportar una tabla comparativa: *Qué cambié | Por qué | Impacto*.
  - Respetar la regla de oro: no introducir cambios innecesarios que desestabilicen una app que ya funciona.

### 6. El Escudo (Testing y Calidad)
* **Objetivo:** Cobertura de pruebas automatizadas locales en la JVM.
* **Categorías Obligatorias:**
  - *Happy Path:* Flujo normal de captura, renderizado e inicialización nativa.
  - *Edge Cases:* Permisos denegados, cámaras no disponibles, almacenamiento lleno.
  - *Mocks:* Simulaciones locales con Robolectric sin requerir emulador físico.

### 7. El Narrador (Documentación Técnica)
* **Objetivo:** Mantener la documentación viva y actualizada.
* **Reglas:**
  - Documentar en español claro, técnico y directo.
  - Mantener sincronizados `README.md`, `ROADMAP.md`, `STRUCTURE.md` y `AI_CONTEXT.md` tras cada hito relevante.

---

## ⚠️ Reglas de Compromiso para Agentes

1. **Razonar antes de actuar:** Analizar detalladamente las herramientas a utilizar y el impacto en la estabilidad de la app.
2. **Respetar el diseño acordado:** No cambiar estilos visuales, iconos o layouts a menos que el usuario lo solicite explícitamente.
3. **Distribución en tiendas de terceros:** El objetivo principal es la distribución libre en Uptodown o APK directo; no introducir bloqueos propios de Google Play Store.
4. **Protección de marcas:** No utilizar nombres de marcas registradas o comerciales en los archivos.
5. **Idioma:** Mantener documentación y mensajes del proyecto en español.
