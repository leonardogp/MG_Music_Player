package com.lg.monkeymusicplayer.ui

/**
 * Estado de carga de la biblioteca musical.
 *
 * Reemplaza los 4 campos booleanos sueltos (isLoading, isScanning,
 * scanProgress, scanTotal) por un sealed class explícito.
 *
 * Transiciones válidas:
 *   Idle ──► Scanning ──► Idle
 *         └──────────────► Error
 */
sealed class LibraryLoadState {

    /** Estado inicial o tras scan completado sin errores. */
    data object Idle : LibraryLoadState()

    /**
     * Scan en progreso.
     * @param progress  Canciones procesadas hasta ahora.
     * @param total     Total de canciones encontradas (0 si aún no se conoce).
     */
    data class Scanning(
        val progress: Int = 0,
        val total: Int = 0
    ) : LibraryLoadState() {
        val fraction: Float get() = if (total > 0) progress.toFloat() / total else 0f
        val isIndeterminate: Boolean get() = total == 0
    }

    /**
     * El scan terminó con error irrecuperable.
     * @param message  Mensaje para mostrar al usuario.
     */
    data class Error(val message: String) : LibraryLoadState()
}
