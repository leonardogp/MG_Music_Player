package com.lg.monkeymusicplayer.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.lg.monkeymusicplayer.core.feature.FeatureGate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BillingManager — gestiona el ciclo de vida de Google Play Billing.
 *
 * ## Productos configurados
 * - [SKU_PRO_LIFETIME]: compra única que desbloquea todas las features premium.
 *   Registrar este SKU en Google Play Console → Monetización → Productos in-app.
 *
 * ## Flujo de compra
 * ```
 * 1. launchBillingFlow(activity)   → abre el sheet de Google Play
 * 2. onPurchasesUpdated()          → callback cuando el usuario completa/cancela
 * 3. handlePurchase(purchase)      → verifica + acknowledge + desbloquea PRO
 * 4. featureGate.unlockPro()       → persiste el estado
 * ```
 *
 * ## Verificación
 * Actualmente solo verifica el estado local de la compra (PURCHASED).
 * Para producción, añadir verificación server-side del purchaseToken contra
 * la Google Play Developer API para prevenir fraude.
 *
 * ## Uso en ViewModel
 * ```kotlin
 * class SettingsViewModel @Inject constructor(
 *     val billingManager: BillingManager
 * ) : ViewModel() {
 *     fun onUpgradeClick(activity: Activity) = billingManager.launchBillingFlow(activity)
 * }
 * ```
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val featureGate: FeatureGate
) {
    companion object {
        /** SKU del producto PRO (registrar en Play Console con este ID exacto). */
        const val SKU_PRO_LIFETIME = "monkey_music_pro_lifetime"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Estado público ───────────────────────────────────────────────────────

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Disconnected)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _proProductDetails = MutableStateFlow<ProductDetails?>(null)
    /** Detalles del producto PRO (precio formateado, etc.) para mostrar en la UI. */
    val proProductDetails: StateFlow<ProductDetails?> = _proProductDetails.asStateFlow()

    // ── BillingClient ────────────────────────────────────────────────────────

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.d("BillingManager: user canceled")
                _billingState.value = BillingState.Idle
            }
            else -> {
                Timber.w("BillingManager: billing error ${result.responseCode} — ${result.debugMessage}")
                _billingState.value = BillingState.Error(result.debugMessage)
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    /**
     * Conecta al Play Billing Service. Llamar desde [MainActivity.onCreate].
     * Después de conectar, verifica compras existentes y carga detalles del producto.
     */
    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("BillingManager: connected")
                    _billingState.value = BillingState.Idle
                    scope.launch {
                        queryExistingPurchases()
                        queryProductDetails()
                    }
                } else {
                    Timber.w("BillingManager: setup failed ${result.responseCode}")
                    _billingState.value = BillingState.Error(result.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                Timber.d("BillingManager: disconnected")
                _billingState.value = BillingState.Disconnected
            }
        })
    }

    /** Desconecta el cliente. Llamar desde [MainActivity.onDestroy]. */
    fun disconnect() {
        billingClient.endConnection()
    }

    // ── Compra ───────────────────────────────────────────────────────────────

    /**
     * Abre el sheet de Google Play para comprar el plan PRO.
     * Requiere una [Activity] activa — llamar desde un click de usuario.
     */
    fun launchBillingFlow(activity: Activity) {
        val productDetails = _proProductDetails.value ?: run {
            Timber.w("BillingManager: product details not loaded yet")
            _billingState.value = BillingState.Error("Product not available. Try again.")
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        _billingState.value = BillingState.Purchasing
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.w("BillingManager: launchBillingFlow failed — ${result.debugMessage}")
            _billingState.value = BillingState.Error(result.debugMessage)
        }
    }

    // ── Internos ─────────────────────────────────────────────────────────────

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(SKU_PRO_LIFETIME)) return

        // Acknowledge: obligatorio para confirmar la compra en Google Play.
        // Sin acknowledge, la compra se revierte automáticamente a los 3 días.
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            scope.launch {
                val result = billingClient.acknowledgePurchase(ackParams)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("BillingManager: purchase acknowledged")
                    unlockPro()
                } else {
                    Timber.w("BillingManager: acknowledge failed — ${result.debugMessage}")
                }
            }
        } else {
            unlockPro()
        }
    }

    private fun unlockPro() {
        featureGate.unlockPro()
        _billingState.value = BillingState.PurchaseSuccess
        Timber.d("BillingManager: PRO unlocked via FeatureGate")
    }

    private suspend fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            result.purchasesList.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.contains(SKU_PRO_LIFETIME)
                ) {
                    unlockPro()
                    Timber.d("BillingManager: existing PRO purchase found — restored")
                }
            }
        }
    }

    private suspend fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SKU_PRO_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _proProductDetails.value = result.productDetailsList?.firstOrNull()
            Timber.d("BillingManager: product details loaded — ${_proProductDetails.value?.name}")
        }
    }
}
