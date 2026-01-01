package com.example.billbro.data.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.billbro.data.module.GroupEntity
import com.example.billbro.data.module.UserEntity

@Dao
interface GroupUserJoinDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(join: GroupUserJoin)

    @Query("DELETE FROM group_user_join WHERE groupId = :groupId AND userId = :userId")
    suspend fun removeUserFromGroup(groupId: String, userId: String)

    @Query("SELECT * FROM group_user_join WHERE groupId = :groupId")
    suspend fun getGroupUsers(groupId: String): List<GroupUserJoin>
}

@Entity(
    tableName = "group_user_join",
    primaryKeys = ["groupId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GroupUserJoin(
    val groupId: String,
    val userId: String
)