package com.example.komunalka.ui.bills

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.komunalka.data.Apartment
import com.example.komunalka.data.relations.BillWithDetails
import com.example.komunalka.repository.BillSort
import com.example.komunalka.repository.PaidFilter
import com.example.komunalka.repository.UtilityRepository
import com.example.komunalka.utils.Prefs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Ядро "изюминки" приложения: список счетов пересчитывается на лету
 * при переключении квартиры через выпадающий список — то есть у каждой
 * квартиры фактически свой, независимо фильтруемый и сортируемый список счетов.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BillsViewModel(
    private val repository: UtilityRepository,
    private val prefs: Prefs
) : ViewModel() {

    val apartments: LiveData<List<Apartment>> = repository.observeApartments().asLiveData()

    private val selectedApartmentId = MutableStateFlow(prefs.lastApartmentId)
    private val searchQuery = MutableStateFlow("")
    private val paidFilter = MutableStateFlow(PaidFilter.ALL)
    private val sort = MutableStateFlow(BillSort.entries.getOrElse(prefs.defaultSortIndex) { BillSort.DATE })

    val currentFilter: StateFlow<PaidFilter> = paidFilter
    val currentSort: StateFlow<BillSort> = sort
    val currentApartmentId: StateFlow<Long> = selectedApartmentId

    val bills: LiveData<List<BillWithDetails>> =
        combine(selectedApartmentId, searchQuery, paidFilter, sort) { apartmentId, query, filter, sortBy ->
            Quad(apartmentId, query, filter, sortBy)
        }.flatMapLatest { (apartmentId, query, filter, sortBy) ->
            if (apartmentId <= 0) flowOf(emptyList())
            else repository.observeBills(apartmentId, query, filter, sortBy)
        }.asLiveData()

    val totalAmount: LiveData<Double> = bills.map { list ->
        list.filter { !it.bill.isPaid }.sumOf { it.bill.amount }
    }

    fun selectApartment(apartmentId: Long) {
        selectedApartmentId.value = apartmentId
        prefs.lastApartmentId = apartmentId
    }

    /** Если сохранённой квартиры больше нет (или это первый запуск) — берём первую из списка. */
    fun ensureApartmentSelected(list: List<Apartment>) {
        if (list.isEmpty()) return
        val stillExists = list.any { it.id == selectedApartmentId.value }
        if (!stillExists) selectApartment(list.first().id)
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setFilter(filter: PaidFilter) {
        paidFilter.value = filter
    }

    fun setSort(sortBy: BillSort) {
        sort.value = sortBy
        prefs.defaultSortIndex = sortBy.ordinal
    }

    fun deleteBill(bill: com.example.komunalka.data.Bill) {
        viewModelScope.launch { repository.deleteBill(bill) }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
