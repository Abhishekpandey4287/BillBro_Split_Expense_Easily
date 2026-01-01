package com.example.billbro.data.module

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val groupId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

class Group(
    val groupId: String,
    val name: String,
    private val members: MutableList<User> = mutableListOf(),
    private val expenseMap: MutableMap<String, Expense> = mutableMapOf(),
    private val balanceMap: MutableMap<String, MutableMap<String, Double>> = mutableMapOf()
) : Observer {

    private val observers = mutableListOf<Observer>()

    fun getMembers(): List<User> = members.toList()
    fun getExpenses(): Map<String, Expense> = expenseMap
    fun getBalances(): Map<String, Map<String, Double>> = balanceMap

    fun addMember(user: User) {
        if (members.none { it.userId == user.userId }) {
            members.add(user)
            balanceMap[user.userId] = mutableMapOf()
            user.addObserver(this)
            notifyAll("User ${user.name} added to group $name")
        }
    }

    fun removeMember(userId: String) {
        val user = members.find { it.userId == userId }
        user?.let {
            members.remove(it)
            balanceMap.remove(userId)
            balanceMap.forEach { (_, userBalance) ->
                userBalance.remove(userId)
            }
            it.removeObserver(this)
            notifyAll("User ${it.name} removed from group $name")
        }
    }

    fun addExpense(expense: Expense, splitStrategy: SplitStrategy, values: List<Double>?) {
        expenseMap[expense.expenseId] = expense

        val splits = splitStrategy.calculateSplit(
            expense.amount,
            members.map { it.userId },
            values
        )

        splits.forEach { (userId, amount) ->
            if (userId != expense.paidUserId) {
                updateBalance(userId, expense.paidUserId, amount)
            }
        }

        notifyAll("Expense '${expense.description}' of ${expense.amount} added to group $name")
    }

    fun updateBalance(fromUserId: String, toUserId: String, amount: Double) {
        val fromBalance = balanceMap.getOrPut(fromUserId) { mutableMapOf() }
        val currentFrom = fromBalance[toUserId] ?: 0.0
        fromBalance[toUserId] = currentFrom - amount

        if (fromBalance[toUserId] == 0.0) {
            fromBalance.remove(toUserId)
        }

        val toBalance = balanceMap.getOrPut(toUserId) { mutableMapOf() }
        val currentTo = toBalance[fromUserId] ?: 0.0
        toBalance[fromUserId] = currentTo + amount

        if (toBalance[fromUserId] == 0.0) {
            toBalance.remove(fromUserId)
        }

        members.find { it.userId == fromUserId }?.updateBalance(toUserId, -amount)
        members.find { it.userId == toUserId }?.updateBalance(fromUserId, amount)

        notifyAll("Balance updated: $fromUserId owes $toUserId $amount")
    }

    fun settlePayment(fromUserId: String, toUserId: String, amount: Double) {
        updateBalance(fromUserId, toUserId, -amount)
        notifyAll("Payment settled: $fromUserId paid $toUserId $amount")
    }

    fun simplifyDebts(debtSimplifier: DebtSimplifier): Map<String, Map<String, Double>> {
        return debtSimplifier.simplifyDebts(balanceMap)
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    private fun notifyAll(message: String) {
        observers.forEach { it.update("Group $name: $message") }
        members.forEach { it.update("Group $name: $message") }
    }

    override fun update(msg: String) {
        println("Group $name received: $msg")
    }

    fun toEntity(): GroupEntity {
        return GroupEntity(groupId, name)
    }
}