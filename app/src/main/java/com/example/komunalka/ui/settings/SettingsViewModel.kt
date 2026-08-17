package com.example.komunalka.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.komunalka.data.Apartment
import com.example.komunalka.data.UtilityType
import com.example.komunalka.repository.UtilityRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: UtilityRepository) : ViewModel() {

    val apartments: LiveData<List<Apartment>> = repository.observeApartments().asLiveData()
    val utilityTypes: LiveData<List<UtilityType>> = repository.observeUtilityTypes().asLiveData()

    fun addApartment(name: String, address: String, area: Double?) {
        viewModelScope.launch { repository.addApartment(Apartment(name = name, address = address, areaSqm = area)) }
    }

    fun updateApartment(apartment: Apartment) {
        viewModelScope.launch { repository.updateApartment(apartment) }
    }

    fun deleteApartment(apartment: Apartment) {
        viewModelScope.launch { repository.deleteApartment(apartment) }
    }

    fun addUtilityType(name: String, unit: String, tariff: Double) {
        viewModelScope.launch { repository.addUtilityType(UtilityType(name = name, unit = unit, tariff = tariff)) }
    }

    fun updateUtilityType(type: UtilityType) {
        viewModelScope.launch { repository.updateUtilityType(type) }
    }

    fun deleteUtilityType(type: UtilityType) {
        viewModelScope.launch { repository.deleteUtilityType(type) }
    }
}
