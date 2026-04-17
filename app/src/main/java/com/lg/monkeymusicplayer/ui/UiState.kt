package com.lg.monkeymusicplayer.ui

/**
 * Estado UI genérico para pantallas que cargan datos asincrónicamente.
 *
 * Reemplaza el patrón ad-hoc de múltiples booleanos sueltos (isLoading, error, data).
 * Debe usarse como tipo del StateFlow expuesto por cada ViewModel a su pantalla.
 *
 * Uso típico en ViewModel:
 * ```
 * private val _statsState = MutableStateFlow<UiState<UserStats>>(UiState.Loading)
 * val statsState: StateFlow<UiState<UserStats>> = _statsState.asStateFlow()
 *
 * viewModelScope.launch {
 *     getUserStatsUseCase().collect { stats ->
 *         _statsState.value = UiState.Success(stats)
 *     }
 * }
 * ```
 *
 * Uso típico en Composable:
 * ```
 * when (val state = statsState) {
 *     is UiState.Loading -> LoadingIndicator()
 *     is UiState.Success -> StatsContent(state.data)
 *     is UiState.Error   -> ErrorMessage(state.message)
 * }
 * ```
 */
sealed class UiState<out T> {

    /** Carga inicial en progreso — no hay datos disponibles aún. */
    data object Loading : UiState<Nothing>()

    /**
     * Datos disponibles y válidos.
     * @param data El resultado exitoso de tipo [T].
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * Operación fallida — no hay datos válidos.
     * @param message Descripción del error para mostrar al usuario.
     * @param cause   Excepción original, útil para logging (opcional).
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : UiState<Nothing>()
}

// ── Extension functions ───────────────────────────────────────────────────────

fun <T> UiState<T>.onSuccess(action: (T) -> Unit): UiState<T> {
    if (this is UiState.Success) action(data)
    return this
}

fun <T> UiState<T>.onError(action: (String) -> Unit): UiState<T> {
    if (this is UiState.Error) action(message)
    return this
}

fun <T> UiState<T>.onLoading(action: () -> Unit): UiState<T> {
    if (this is UiState.Loading) action()
    return this
}

/** Devuelve los datos si están disponibles, o null en cualquier otro estado. */
fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data

/** True si el estado contiene datos válidos. */
val <T> UiState<T>.isSuccess: Boolean get() = this is UiState.Success

/** True si está en proceso de carga. */
val <T> UiState<T>.isLoading: Boolean get() = this is UiState.Loading
