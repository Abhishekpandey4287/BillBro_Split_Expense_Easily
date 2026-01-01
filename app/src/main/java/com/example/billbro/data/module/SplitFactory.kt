package com.example.billbro.data.module

class SplitFactory {
    fun createSplitStrategy(splitType: SplitType): SplitStrategy {
        return when (splitType) {
            SplitType.EQUAL -> EqualSplitStrategy()
            SplitType.PERCENTAGE -> PercentageSplitStrategy()
            SplitType.EXACT -> ExactSplitStrategy()
            SplitType.BETWEEN -> EqualSplitStrategy()
        }
    }
}