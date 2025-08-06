package com.example.myapplication.service

interface StepDetector {
    fun startDetection(onStepDetected: (Int) -> Unit)
    fun stopDetection()
    fun isAvailable(): Boolean
    fun resetDailyBaseline()
}