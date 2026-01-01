package com.example.billbro.data.module

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.billbro.data.dao.SplitTypeConverter

@Entity(tableName = "splits")
data class SplitEntity(
    @PrimaryKey(autoGenerate = true)
    val splitId: Long = 0,
    val expenseId: String,
    val userId: String,
    val amount: Double,
    @param:TypeConverters(SplitTypeConverter::class)
    val splitType: SplitType
)

class Split(
    val userId: String,
    val amount: Double,
    val splitType: SplitType = SplitType.EQUAL
) {
    fun toEntity(expenseId: String): SplitEntity {
        return SplitEntity(expenseId = expenseId, userId = userId, amount = amount, splitType = splitType)
    }
}