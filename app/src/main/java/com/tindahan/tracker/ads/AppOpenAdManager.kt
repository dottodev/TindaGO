package com.tindahan.tracker.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

/**
 * Shows one App Open ad each time the app is opened (cold start or return
 * from background after [MIN_BACKGROUND_MS]). Short trips away (e.g. the
 * system share sheet) do not trigger an ad.
 */
class AppOpenAdManager(private val app: Application) : Application.ActivityLifecycleCallbacks {

    companion object {
        const val AD_UNIT_ID = "ca-app-pub-7136110163848507/2460433336"
        private const val MIN_BACKGROUND_MS = 60_000L
        private const val MAX_AD_AGE_MS = 4 * 60 * 60 * 1000L
    }

    private var ad: AppOpenAd? = null
    private var isShowing = false
    private var isLoading = false
    private var loadTime = 0L
    private var lastShown = 0L
    private var backgroundedAt = 0L
    private var startedCount = 0
    private var firstStart = true

    fun load() {
        if (ad != null || isShowing || isLoading) return
        isLoading = true
        AppOpenAd.load(
            app,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    this@AppOpenAdManager.ad = ad
                    loadTime = System.currentTimeMillis()
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    this@AppOpenAdManager.ad = null
                    isLoading = false
                }
            }
        )
    }

    private fun isFresh(): Boolean =
        ad != null && System.currentTimeMillis() - loadTime < MAX_AD_AGE_MS

    fun showIfAvailable(activity: Activity) {
        if (isShowing || activity.isFinishing) return
        val current = ad
        if (current != null && isFresh()) {
            current.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    ad = null
                    isShowing = false
                    lastShown = System.currentTimeMillis()
                    load()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    ad = null
                    isShowing = false
                    load()
                }

                override fun onAdShowedFullScreenContent() {
                    isShowing = true
                }
            }
            current.show(activity)
        } else {
            ad = null
            load()
        }
    }

    // ---- Foreground tracking: show once per app open, not on quick returns ----
    override fun onActivityStarted(activity: Activity) {
        if (startedCount == 0) {
            val away = if (backgroundedAt == 0L) Long.MAX_VALUE else System.currentTimeMillis() - backgroundedAt
            if (firstStart || away >= MIN_BACKGROUND_MS) {
                firstStart = false
                // Don't show twice in a row without real use in between.
                if (System.currentTimeMillis() - lastShown >= MIN_BACKGROUND_MS) {
                    showIfAvailable(activity)
                }
            }
        }
        startedCount++
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount = (startedCount - 1).coerceAtLeast(0)
        if (startedCount == 0) backgroundedAt = System.currentTimeMillis()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
