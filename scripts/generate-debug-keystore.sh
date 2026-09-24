#!/usr/bin/env bash
# ==============================================================================
# Script: generate-debug-keystore.sh
# Propósito: Obliga a generar una firma de desarrollo (debug.keystore) desde cero
#           de forma 100% automatizada, limpia y no interactiva para compilaciones
#           locales o flujos de integración continua (CI) en GitHub Actions.
# ==============================================================================

set -euo pipefail

# Ruta de destino del almacén de claves (por defecto: debug.keystore en la raíz del proyecto)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TARGET_KEYSTORE="${1:-${PROJECT_ROOT}/debug.keystore}"

echo "=========================================================="
echo "🔨 Generador de Clave de Depuración (debug.keystore)"
echo "Destino: ${TARGET_KEYSTORE}"
echo "=========================================================="

# 1. Elimina de inmediato cualquier firma preexistente para obligar a regenerar desde cero
if [ -f "${TARGET_KEYSTORE}" ]; then
    echo "⚠️ Eliminando almacén de claves previo para asegurar firma limpia..."
    rm -f "${TARGET_KEYSTORE}"
fi

# 2. Crea directorio padre si no existe
mkdir -p "$(dirname "${TARGET_KEYSTORE}")"

# 3. Genera la nueva clave RSA 2048-bit estándar para Android Debug sin prompts interactivos
echo "🔑 Generando nuevo par de claves RSA (alias: androiddebugkey)..."
keytool -genkeypair \
    -v \
    -keystore "${TARGET_KEYSTORE}" \
    -storepass "android" \
    -alias "androiddebugkey" \
    -keypass "android" \
    -keyalg "RSA" \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"

echo "=========================================================="
echo "✅ Clave debug generada exitosamente desde cero:"
echo "   Archivo: ${TARGET_KEYSTORE}"
echo "   Alias: androiddebugkey"
echo "   Contraseña: android"
echo "=========================================================="
