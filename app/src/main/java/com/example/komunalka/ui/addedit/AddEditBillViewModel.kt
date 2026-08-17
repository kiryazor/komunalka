package com.example.komunalka.ui.addedit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.komunalka.data.Bill
import com.example.komunalka.data.UtilityType
import com.example.komunalka.repository.UtilityRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class AddEditBillViewModel(private val repository: UtilityRepository) : ViewModel() {

    val utilityTypes: LiveData<List<UtilityType>> = repository.observeUtilityTypes().asLiveData()

    private val _existingBill = MutableLiveData<Bill?>()
    val existingBill: LiveData<Bill?> = _existingBill

    private val _scanResult = MutableSharedFlow<String?>(replay = 0)
    val scanResult = _scanResult

    fun loadBill(billId: Long) {
        if (billId <= 0) {
            _existingBill.value = null
            return
        }
        viewModelScope.launch {
            _existingBill.value = repository.getBill(billId)
        }
    }

    fun save(bill: Bill, onSaved: () -> Unit) {
        viewModelScope.launch {
            if (bill.id == 0L) repository.addBill(bill) else repository.updateBill(bill)
            onSaved()
        }
    }

    /** Запускает распознавание фото счётчика и публикует результат подписчикам (форме). */
    fun onMeterPhotoScanned(reading: String?) {
        viewModelScope.launch { _scanResult.emit(reading) }
    }
}
