package com.example.komunalka.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.komunalka.data.Apartment
import com.example.komunalka.databinding.ItemSettingsRowBinding

class ApartmentsAdapter(
    private val onEdit: (Apartment) -> Unit,
    private val onDelete: (Apartment) -> Unit
) : ListAdapter<Apartment, ApartmentsAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettingsRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemSettingsRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(apartment: Apartment) {
            binding.tvRowTitle.text = apartment.name
            binding.tvRowSubtitle.text = apartment.address
            binding.btnRowEdit.setOnClickListener { onEdit(apartment) }
            binding.btnRowDelete.setOnClickListener { onDelete(apartment) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Apartment>() {
            override fun areItemsTheSame(oldItem: Apartment, newItem: Apartment) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Apartment, newItem: Apartment) = oldItem == newItem
        }
    }
}
