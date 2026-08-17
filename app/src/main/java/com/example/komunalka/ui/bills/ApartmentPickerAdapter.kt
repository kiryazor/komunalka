package com.example.komunalka.ui.bills

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.komunalka.data.Apartment
import com.example.komunalka.databinding.ItemApartmentRowBinding

/**
 * Список квартир в нижнем листе — часть "изюминки": пользователь тапает
 * на оранжевую таблетку-селектор и мгновенно переключается на счета другой квартиры.
 */
class ApartmentPickerAdapter(
    private val apartments: List<Apartment>,
    private val selectedId: Long,
    private val onSelect: (Apartment) -> Unit
) : RecyclerView.Adapter<ApartmentPickerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemApartmentRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(apartments[position])

    override fun getItemCount() = apartments.size

    inner class ViewHolder(private val binding: ItemApartmentRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(apartment: Apartment) {
            binding.tvRowName.text = apartment.name
            binding.tvRowAddress.text = apartment.address
            binding.ivRowCheck.visibility = if (apartment.id == selectedId) android.view.View.VISIBLE else android.view.View.GONE
            binding.root.setOnClickListener { onSelect(apartment) }
        }
    }
}
