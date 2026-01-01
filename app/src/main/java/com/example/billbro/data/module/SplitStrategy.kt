package com.example.billbro.data.module

interface SplitStrategy {
    fun calculateSplit(amount: Double, users: List<String>, values: List<Double>? = null): Map<String, Double>
}