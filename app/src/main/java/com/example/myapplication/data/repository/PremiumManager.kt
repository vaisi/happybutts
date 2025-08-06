// Created: PremiumManager for Google Play Billing premium entitlement check
package com.example.myapplication.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
// import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "PremiumManager"
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium
    
    // Temporarily disabled for build testing
    /*
    private var billingClient: BillingClient? = null
    private val skuList = listOf("premium_subscription") // Your product ID

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Billing client connected")
                    checkPremiumStatus()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    private fun checkPremiumStatus() {
        billingClient?.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasActivePurchase = purchases.any { purchase ->
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    _isPremium.value = hasActivePurchase
                    Log.i(TAG, "Premium status: $hasActivePurchase")
                }
            }
    }

    fun refreshPremiumStatus() {
        checkPremiumStatus()
    }

    fun purchasePremium(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_subscription") // Your product ID
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetails = productDetailsList.firstOrNull()
                if (productDetails != null) {
                    val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    if (offerToken != null) {
                        val productDetailsParamsList = listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )

                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build()

                        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
                        if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                            onSuccess()
                        } else {
                            onError("Billing flow failed: ${billingResult?.debugMessage}")
                        }
                    } else {
                        onError("No offer token found")
                    }
                } else {
                    onError("Product details not found")
                }
            } else {
                onError("Query product details failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.i(TAG, "Purchase acknowledged")
                        _isPremium.value = true
                    }
                }
            } else {
                _isPremium.value = true
            }
        }
    }
    */
    
    // Temporary implementation for testing
    fun refreshPremiumStatus() {
        // Do nothing for now
    }
    
    fun purchasePremium(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Do nothing for now
        onError("Premium features temporarily disabled")
    }
}