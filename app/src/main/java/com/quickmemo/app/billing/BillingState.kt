package com.quickmemo.app.billing

import com.android.billingclient.api.ProductDetails

data class PurchaseState(
    val isAdFree: Boolean = false,
) {
    val shouldShowAds: Boolean
        get() = !isAdFree
}

data class BillingState(
    val isReady: Boolean = false,
    val isPurchased: Boolean = false,
    val isPurchaseInProgress: Boolean = false,
    val productDetails: ProductDetails? = null,
    val purchaseState: PurchaseState = PurchaseState(),
    val inAppProductDetails: Map<String, ProductDetails> = emptyMap(),
    val subscriptionProductDetails: Map<String, ProductDetails> = emptyMap(),
    val lastErrorMessage: String? = null,
)
