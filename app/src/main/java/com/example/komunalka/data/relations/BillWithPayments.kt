package com.example.komunalka.data.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.komunalka.data.Bill
import com.example.komunalka.data.Payment
import com.example.komunalka.data.PaymentBillCrossRef

/**
 * Счёт вместе со всеми платежами, которые к нему применены
 * (many-to-many через промежуточную таблицу).
 */
data class BillWithPayments(
    @Embedded val bill: Bill,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PaymentBillCrossRef::class,
            parentColumn = "billId",
            entityColumn = "paymentId"
        )
    )
    val payments: List<Payment>
)
