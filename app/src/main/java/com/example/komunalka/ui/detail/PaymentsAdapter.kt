package com.example.komunalka.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.komunalka.data.Payment
import com.example.komunalka.databinding.ItemPaymentBinding
import com.example.komunalka.utils.Formatters

class PaymentsAdapter : ListAdapter<Payment, PaymentsAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(private val binding: ItemPaymentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(payment: Payment) {
            binding.tvPaymentAmount.text = "${Formatters.money(payment.totalAmount)} ₽"
            binding.tvPaymentMethod.text = payment.method
            binding.tvPaymentDate.text = Formatters.date(payment.dateMillis)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Payment>() {
            override fun areItemsTheSame(oldItem: Payment, newItem: Payment) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Payment, newItem: Payment) = oldItem == newItem
        }
    }
}
