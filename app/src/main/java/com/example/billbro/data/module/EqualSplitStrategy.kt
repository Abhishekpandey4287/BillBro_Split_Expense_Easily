package com.example.billbro.data.module

class EqualSplitStrategy : SplitStrategy {
    override fun calculateSplit(amount: Double, users: List<String>, values: List<Double>?): Map<String, Double> {
        val splitAmount = amount / users.size
        return users.associateWith { splitAmount }
    }
}

class PercentageSplitStrategy : SplitStrategy {
    override fun calculateSplit(amount: Double, users: List<String>, values: List<Double>?): Map<String, Double> {
        require(values != null && users.size == values.size) { "Percentage values required for each user" }
        require(values.sum() == 100.0) { "Percentages must sum to 100" }

        return users.mapIndexed { index, userId ->
            userId to (amount * values[index] / 100)
        }.toMap()
    }
}

class ExactSplitStrategy : SplitStrategy {
    override fun calculateSplit(amount: Double, users: List<String>, values: List<Double>?): Map<String, Double> {
        require(values != null && users.size == values.size) { "Exact amounts required for each user" }
        require(values.sum() == amount) { "Exact amounts must sum to total" }

        return users.mapIndexed { index, userId ->
            userId to values[index]
        }.toMap()
    }
}