package com.example.komunalka.repository

import com.example.komunalka.data.*
import com.example.komunalka.data.dao.MonthlyTotal
import com.example.komunalka.data.dao.UtilityTotal
import com.example.komunalka.data.relations.BillWithDetails
import com.example.komunalka.data.relations.BillWithPayments
import kotlinx.coroutines.flow.Flow

enum class BillSort { DATE, AMOUNT, NAME }
enum class PaidFilter { ALL, PAID, UNPAID }

/**
 * Единая точка доступа к данным — скрывает от ViewModel'ей то, что
 * источником данных является Room/SQLite. Всё общение с БД идёт
 * через suspend-функции и Flow, поэтому UI никогда не блокируется.
 */
class UtilityRepository(private val db: AppDatabase) {

    // Квартиры -----------------------------------------------------------
    fun observeApartments(): Flow<List<Apartment>> = db.apartmentDao().observeAll()
    suspend fun getApartment(id: Long): Apartment? = db.apartmentDao().getById(id)
    suspend fun addApartment(apartment: Apartment): Long = db.apartmentDao().insert(apartment)
    suspend fun updateApartment(apartment: Apartment) = db.apartmentDao().update(apartment)
    suspend fun deleteApartment(apartment: Apartment) = db.apartmentDao().delete(apartment)

    // Типы услуг -----------------------------------------------------------
    fun observeUtilityTypes(): Flow<List<UtilityType>> = db.utilityTypeDao().observeAll()
    suspend fun getUtilityTypesOnce(): List<UtilityType> = db.utilityTypeDao().getAllOnce()
    suspend fun addUtilityType(type: UtilityType): Long = db.utilityTypeDao().insert(type)
    suspend fun updateUtilityType(type: UtilityType) = db.utilityTypeDao().update(type)
    suspend fun deleteUtilityType(type: UtilityType) = db.utilityTypeDao().delete(type)

    // Счета -----------------------------------------------------------
    suspend fun getBill(id: Long): Bill? = db.billDao().getById(id)
    fun observeBillWithDetails(id: Long): Flow<BillWithDetails?> = db.billDao().observeBillWithDetails(id)
    suspend fun addBill(bill: Bill): Long = db.billDao().insert(bill)
    suspend fun updateBill(bill: Bill) = db.billDao().update(bill)
    suspend fun deleteBill(bill: Bill) = db.billDao().delete(bill)
    suspend fun setPaid(billId: Long, isPaid: Boolean) = db.billDao().setPaidStatus(billId, isPaid)

    fun observeBills(
        apartmentId: Long,
        query: String,
        filter: PaidFilter,
        sort: BillSort
    ): Flow<List<BillWithDetails>> {
        val paid: Boolean? = when (filter) {
            PaidFilter.ALL -> null
            PaidFilter.PAID -> true
            PaidFilter.UNPAID -> false
        }
        return when (sort) {
            BillSort.DATE -> db.billDao().observeByDateDesc(apartmentId, query, paid)
            BillSort.AMOUNT -> db.billDao().observeByAmountDesc(apartmentId, query, paid)
            BillSort.NAME -> db.billDao().observeByNameAsc(apartmentId, query, paid)
        }
    }

    // Платежи -----------------------------------------------------------
    fun observeBillWithPayments(billId: Long): Flow<BillWithPayments?> = db.paymentDao().observeBillWithPayments(billId)

    suspend fun payForBill(billId: Long, amount: Double, method: String, dateMillis: Long) {
        db.paymentDao().payForBill(billId, amount, method, dateMillis)
        val alreadyPaid = db.paymentDao().getPaidAmountForBill(billId)
        val bill = db.billDao().getById(billId)
        if (bill != null && alreadyPaid >= bill.amount) {
            db.billDao().setPaidStatus(billId, true)
        }
    }

    // Статистика -----------------------------------------------------------
    fun observeTotalsByUtility(apartmentId: Long, period: String): Flow<List<UtilityTotal>> =
        db.billDao().observeTotalsByUtility(apartmentId, period)

    fun observeMonthlyTotals(apartmentId: Long): Flow<List<MonthlyTotal>> =
        db.billDao().observeMonthlyTotals(apartmentId)

    fun observeTotalPaid(apartmentId: Long): Flow<Double> = db.billDao().observeTotalPaid(apartmentId)
    fun observeTotalUnpaid(apartmentId: Long): Flow<Double> = db.billDao().observeTotalUnpaid(apartmentId)
    fun observeAverageBill(apartmentId: Long): Flow<Double> = db.billDao().observeAverageBill(apartmentId)
}
