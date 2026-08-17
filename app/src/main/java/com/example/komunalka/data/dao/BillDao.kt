package com.example.komunalka.data.dao

import androidx.room.*
import com.example.komunalka.data.Bill
import com.example.komunalka.data.relations.BillWithDetails
import kotlinx.coroutines.flow.Flow

/** Значение суммы по одному типу услуги — для круговой диаграммы статистики. */
data class UtilityTotal(val utilityName: String, val total: Double)

/** Сумма счетов за один календарный месяц — для столбчатой диаграммы / прогноза. */
data class MonthlyTotal(val period: String, val total: Double)

@Dao
interface BillDao {

    @Insert
    suspend fun insert(bill: Bill): Long

    @Update
    suspend fun update(bill: Bill)

    @Delete
    suspend fun delete(bill: Bill)

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getById(id: Long): Bill?

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :id")
    fun observeBillWithDetails(id: Long): Flow<BillWithDetails?>

    @Query("UPDATE bills SET isPaid = :isPaid WHERE id = :billId")
    suspend fun setPaidStatus(billId: Long, isPaid: Boolean)

    // --- Список с поиском/фильтром/сортировкой -----------------------------
    // Поиск ведётся по названию типа услуги и по комментарию (JOIN).
    // paidFilter: null = все, true = только оплаченные, false = только неоплаченные.

    @Transaction
    @Query(
        """
        SELECT bills.* FROM bills
        INNER JOIN utility_types ON bills.utilityTypeId = utility_types.id
        WHERE bills.apartmentId = :apartmentId
          AND (:paidFilter IS NULL OR bills.isPaid = :paidFilter)
          AND (utility_types.name LIKE '%' || :query || '%' OR bills.comment LIKE '%' || :query || '%')
        ORDER BY bills.dueDateMillis DESC
        """
    )
    fun observeByDateDesc(apartmentId: Long, query: String, paidFilter: Boolean?): Flow<List<BillWithDetails>>

    @Transaction
    @Query(
        """
        SELECT bills.* FROM bills
        INNER JOIN utility_types ON bills.utilityTypeId = utility_types.id
        WHERE bills.apartmentId = :apartmentId
          AND (:paidFilter IS NULL OR bills.isPaid = :paidFilter)
          AND (utility_types.name LIKE '%' || :query || '%' OR bills.comment LIKE '%' || :query || '%')
        ORDER BY bills.amount DESC
        """
    )
    fun observeByAmountDesc(apartmentId: Long, query: String, paidFilter: Boolean?): Flow<List<BillWithDetails>>

    @Transaction
    @Query(
        """
        SELECT bills.* FROM bills
        INNER JOIN utility_types ON bills.utilityTypeId = utility_types.id
        WHERE bills.apartmentId = :apartmentId
          AND (:paidFilter IS NULL OR bills.isPaid = :paidFilter)
          AND (utility_types.name LIKE '%' || :query || '%' OR bills.comment LIKE '%' || :query || '%')
        ORDER BY utility_types.name ASC
        """
    )
    fun observeByNameAsc(apartmentId: Long, query: String, paidFilter: Boolean?): Flow<List<BillWithDetails>>

    // --- Статистика ----------------------------------------------------------

    @Query(
        """
        SELECT utility_types.name AS utilityName, SUM(bills.amount) AS total
        FROM bills INNER JOIN utility_types ON bills.utilityTypeId = utility_types.id
        WHERE bills.apartmentId = :apartmentId AND bills.period = :period
        GROUP BY utility_types.id
        """
    )
    fun observeTotalsByUtility(apartmentId: Long, period: String): Flow<List<UtilityTotal>>

    @Query(
        """
        SELECT bills.period AS period, SUM(bills.amount) AS total
        FROM bills
        WHERE bills.apartmentId = :apartmentId
        GROUP BY bills.period
        ORDER BY bills.period ASC
        """
    )
    fun observeMonthlyTotals(apartmentId: Long): Flow<List<MonthlyTotal>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM bills WHERE apartmentId = :apartmentId AND isPaid = 1")
    fun observeTotalPaid(apartmentId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM bills WHERE apartmentId = :apartmentId AND isPaid = 0")
    fun observeTotalUnpaid(apartmentId: Long): Flow<Double>

    @Query("SELECT COALESCE(AVG(amount), 0) FROM bills WHERE apartmentId = :apartmentId")
    fun observeAverageBill(apartmentId: Long): Flow<Double>
}
