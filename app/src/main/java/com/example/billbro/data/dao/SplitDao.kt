package com.example.billbro.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.billbro.data.module.SplitEntity

@Dao
interface SplitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplit(split: SplitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSplits(splits: List<SplitEntity>)

    @Query("SELECT * FROM splits WHERE expenseId = :expenseId")
    suspend fun getSplitsByExpense(expenseId: String): List<SplitEntity>


    @Query("SELECT * FROM splits WHERE userId = :userId")
    suspend fun getSplitsByUser(userId: String): List<SplitEntity>

    @Query("DELETE FROM splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsByExpense(expenseId: String)
}