package com.lg.monkeymusicplayer.core.billing

/**
 * Estados del ciclo de vida de Google Play Billing.
 *
 * La UI observa [BillingManager.billingState] y reacciona:
 * - [Idle]: mostrar botón "Obtener PRO" habilitado.
 * - [Purchasing]: mostrar indicador de carga, deshabilitar botón.
 * - [PurchaseSuccess]: mostrar confirmación, navegar a pantalla PRO.
 * - [Error]: mostrar mensaje de error con opción de reintentar.
 * - [Disconnected]: Play Billing no disponible (sin GMS o sin conexión).
 */
sealed class BillingState {
    data object Disconnected    : BillingState()
    data object Idle            : BillingState()
    data object Purchasing      : BillingState()
    data object PurchaseSuccess : BillingState()
    data class  Error(val message: String) : BillingState()
}
