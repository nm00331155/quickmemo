package com.quickmemo.app.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdsManager @Inject constructor() {

    fun initialize(context: Context) {
        MobileAds.initialize(context)
    }

    companion object {
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    }
}
