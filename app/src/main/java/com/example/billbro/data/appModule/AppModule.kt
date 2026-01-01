package com.example.billbro.data.appModule

import android.content.Context
import com.example.billbro.data.dao.*
import com.example.billbro.data.database.BillBroDatabase
import com.example.billbro.data.module.DebtSimplifier
import com.example.billbro.data.module.SplitFactory
import com.example.billbro.data.repository.BillBro
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BillBroDatabase {
        return BillBroDatabase.getDatabase(context)
    }

    @Provides
    fun provideUserDao(database: BillBroDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideGroupDao(database: BillBroDatabase): GroupDao {
        return database.groupDao()
    }

    @Provides
    fun provideExpenseDao(database: BillBroDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    fun provideSplitDao(database: BillBroDatabase): SplitDao {
        return database.splitDao()
    }

    @Provides
    fun provideGroupUserJoinDao(database: BillBroDatabase): GroupUserJoinDao {
        return database.groupUserJoinDao()
    }

    @Provides
    @Singleton
    fun provideSplitFactory(): SplitFactory {
        return SplitFactory()
    }

    @Provides
    @Singleton
    fun provideDebtSimplifier(): DebtSimplifier {
        return DebtSimplifier()
    }

    @Provides
    @Singleton
    fun provideBillBro(
        userDao: UserDao,
        groupDao: GroupDao,
        expenseDao: ExpenseDao,
        splitDao: SplitDao,
        splitFactory: SplitFactory,
        debtSimplifier: DebtSimplifier,
        groupUserJoinDao: GroupUserJoinDao
    ): BillBro {
        return BillBro(userDao, groupDao, expenseDao, splitDao, splitFactory, debtSimplifier , groupUserJoinDao )
    }
}