package com.example.komunalka.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.komunalka.data.UtilityType
import com.example.komunalka.databinding.ItemSettingsRowBinding
import com.example.komunalka.utils.Formatters

class UtilityTypesAdapter(
    private val onEdit: (UtilityType) -> Unit,
    private val onDelete: (UtilityType) -> Unit
) : ListAdapter<UtilityType, UtilityTypesAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettingsRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemSettingsRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(type: UtilityType) {
            binding.tvRowTitle.text = type.name
            binding.tvRowSubtitle.text = "${Formatters.money(type.tariff)} ₽ / ${type.unit}"
            binding.btnRowEdit.setOnClickListener { onEdit(type) }
            binding.btnRowDelete.setOnClickListener { onDelete(type) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UtilityType>() {
            override fun areItemsTheSame(oldItem: UtilityType, newItem: UtilityType) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: UtilityType, newItem: UtilityType) = oldItem == newItem
        }
    }
}
