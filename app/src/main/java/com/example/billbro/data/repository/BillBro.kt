package com.example.billbro.data.repository

import com.example.billbro.data.dao.*
import com.example.billbro.data.module.DebtSimplifier
import com.example.billbro.data.module.SplitFactory
import com.example.billbro.data.module.*
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class BillBro @Inject constructor(
    private val userDao: UserDao,
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val splitDao: SplitDao,
    private val splitFactory: SplitFactory,
    private val debtSimplifier: DebtSimplifier,
    private val groupUserJoinDao: GroupUserJoinDao
) {

    private val users = mutableMapOf<String, User>()
    private val groups = mutableMapOf<String, Group>()
    private val expenses = mutableMapOf<String, Expense>()

    suspend fun createUser(name: String, email: String): User {
        val userId = UUID.randomUUID().toString()
        val user = User(userId, name, email)
        users[userId] = user
        userDao.insertUser(user.toEntity())
        return user
    }

    suspend fun getUser(userId: String): User? {
        return users[userId] ?: userDao.getUserById(userId)?.let {
            User.fromEntity(it).also { user -> users[userId] = user }
        }
    }

    suspend fun updateUser(userId: String, name: String, email: String): User? {
        val user = getUser(userId)
        user?.let {
            users[userId] = it
            userDao.updateUser(it.toEntity())
        }
        return user
    }

    suspend fun deleteUser(userId: String): Boolean {
        users.remove(userId)
        userDao.deleteUser(userId)
        return true
    }

    suspend fun createGroup(name: String): Group {
        val existingGroup = groupDao.getGroupByName(name)
        if (existingGroup != null) {
            throw IllegalArgumentException("Group with name '$name' already exists!")
        }

        val groupId = UUID.randomUUID().toString()
        val group = Group(groupId, name)

        groups[groupId] = group
        groupDao.insertGroup(group.toEntity())
        return group
    }

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)?.let { User.fromEntity(it) }
    }

    suspend fun getGroup(groupId: String): Group? {
        return groups[groupId] ?: groupDao.getGroupById(groupId)?.let { groupEntity ->
            val group = Group(groupEntity.groupId, groupEntity.name)

            val usersInGroup = groupDao.getUsersInGroup(groupId)
            usersInGroup.forEach { userEntity ->
                val user = User.fromEntity(userEntity)
                group.addMember(user)
            }

            val expenses = expenseDao.getExpensesByGroup(groupId)

            expenses.forEach { expenseEntity ->
                val splits = splitDao.getSplitsByExpense(expenseEntity.expenseId)
                splits.forEach { split ->
                    if (split.userId != expenseEntity.paidUserId) {
                        group.updateBalance(split.userId, expenseEntity.paidUserId, split.amount)
                    }
                }
            }

            groups[groupId] = group
            group
        }
    }

    suspend fun deleteGroup(groupId: String): Boolean {
        groups.remove(groupId)
        val groupUsers = groupUserJoinDao.getGroupUsers(groupId)
        groupUsers.forEach { join ->
            groupUserJoinDao.removeUserFromGroup(groupId, join.userId)
        }

        val groupExpenses = expenseDao.getExpensesByGroup(groupId)
        groupExpenses.forEach { expense ->
            expenseDao.deleteExpense(expense.expenseId)
        }

        groupDao.deleteGroup(groupId)

        return true
    }

    suspend fun addUserToGroup(groupId: String, userId: String): Boolean {
        val group = getGroup(groupId) ?: return false
        val user = getUser(userId) ?: return false

        group.addMember(user)
        groupUserJoinDao.insert(GroupUserJoin(groupId, userId))
        return true
    }

    suspend fun removeUserFromGroup(groupId: String, userId: String): Boolean {
        val group = getGroup(groupId) ?: return false
        group.removeMember(userId)
        groupUserJoinDao.removeUserFromGroup(groupId, userId)
        return true
    }

    suspend fun addExpense(
        description: String,
        amount: Double,
        paidUserId: String,
        groupId: String? = null,
        splitType: SplitType = SplitType.EQUAL,
        values: List<Double>? = null,
        splitBetweenUserIds: List<String>? = null
    ): Expense? {
        val expenseId = UUID.randomUUID().toString()

        if (groupId != null) {
            // Group expense
            val group = getGroup(groupId) ?: return null
            val paidUser = getUser(paidUserId) ?: return null

            val splitStrategy = splitFactory.createSplitStrategy(splitType)

            val users = if (splitType == SplitType.BETWEEN && !splitBetweenUserIds.isNullOrEmpty()) {
                splitBetweenUserIds
            } else {
                group.getMembers().map { it.userId }
            }

            // Calculate splits
            val splits = splitStrategy.calculateSplit(
                amount,
                users,
                values
            )

            // Create expense
            val expense = Expense(
                expenseId = expenseId,
                description = description,
                date = System.currentTimeMillis(),
                amount = amount,
                paidUserId = paidUserId,
                splits = splits.map { (userId, amount) ->
                    Split(userId, amount, splitType)
                },
                groupId = groupId,
                splitType = splitType
            )

            // Add to group
            group.addExpense(expense, splitStrategy, values)

            // Save to database
            expenseDao.insertExpense(expense.toEntity())
            splits.forEach { (userId, splitAmount) ->
                splitDao.insertSplit(Split(userId, splitAmount, splitType).toEntity(expenseId))
            }

            expenses[expenseId] = expense
            return expense

        } else {
            // Individual expense (not in diagram but mentioned)
            return addIndividualExpense(description, amount, paidUserId, splitType, values)
        }
    }

    suspend fun addIndividualExpense(
        description: String,
        amount: Double,
        paidUserId: String,
        splitType: SplitType = SplitType.EQUAL,
        values: List<Double>? = null
    ): Expense? {
        val expenseId = UUID.randomUUID().toString()
        val expense = Expense(
            expenseId = expenseId,
            description = description,
            date = System.currentTimeMillis(),
            amount = amount,
            paidUserId = paidUserId,
            splitType = splitType
        )

        expenseDao.insertExpense(expense.toEntity())
        expenses[expenseId] = expense

        return expense
    }

    suspend fun deleteIndividualPayment(paymentId: String): Boolean {
        // Implementation for deleting individual payment
        expenseDao.deleteExpense(paymentId)
        expenses.remove(paymentId)
        return true
    }

    suspend fun settlePayment(groupId: String, fromUserId: String, toUserId: String, amount: Double): Boolean {
        val group = getGroup(groupId) ?: return false
        group.settlePayment(fromUserId, toUserId, amount)
        return true
    }

    suspend fun simplifyGroupDebts(groupId: String): Map<String, Map<String, Double>> {
        val group = getGroup(groupId) ?: return emptyMap()
        return group.simplifyDebts(debtSimplifier)
    }

    // Get all users
    suspend fun getAllUsers(): List<User> {
        return userDao.getAllUsers().map { User.fromEntity(it) }
    }


    suspend fun getAllGroups(): List<Group> {
        return groupDao.getAllGroups().map { groupEntity ->
            val group = Group(groupEntity.groupId, groupEntity.name)

            val userEntities = groupDao.getUsersInGroup(groupEntity.groupId)
            userEntities.forEach { userEntity ->
                val user = User.fromEntity(userEntity)
                group.addMember(user)
            }

            group
        }
    }

    suspend fun getAllExpenses(): List<Expense> {
        return expenseDao.getAllExpenses().map {
            Expense(
                it.expenseId,
                it.description,
                it.date,
                it.amount,
                it.paidUserId,
                emptyList(),
                it.groupId,
                it.splitType
            )
        }
    }

    suspend fun getSplitsByExpense(expenseId: String): List<SplitEntity> {
        return splitDao.getSplitsByExpense(expenseId)
    }

    suspend fun deleteExpense(expenseId: String) {
        splitDao.deleteSplitsByExpense(expenseId)
        expenseDao.deleteExpense(expenseId)
    }
}