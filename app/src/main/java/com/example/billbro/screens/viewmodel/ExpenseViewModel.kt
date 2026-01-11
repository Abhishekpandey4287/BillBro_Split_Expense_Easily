package com.example.billbro.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billbro.data.module.DetailedLine
import com.example.billbro.data.module.Expense
import com.example.billbro.data.module.Group
import com.example.billbro.data.module.SplitType
import com.example.billbro.data.module.User
import com.example.billbro.data.repository.BillBro
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val billBro: BillBro
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    private val _usersInGroup = MutableStateFlow<List<User>>(emptyList())
    val usersInGroup: StateFlow<List<User>> = _usersInGroup

    private val _balances = MutableStateFlow<Map<String, Map<String, Double>>>(emptyMap())
    val balances: StateFlow<Map<String, Map<String, Double>>> = _balances.asStateFlow()


    fun loadGroupExpenses(groupId: String) {
        viewModelScope.launch {
            val allExpenses = billBro.getAllExpenses()
            _expenses.value = allExpenses.filter { it.groupId == groupId }
        }
    }

    fun loadUsersInGroup(groupId: String) {
        viewModelScope.launch {
            val group = billBro.getGroup(groupId)
            group?.let {
                _usersInGroup.value = it.getMembers()
            }
        }
    }

    suspend fun addExpense(
        description: String,
        amount: Double,
        paidUserId: String,
        groupId: String,
        splitType: SplitType,
        values: List<Double>?,
        splitBetweenUserIds: List<String>? = null
    ) {
        billBro.addExpense(
            description = description,
            amount = amount,
            paidUserId = paidUserId,
            groupId = groupId,
            splitType = splitType,
            values = values,
            splitBetweenUserIds = splitBetweenUserIds
        )
        loadGroupExpenses(groupId)
        loadUsersInGroup(groupId)
    }

    suspend fun getDetailedNetSummary(
        groupId: String,
        targetUserId: String
    ): List<DetailedLine> {

        val expenses = billBro.getAllExpenses().filter { it.groupId == groupId }
        val users = _usersInGroup.value
        if (users.isEmpty()) return emptyList()

        val rawDebts = mutableMapOf<Pair<String, String>, Double>()

        expenses.forEach { expense ->
            val splits = billBro.getSplitsByExpense(expense.expenseId)

            splits.forEach { split ->
                if (split.userId != expense.paidUserId) {
                    val key = split.userId to expense.paidUserId
                    rawDebts[key] = (rawDebts[key] ?: 0.0) + split.amount
                }
            }
        }

        val netDebts = mutableMapOf<Pair<String,String>, Double>()

        rawDebts.forEach { (pair, amountAB) ->
            val (A, B) = pair
            val amountBA = rawDebts[B to A] ?: 0.0
            val net = amountAB - amountBA

            if (net > 0.01) netDebts[A to B] = net
            else if (-net > 0.01) netDebts[B to A] = -net
        }

        val result = mutableListOf<DetailedLine>()

        netDebts.forEach { (pair, amount) ->
            val (A, B) = pair
            val nameA = users.find { it.userId == A }?.name ?: A
            val nameB = users.find { it.userId == B }?.name ?: B

            val formattedAmount = "₹${"%.2f".format(amount)}"

            if (A == targetUserId) {
                result.add(
                    DetailedLine(
                        text = "I owe $nameB $formattedAmount",
                        isOwed = true
                    )
                )
            } else if (B == targetUserId) {
                result.add(
                    DetailedLine(
                        text = "$nameA owes me $formattedAmount",
                        isOwed = false
                    )
                )
            }
        }

        return result
    }

    suspend fun calculateNetBalances(groupId: String): Map<String, Double> {

        val group = billBro.getGroup(groupId) ?: return emptyMap()
        val users = group.getMembers()

        val netBalances = mutableMapOf<String, Double>()
        users.forEach { netBalances[it.userId] = 0.0 }

        val expenses = billBro.getAllExpenses()
            .filter { it.groupId == groupId }

        expenses.forEach { expense ->
            netBalances[expense.paidUserId] =
                netBalances[expense.paidUserId]!! + expense.amount

            val splits = billBro.getSplitsByExpense(expense.expenseId)
            splits.forEach { split ->
                netBalances[split.userId] =
                    netBalances[split.userId]!! - split.amount
            }
        }

        return netBalances
    }

    fun deleteExpense(expenseId: String, groupId: String) {
        viewModelScope.launch {
            billBro.deleteExpense(expenseId)
            loadGroupExpenses(groupId)
            loadUsersInGroup(groupId)
        }
    }
}