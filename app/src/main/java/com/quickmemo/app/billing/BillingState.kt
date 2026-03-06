package com.quickmemo.app.billing

import com.android.billingclient.api.ProductDetails

data class PurchaseState(
    val isPro: Boolean = false,
    val isAdFree: Boolean = false,
    val hasTranslatePack: Boolean = false,
    val hasAllInOne: Boolean = false,
) {
    val shouldShowAds: Boolean
        get() = !isPro && !isAdFree && !hasAllInOne

    val hasTranslation: Boolean
        get() = isPro || hasTranslatePack || hasAllInOne
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
