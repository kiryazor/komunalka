package com.example.komunalka.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Промежуточная (4-я) таблица для связи многие-ко-многим между
 * Payment и Bill: один платёж может частично или полностью
 * закрывать несколько разных счетов.
 */
@Entity(
    tableName = "payment_bill_cross_ref",
    primaryKeys = ["paymentId", "billId"],
    foreignKeys = [
        ForeignKey(
            entity = Payment::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Bill::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("paymentId"), Index("billId")]
)
data class PaymentBillCrossRef(
    val paymentId: Long,
    val billId: Long,
    val amountApplied: Double
)
