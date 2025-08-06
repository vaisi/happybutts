// Created: PremiumViewModel to handle premium status for UI components
package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.repository.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import android.app.Activity

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val premiumManager: PremiumManager
) : ViewModel() {
    
    val isPremium: StateFlow<Boolean> = premiumManager.isPremium
    
    fun refreshPremiumStatus() {
        premiumManager.refreshPremiumStatus()
    }
    
    fun purchasePremium(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        premiumManager.purchasePremium(activity, onSuccess, onError)
    }
} 