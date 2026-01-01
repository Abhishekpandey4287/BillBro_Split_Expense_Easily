package com.example.billbro.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.billbro.data.module.ExpenseEntity

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE expenseId = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    @Query("SELECT * FROM expenses WHERE expenseId = :expenseId")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE groupId = :groupId")
    suspend fun getExpensesByGroup(groupId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE paidUserId = :userId")
    suspend fun getExpensesByUser(userId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<ExpenseEntity>
}