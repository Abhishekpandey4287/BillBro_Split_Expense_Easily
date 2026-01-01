package com.example.billbro.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.billbro.data.module.GroupEntity
import com.example.billbro.data.module.UserEntity

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Query("SELECT * FROM `groups` WHERE groupId = :groupId")
    suspend fun getGroupById(groupId: String): GroupEntity?

    @Query("SELECT * FROM `groups` WHERE LOWER(name) = LOWER(:name)")
    suspend fun getGroupByName(name: String): GroupEntity?

    @Query("SELECT * FROM `groups`")
    suspend fun getAllGroups(): List<GroupEntity>

    // In GroupDao.kt, add this function:
    @Query("DELETE FROM `groups` WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("""
        SELECT users.* FROM users
        INNER JOIN group_user_join ON users.userId = group_user_join.userId
        WHERE group_user_join.groupId = :groupId
    """)
    suspend fun getUsersInGroup(groupId: String): List<UserEntity>
}