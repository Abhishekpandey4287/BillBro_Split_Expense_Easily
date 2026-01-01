package com.example.billbro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.billbro.data.dao.GroupUserJoin
import com.example.billbro.data.dao.MapConverter
import com.example.billbro.data.dao.SplitTypeConverter
import com.example.billbro.data.dao.*
import com.example.billbro.data.module.*

@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        ExpenseEntity::class,
        SplitEntity::class,
        GroupUserJoin::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(MapConverter::class, SplitTypeConverter::class)
abstract class BillBroDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun splitDao(): SplitDao
    abstract fun groupUserJoinDao(): GroupUserJoinDao

    companion object {
        @Volatile
        private var INSTANCE: BillBroDatabase? = null

        fun getDatabase(context: Context): BillBroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BillBroDatabase::class.java,
                    "billbro_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}