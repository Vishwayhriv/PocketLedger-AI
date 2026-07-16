package com.example.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

object AdMobManager {
    private const val TAG = "AdMobManager"

    // Official AdMob Test Ad Unit IDs
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private var transactionCount = 0
    private var lastInterstitialTimeMs: Long = 0

    fun initialize(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) {
            isInitialized = true
            Log.d(TAG, "AdMob Initialized.")
            preloadInterstitial(context)
            preloadRewarded(context)
        }
    }

    fun preloadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun preloadRewarded(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }

    fun trackCompletedTransaction(activity: Activity, isPremium: Boolean) {
        if (isPremium) return
        transactionCount++
        Log.d(TAG, "Completed transactions: $transactionCount")
        // "after every 8-10 completed transactions"
        if (transactionCount >= 8) {
            val currentTimeMs = System.currentTimeMillis()
            val timeDiffMs = currentTimeMs - lastInterstitialTimeMs
            // "Maximum: 1 interstitial every 10 minutes"
            if (timeDiffMs >= TimeUnit.MINUTES.toMillis(10)) {
                showInterstitial(activity)
                transactionCount = 0
                lastInterstitialTimeMs = currentTimeMs
            } else {
                Log.d(TAG, "Interstitial skipped due to 10-minute capping rule.")
            }
        }
    }

    private fun showInterstitial(activity: Activity) {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed.")
                    interstitialAd = null
                    preloadInterstitial(activity)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Interstitial ad failed to show: ${error.message}")
                    interstitialAd = null
                    preloadInterstitial(activity)
                }
            }
            ad.show(activity)
        } ?: run {
            Log.d(TAG, "Interstitial not preloaded yet. Preloading now...")
            preloadInterstitial(activity)
        }
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed.")
                    rewardedAd = null
                    preloadRewarded(activity)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Rewarded ad failed to show: ${error.message}")
                    rewardedAd = null
                    preloadRewarded(activity)
                }
            }
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount}")
                onRewardEarned()
            }
        } ?: run {
            Log.d(TAG, "Rewarded ad not ready. Fallback logic to protect user experience...")
            preloadRewarded(activity)
            onRewardEarned()
        }
    }
}

@Composable
fun AdMobBannerAd(isPremium: Boolean, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    if (isPremium) return

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // Let it persist or update
        }
    )
}

