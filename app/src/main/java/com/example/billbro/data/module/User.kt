package com.example.billbro.data.module

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.billbro.data.dao.MapConverter

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val email: String,
    @param:TypeConverters(MapConverter::class)
    val balanceMap: Map<String, Double> = emptyMap()
)

class User(
    val userId: String,
    val name: String,
    val email: String,
    private val balanceMap: MutableMap<String, Double> = mutableMapOf()
) : Observer {

    private val observers = mutableListOf<Observer>()

    fun getBalance(): Map<String, Double> = balanceMap

    fun updateBalance(userId: String, amount: Double) {
        val current = balanceMap[userId] ?: 0.0
        balanceMap[userId] = current + amount
        if (balanceMap[userId] == 0.0) {
            balanceMap.remove(userId)
        }
        notifyObservers("Balance updated for $userId: $amount")
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    private fun notifyObservers(message: String) {
        observers.forEach { it.update(message) }
    }

    override fun update(msg: String) {
        println("User $name received update: $msg")
    }

    fun toEntity(): UserEntity {
        return UserEntity(userId, name, email, balanceMap)
    }

    companion object {
        fun fromEntity(entity: UserEntity): User {
            return User(entity.userId, entity.name, entity.email, entity.balanceMap.toMutableMap())
        }
    }
}