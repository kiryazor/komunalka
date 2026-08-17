package com.example.komunalka.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.komunalka.data.dao.MonthlyTotal
import com.example.komunalka.data.dao.UtilityTotal
import com.example.komunalka.repository.UtilityRepository
import com.example.komunalka.utils.Formatters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(private val repository: UtilityRepository) : ViewModel() {

    private val apartmentId = MutableStateFlow(-1L)

    val totalsByUtility: LiveData<List<UtilityTotal>> = apartmentId.flatMapLatest { id ->
        if (id <= 0) flowOf(emptyList()) else repository.observeTotalsByUtility(id, Formatters.currentPeriod())
    }.asLiveData()

    val monthlyTotals: LiveData<List<MonthlyTotal>> = apartmentId.flatMapLatest { id ->
        if (id <= 0) flowOf(emptyList()) else repository.observeMonthlyTotals(id)
    }.asLiveData()

    val totalPaid: LiveData<Double> = apartmentId.flatMapLatest { id ->
        if (id <= 0) flowOf(0.0) else repository.observeTotalPaid(id)
    }.asLiveData()

    val totalUnpaid: LiveData<Double> = apartmentId.flatMapLatest { id ->
        if (id <= 0) flowOf(0.0) else repository.observeTotalUnpaid(id)
    }.asLiveData()

    val averageBill: LiveData<Double> = apartmentId.flatMapLatest { id ->
        if (id <= 0) flowOf(0.0) else repository.observeAverageBill(id)
    }.asLiveData()

    /** "Изюминка" из рекомендации темы — прогноз на следующий месяц по среднему за последние периоды. */
    val forecastNextMonth: LiveData<Double?> = MediatorLiveData<Double?>().apply {
        addSource(monthlyTotals) { months ->
            value = if (months.isEmpty()) null else {
                val lastThree = months.takeLast(3)
                lastThree.sumOf { it.total } / lastThree.size
            }
        }
    }

    fun setApartment(id: Long) {
        if (apartmentId.value != id) apartmentId.value = id
    }
}
