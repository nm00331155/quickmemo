package com.quickmemo.app.presentation.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.quickmemo.app.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
) : ViewModel() {

    val billingState: StateFlow<com.quickmemo.app.billing.BillingState> = billingManager.state

    fun refresh() {
        billingManager.startConnection()
        billingManager.queryPurchases()
    }

    fun purchase(activity: Activity, productId: String) {
        billingManager.launchPurchase(activity, productId)
    }
}
