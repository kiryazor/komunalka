package com.example.komunalka.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Счёт за коммунальную услугу для конкретной квартиры и периода.
 * Главная сущность приложения, вокруг которой строится весь CRUD.
 *
 * Связи:
 *  - apartmentId -> Apartment (многие Bill к одной Apartment)
 *  - utilityTypeId -> UtilityType (многие Bill к одному UtilityType)
 *  - Многие-ко-многим с Payment реализованы через PaymentBillCrossRef.
 */
@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = Apartment::class,
            parentColumns = ["id"],
            childColumns = ["apartmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UtilityType::class,
            parentColumns = ["id"],
            childColumns = ["utilityTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("apartmentId"), Index("utilityTypeId")]
)
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val apartmentId: Long,
    val utilityTypeId: Long,
    val period: String,              // "2026-09"
    val previousReading: Double? = null,
    val currentReading: Double? = null,
    val amount: Double,
    val dueDateMillis: Long,
    val isPaid: Boolean = false,
    val meterPhotoPath: String? = null,
    val comment: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
