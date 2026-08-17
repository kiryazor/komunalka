package com.example.komunalka.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.komunalka.data.Apartment
import com.example.komunalka.data.Bill
import com.example.komunalka.data.UtilityType

/**
 * Счёт вместе с квартирой и типом услуги — то, что реально показывается
 * в списке и на экране деталей (JOIN трёх таблиц).
 */
data class BillWithDetails(
    @Embedded val bill: Bill,
    @Relation(parentColumn = "apartmentId", entityColumn = "id")
    val apartment: Apartment,
    @Relation(parentColumn = "utilityTypeId", entityColumn = "id")
    val utilityType: UtilityType
)
