package com.lg.monkeymusicplayer.core.feature

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FeatureGate — fuente de verdad sobre qué features están desbloqueadas.
 *
 * ## Arquitectura
 * Actualmente usa [SharedPreferences] como backend (modo desarrollo/debug).
 * Cuando se integre Google Billing, este será el único punto a modificar:
 * reemplazar [prefs] por las entitlements del BillingClient.
 *
 * ## Modos
 * - **FREE:** Solo las features con [Feature.freeDefault] = true están activas.
 * - **PRO (unlocked):** Todas las features están activas.
 * - **Override por feature:** Permite desbloquear/bloquear features individuales
 *   (útil para tests A/B o betas de features específicas).
 *
 * ## Uso típico en Composable
 * ```kotlin
 * val gate: FeatureGate = hiltViewModel<SettingsViewModel>().featureGate
 * // o bien inyectado directamente si el Composable tiene acceso al DI:
 * if (gate.isUnlocked(Feature.VISUALIZER)) { ... }
 * ```
 *
 * ## Uso en ViewModel
 * ```kotlin
 * class MyViewModel @Inject constructor(val featureGate: FeatureGate) : ViewModel() {
 *     fun onCrossfadeEnabled() {
 *         if (!featureGate.isUnlocked(Feature.CROSSFADE)) {
 *             // emitir evento de paywall
 *             return
 *         }
 *         // lógica real
 *     }
 * }
 * ```
 */
@Singleton
class FeatureGate @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "monkey_feature_gate"
        private const val KEY_PRO_UNLOCKED = "pro_unlocked"
        private const val KEY_PREFIX = "feature_"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * True si la feature está disponible para el usuario actual.
     *
     * Orden de evaluación:
     * 1. Si PRO está desbloqueado → true para todas.
     * 2. Si la feature tiene override individual → usa ese valor.
     * 3. Si la feature es gratuita por defecto → true.
     * 4. En cualquier otro caso → false (bloqueada).
     */
    fun isUnlocked(feature: Feature): Boolean {
        if (isProUnlocked()) return true
        val override = prefs.getString("$KEY_PREFIX${feature.key}", null)
        if (override != null) return override == "true"
        return feature.freeDefault
    }

    /** True si el usuario tiene PRO (todas las features desbloqueadas). */
    fun isProUnlocked(): Boolean = prefs.getBoolean(KEY_PRO_UNLOCKED, false)

    /**
     * Desbloquea PRO completo. Llamar desde el callback de Google Billing
     * cuando la compra se confirma:
     * ```kotlin
     * featureGate.unlockPro()
     * ```
     */
    fun unlockPro() {
        prefs.edit { putBoolean(KEY_PRO_UNLOCKED, true) }
    }

    /** Revoca PRO (ej. reembolso o expiración de suscripción). */
    fun revokePro() {
        prefs.edit { putBoolean(KEY_PRO_UNLOCKED, false) }
    }

    /**
     * Override manual de una feature individual.
     * Útil para flags de beta, tests A/B, o desbloqueos parciales.
     */
    fun setFeatureOverride(feature: Feature, unlocked: Boolean) {
        prefs.edit { putString("$KEY_PREFIX${feature.key}", unlocked.toString()) }
    }

    /** Elimina el override de una feature, volviendo al comportamiento por defecto. */
    fun clearFeatureOverride(feature: Feature) {
        prefs.edit { remove("$KEY_PREFIX${feature.key}") }
    }

    /**
     * Lista de features premium bloqueadas para el usuario actual.
     * Útil para mostrar el paywall con las features disponibles al comprar PRO.
     */
    fun lockedPremiumFeatures(): List<Feature> =
        Feature.entries.filter { !it.freeDefault && !isUnlocked(it) }

    /**
     * DEBUG ONLY: desbloquea todo temporalmente (no persiste en producción).
     * Usar solo en builds de debug para testing.
     */
    fun debugUnlockAll() {
        prefs.edit {
            Feature.entries.forEach { putString("$KEY_PREFIX${it.key}", "true") }
        }
    }
}
