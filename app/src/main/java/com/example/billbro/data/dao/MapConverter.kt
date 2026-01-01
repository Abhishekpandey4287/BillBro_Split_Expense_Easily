package com.example.billbro.data.dao

import androidx.room.TypeConverter
import com.example.billbro.data.module.SplitType

class MapConverter {
    @TypeConverter
    fun fromString(value: String?): Map<String, Double> {
        if (value.isNullOrEmpty()) return emptyMap()
        return try {
            value.split(";").associate {
                val (key, valueStr) = it.split(":")
                key to valueStr.toDouble()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun toString(map: Map<String, Double>?): String {
        return map?.map { "${it.key}:${it.value}" }?.joinToString(";") ?: ""
    }
}

class SplitTypeConverter {
    @TypeConverter
    fun fromString(value: String?): SplitType? {
        return value?.let { enumValueOf<SplitType>(it) }
    }

    @TypeConverter
    fun toString(splitType: SplitType?): String? {
        return splitType?.name
    }
}