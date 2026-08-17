package com.example.komunalka.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Тип коммунальной услуги (Электричество, Вода, Газ, Отопление, ...).
 * Основная справочная сущность. Один-ко-многим: UtilityType -> Bill.
 */
@Entity(tableName = "utility_types")
data class UtilityType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String,       // напр. "кВт·ч", "м³", "Гкал"
    val tariff: Double,      // цена за единицу, ₽
    val iconKey: String = "default" // ключ для выбора иконки в UI
)
