package com.example.komunalka.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Платёж пользователя. Один платёж может закрывать сразу несколько счетов
 * (например, оплата "одной кнопкой" сразу за свет, воду и газ) —
 * поэтому связь Payment <-> Bill реализована как многие-ко-многим
 * через промежуточную таблицу PaymentBillCrossRef.
 */
@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val totalAmount: Double,
    val method: String
)
