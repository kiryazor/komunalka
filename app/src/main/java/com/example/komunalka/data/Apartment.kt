package com.example.komunalka.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Квартира пользователя. Одна из «основных» сущностей.
 * Один-ко-многим: Apartment -> Bill (в одной квартире много счетов).
 */
@Entity(tableName = "apartments")
data class Apartment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val areaSqm: Double? = null
)
