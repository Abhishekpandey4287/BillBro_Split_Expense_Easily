package com.example.billbro.data.module

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.billbro.data.dao.SplitTypeConverter

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val expenseId: String,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val amount: Double,
    val paidUserId: String,
    val groupId: String? = null,
    @param:TypeConverters(SplitTypeConverter::class)
    val splitType: SplitType = SplitType.EQUAL
)

class Expense(
    val expenseId: String,
    val description: String,
    val date: Long,
    val amount: Double,
    val paidUserId: String,
    val splits: List<Split> = emptyList(),
    val groupId: String? = null,
    val splitType: SplitType = SplitType.EQUAL
) {
    fun toEntity(): ExpenseEntity {
        return ExpenseEntity(expenseId, description, date, amount, paidUserId, groupId, splitType)
    }
}