package com.example.billbro.data.module

import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class DebtSimplifier @Inject constructor() {

    fun simplifyDebts(balanceMap: Map<String, Map<String, Double>>): Map<String, Map<String, Double>> {
        val netBalances = mutableMapOf<String, Double>()

        balanceMap.forEach { (userId, userBalances) ->
            netBalances[userId] = userBalances.values.sum()
        }

        val debtors = netBalances.filter { it.value < 0 }
            .map { it.key to -it.value }
            .toMutableList()

        val creditors = netBalances.filter { it.value > 0 }
            .map { it.key to it.value }
            .toMutableList()

        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }

        val simplified = mutableMapOf<String, MutableMap<String, Double>>()

        var dIndex = 0
        var cIndex = 0

        while (dIndex < debtors.size && cIndex < creditors.size) {
            val (debtor, debtAmount) = debtors[dIndex]
            val (creditor, creditAmount) = creditors[cIndex]

            val minAmount = minOf(debtAmount, creditAmount)

            if (minAmount > 0.01) {
                val debtorMap = simplified.getOrPut(debtor) { mutableMapOf() }
                debtorMap[creditor] = minAmount

                debtors[dIndex] = debtor to (debtAmount - minAmount)
                creditors[cIndex] = creditor to (creditAmount - minAmount)

                if (debtAmount - minAmount <= 0.01) {
                    dIndex++
                }
                if (creditAmount - minAmount <= 0.01) {
                    cIndex++
                }
            } else {
                if (debtAmount <= 0.01) dIndex++
                if (creditAmount <= 0.01) cIndex++
            }
        }

        return simplified
    }
}