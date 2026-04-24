package com.customlauncher.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages Google Play Billing for subscriptions and one-time purchases.
 * Uses BillingClient v7 (PurchasesUpdatedListener via constructor).
 */
class BillingManager(
    context: Context,
    private val premiumManager: PremiumManager
) : PurchasesUpdatedListener {

    private val tag = "BillingManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Product IDs — replace with your real Play Console product IDs
    companion object {
        const val SKU_WEEKLY_TRIAL  = "premium_weekly_trial"   // 3-day free trial → weekly
        const val SKU_MONTHLY       = "premium_monthly"
        const val SKU_ANNUAL        = "premium_annual"
    }

    private val _productsState = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productsState: StateFlow<List<ProductDetails>> = _productsState

    private val _purchaseState = MutableStateFlow<PurchaseResult>(PurchaseResult.Idle)
    val purchaseState: StateFlow<PurchaseResult> = _purchaseState

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        connect()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(tag, "Billing connected")
                    scope.launch {
                        queryProducts()
                        restorePurchases()
                    }
                } else {
                    Log.w(tag, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(tag, "Billing disconnected — will retry on next call")
            }
        })
    }

    private suspend fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_WEEKLY_TRIAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params)
        }

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productsState.value = result.productDetailsList ?: emptyList()
            Log.d(tag, "Products loaded: ${result.productDetailsList?.size}")
        } else {
            Log.w(tag, "queryProductDetails failed: ${result.billingResult.debugMessage}")
        }
    }

    /** Launch the Play billing flow for a given product ID. */
    fun launchBillingFlow(activity: Activity, productId: String) {
        val product = _productsState.value.find { it.productId == productId }
        if (product == null) {
            Log.w(tag, "Product $productId not found — retrying connection")
            _purchaseState.value = PurchaseResult.Error("Product not available. Please try again.")
            connect()
            return
        }

        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: run {
            _purchaseState.value = PurchaseResult.Error("No offer available for this product.")
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        _purchaseState.value = PurchaseResult.Loading
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseState.value = PurchaseResult.Error(result.debugMessage)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseResult.Cancelled
            }
            else -> {
                _purchaseState.value = PurchaseResult.Error(result.debugMessage)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            scope.launch {
                acknowledgePurchase(purchase)
                premiumManager.setPremium(true)
                _purchaseState.value = PurchaseResult.Success(purchase)
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            _purchaseState.value = PurchaseResult.Pending
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            withContext(Dispatchers.IO) {
                billingClient.acknowledgePurchase(params)
            }
        }
    }

    /** Restore existing active subscriptions (called on connect and on user request). */
    suspend fun restorePurchases() {
        if (!billingClient.isReady) { connect(); return }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(params)
        }

        val hasActive = result.purchasesList.any {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        result.purchasesList.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }.forEach { acknowledgePurchase(it) }

        premiumManager.setPremium(hasActive)

        if (hasActive) {
            _purchaseState.value = PurchaseResult.Restored
        }

        Log.d(tag, "Restore complete — isPremium=$hasActive")
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseResult.Idle
    }

    fun destroy() {
        if (billingClient.isReady) billingClient.endConnection()
    }
}

sealed class PurchaseResult {
    data object Idle      : PurchaseResult()
    data object Loading   : PurchaseResult()
    data object Cancelled : PurchaseResult()
    data object Pending   : PurchaseResult()
    data object Restored  : PurchaseResult()
    data class  Success(val purchase: Purchase) : PurchaseResult()
    data class  Error(val message: String)      : PurchaseResult()
}
