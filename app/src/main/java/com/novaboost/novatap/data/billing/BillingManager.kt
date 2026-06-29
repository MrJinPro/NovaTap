package com.novaboost.novatap.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onPremiumUnlocked: (Boolean) -> Unit,
    private val onMessage: (String) -> Unit
) {
    private var billingClient: BillingClient? = null
    
    private val _premiumProductDetails = MutableStateFlow<ProductDetails?>(null)
    val premiumProductDetails: StateFlow<ProductDetails?> = _premiumProductDetails

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady

    companion object {
        const val SUBSCRIPTION_ID = "novatap_premium_monthly"
        private const val TAG = "NovaTapBilling"
    }

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases?.forEach { purchase ->
                        handlePurchase(purchase)
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    onMessage(if (isRussianLocale()) "Покупка отменена" else "Purchase cancelled")
                }
                else -> {
                    onMessage("${if (isRussianLocale()) "Ошибка оплаты" else "Billing error"}: ${billingResult.debugMessage}")
                    Log.e(TAG, "Billing failed with response: ${billingResult.responseCode}, message: ${billingResult.debugMessage}")
                }
            }
        }

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        connectToGooglePlay()
    }

    private fun connectToGooglePlay() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Client successfully set up!")
                    _isBillingReady.value = true
                    queryActivePurchases()
                    queryProductDetails()
                } else {
                    Log.e(TAG, "Billing setup failed with response code: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected, attempting to reconnect...")
                _isBillingReady.value = false
                // Try to reconnect with delay
                coroutineScope.launch {
                    kotlinx.coroutines.delay(5000)
                    connectToGooglePlay()
                }
            }
        })
    }

    fun queryActivePurchases() {
        if (billingClient == null || !_isBillingReady.value) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient?.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var premiumFound = false
                for (purchase in purchasesList) {
                    if (purchase.products.contains(SUBSCRIPTION_ID) && 
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        premiumFound = true
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }
                Log.d(TAG, "Active premium subscription check result: $premiumFound")
                onPremiumUnlocked(premiumFound)
            } else {
                Log.e(TAG, "Failed querying purchases: ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryProductDetails() {
        if (billingClient == null || !_isBillingReady.value) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsList.firstOrNull { it.productId == SUBSCRIPTION_ID }
                _premiumProductDetails.value = details
                Log.d(TAG, "Premium product details found: ${details != null}")
            } else {
                Log.e(TAG, "Failed querying product details: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchSubscriptionPurchase(activity: Activity) {
        val details = _premiumProductDetails.value
        if (details == null) {
            onMessage(if (isRussianLocale()) "Продукт недоступен в Google Play" else "Product details not loaded from Google Play")
            queryProductDetails() // retry querying
            return
        }

        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            onMessage(if (isRussianLocale()) "Нет доступных предложений для подписки" else "No active subscription offer found")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.products.contains(SUBSCRIPTION_ID)) {
                onPremiumUnlocked(true)
                onMessage(if (isRussianLocale()) "Премиум-подписка активирована!" else "Premium subscription activated successfully!")
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase successfully acknowledged to avoid auto-refund")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    private fun isRussianLocale(): Boolean {
        return context.resources.configuration.locales[0].language == "ru"
    }
}
