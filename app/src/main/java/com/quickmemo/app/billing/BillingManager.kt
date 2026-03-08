package com.quickmemo.app.billing

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
import com.quickmemo.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsRepository: SettingsRepository,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var latestInAppPurchases: List<Purchase> = emptyList()
    private var latestSubPurchases: List<Purchase> = emptyList()

    private val billingClient: BillingClient = BillingClient
        .newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    init {
        startConnection()
    }

    fun startConnection() {
        if (billingClient.isReady) {
            _state.value = _state.value.copy(isReady = true)
            queryProductDetails()
            queryPurchases()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                _state.value = _state.value.copy(isReady = false)
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val ready = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                _state.value = _state.value.copy(isReady = ready)
                if (ready) {
                    queryProductDetails()
                    queryPurchases()
                }
            }
        })
    }

    fun launchPurchase(activity: Activity) {
        launchPurchase(activity, Products.REMOVE_ADS)
    }

    fun launchPurchase(activity: Activity, productId: String) {
        if (!_state.value.isReady) return

        val details = _state.value.inAppProductDetails[productId]
            ?: _state.value.subscriptionProductDetails[productId]
            ?: return

        val detailsParamsBuilder = BillingFlowParams.ProductDetailsParams
            .newBuilder()
            .setProductDetails(details)

        _state.value = _state.value.copy(
            isPurchaseInProgress = true,
            lastErrorMessage = null,
        )

        billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(detailsParamsBuilder.build()))
                .build(),
        )
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        _state.value = _state.value.copy(isPurchaseInProgress = false)

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            acknowledgePurchases(purchases)
            queryPurchases()
        } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            _state.value = _state.value.copy(lastErrorMessage = billingResult.debugMessage)
        }
    }

    fun queryPurchases() {
        if (!billingClient.isReady) return

        queryPurchasesByType(BillingClient.ProductType.INAPP) { purchases ->
            latestInAppPurchases = purchases
            acknowledgePurchases(purchases)
            updatePurchaseState()
        }

        queryPurchasesByType(BillingClient.ProductType.SUBS) { purchases ->
            latestSubPurchases = purchases
            acknowledgePurchases(purchases)
            updatePurchaseState()
        }
    }

    private fun queryProductDetails() {
        if (!billingClient.isReady) return

        queryProductDetailsByType(
            productType = BillingClient.ProductType.INAPP,
            productIds = listOf(
                Products.REMOVE_ADS,
                Products.UNLOCK_TRANSLATION,
            ),
        ) { detailsMap ->
            _state.value = _state.value.copy(
                inAppProductDetails = detailsMap,
                productDetails = detailsMap[Products.REMOVE_ADS],
                subscriptionProductDetails = emptyMap(),
            )
        }
    }

    private fun queryPurchasesByType(
        productType: String,
        onResult: (List<Purchase>) -> Unit,
    ) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(productType)
                .build(),
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onResult(purchases)
            } else {
                _state.value = _state.value.copy(lastErrorMessage = billingResult.debugMessage)
            }
        }
    }

    private fun queryProductDetailsByType(
        productType: String,
        productIds: List<String>,
        onResult: (Map<String, ProductDetails>) -> Unit,
    ) {
        val productList = productIds.map { productId ->
            QueryProductDetailsParams.Product
                .newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, details ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onResult(details.associateBy { it.productId })
            } else {
                _state.value = _state.value.copy(lastErrorMessage = billingResult.debugMessage)
            }
        }
    }

    private fun updatePurchaseState() {
        val allPurchases = latestInAppPurchases + latestSubPurchases
        val purchaseState = PurchaseState(
            isAdFree = hasPurchased(allPurchases, Products.REMOVE_ADS),
            hasTranslation = hasPurchased(allPurchases, Products.UNLOCK_TRANSLATION),
        )

        val isPurchased = !purchaseState.shouldShowAds
        _state.value = _state.value.copy(
            isPurchased = isPurchased,
            purchaseState = purchaseState,
        )

        scope.launch {
            settingsRepository.setRemoveAdsPurchased(isPurchased)
        }
    }

    private fun hasPurchased(purchases: List<Purchase>, productId: String): Boolean {
        return purchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(productId)
        }
    }

    private fun acknowledgePurchases(purchases: List<Purchase>) {
        purchases
            .filter { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged
            }
            .forEach { purchase ->
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build(),
                ) { }
            }
    }

    object Products {
        const val REMOVE_ADS = "remove_ads"
        const val UNLOCK_TRANSLATION = "unlock_translation"
    }

    companion object {
        const val PRODUCT_ID_REMOVE_ADS = Products.REMOVE_ADS
        const val PRODUCT_ID_UNLOCK_TRANSLATION = Products.UNLOCK_TRANSLATION
    }
}
