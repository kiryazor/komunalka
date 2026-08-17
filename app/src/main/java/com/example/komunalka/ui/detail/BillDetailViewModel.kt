package com.example.komunalka.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.komunalka.data.Bill
import com.example.komunalka.data.relations.BillWithDetails
import com.example.komunalka.data.relations.BillWithPayments
import com.example.komunalka.repository.UtilityRepository
import kotlinx.coroutines.launch

class BillDetailViewModel(private val repository: UtilityRepository, private val billId: Long) : ViewModel() {

    val billWithDetails: LiveData<BillWithDetails?> = repository.observeBillWithDetails(billId).asLiveData()
    val billWithPayments: LiveData<BillWithPayments?> = repository.observeBillWithPayments(billId).asLiveData()

    fun deleteBill(bill: Bill, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            onDeleted()
        }
    }

    fun addPayment(amount: Double, method: String) {
        viewModelScope.launch {
            repository.payForBill(billId, amount, method, System.currentTimeMillis())
        }
    }

    fun togglePaid(bill: Bill) {
        viewModelScope.launch {
            repository.setPaid(bill.id, !bill.isPaid)
        }
    }
}
