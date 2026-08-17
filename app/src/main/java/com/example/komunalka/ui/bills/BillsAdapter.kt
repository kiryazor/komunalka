package com.example.komunalka.ui.bills

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.komunalka.R
import com.example.komunalka.data.relations.BillWithDetails
import com.example.komunalka.databinding.ItemBillBinding
import com.example.komunalka.utils.Formatters
import com.example.komunalka.utils.ServiceIcons

class BillsAdapter(
    private val onClick: (BillWithDetails) -> Unit
) : ListAdapter<BillWithDetails, BillsAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemBillBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BillWithDetails) {
            val ctx = binding.root.context
            binding.tvUtilityLetter.text = ServiceIcons.forKey(item.utilityType.iconKey)
            binding.tvUtilityName.text = item.utilityType.name
            binding.tvPeriod.text =
                "${Formatters.periodLabel(item.bill.period)} · до ${Formatters.date(item.bill.dueDateMillis)}"
            binding.tvAmount.text = "${Formatters.money(item.bill.amount)} ₽"

            if (item.bill.isPaid) {
                binding.chipStatus.text = ctx.getString(R.string.status_paid)
                binding.chipStatus.setChipBackgroundColorResource(R.color.success_green_bg)
                binding.chipStatus.setTextColor(ctx.getColor(R.color.success_green))
            } else if (item.bill.dueDateMillis < System.currentTimeMillis()) {
                binding.chipStatus.text = ctx.getString(R.string.status_overdue)
                binding.chipStatus.setChipBackgroundColorResource(R.color.warning_amber_bg)
                binding.chipStatus.setTextColor(ctx.getColor(R.color.warning_amber))
            } else {
                binding.chipStatus.text = ctx.getString(R.string.status_unpaid)
                binding.chipStatus.setChipBackgroundColorResource(R.color.orange_100)
                binding.chipStatus.setTextColor(ctx.getColor(R.color.orange_700))
            }

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BillWithDetails>() {
            override fun areItemsTheSame(oldItem: BillWithDetails, newItem: BillWithDetails) =
                oldItem.bill.id == newItem.bill.id

            override fun areContentsTheSame(oldItem: BillWithDetails, newItem: BillWithDetails) =
                oldItem == newItem
        }
    }
}
