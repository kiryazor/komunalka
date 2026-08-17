package com.example.komunalka.data.dao

import androidx.room.*
import com.example.komunalka.data.Payment
import com.example.komunalka.data.PaymentBillCrossRef
import com.example.komunalka.data.relations.BillWithPayments
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Insert
    suspend fun insertPayment(payment: Payment): Long

    @Insert
    suspend fun insertCrossRef(crossRef: PaymentBillCrossRef)

    /** Регистрирует платёж и сразу привязывает его к одному счёту. */
    @Transaction
    suspend fun payForBill(billId: Long, amount: Double, method: String, dateMillis: Long) {
        val paymentId = insertPayment(Payment(dateMillis = dateMillis, totalAmount = amount, method = method))
        insertCrossRef(PaymentBillCrossRef(paymentId = paymentId, billId = billId, amountApplied = amount))
    }

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :billId")
    fun observeBillWithPayments(billId: Long): Flow<BillWithPayments?>

    @Query(
        """SELECT COALESCE(SUM(amountApplied), 0) FROM payment_bill_cross_ref WHERE billId = :billId"""
    )
    suspend fun getPaidAmountForBill(billId: Long): Double

    @Delete
    suspend fun deletePayment(payment: Payment)
}
