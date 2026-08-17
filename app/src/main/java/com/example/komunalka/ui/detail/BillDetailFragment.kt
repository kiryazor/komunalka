package com.example.komunalka.ui.detail

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.komunalka.KomunalkaApp
import com.example.komunalka.R
import com.example.komunalka.data.relations.BillWithDetails
import com.example.komunalka.databinding.DialogAddPaymentBinding
import com.example.komunalka.databinding.FragmentBillDetailBinding
import com.example.komunalka.utils.Formatters
import com.example.komunalka.utils.ServiceIcons
import com.example.komunalka.utils.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class BillDetailFragment : Fragment() {

    private var _binding: FragmentBillDetailBinding? = null
    private val binding get() = _binding!!
    private val args: BillDetailFragmentArgs by navArgs()

    private val paymentsAdapter = PaymentsAdapter()

    private val viewModel: BillDetailViewModel by viewModels {
        val app = requireActivity().application as KomunalkaApp
        ViewModelFactory { BillDetailViewModel(app.repository, args.billId) }
    }

    private var currentDetails: BillWithDetails? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvPayments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPayments.adapter = paymentsAdapter

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnEdit.setOnClickListener {
            val details = currentDetails ?: return@setOnClickListener
            findNavController().navigate(
                R.id.action_detail_to_addEdit,
                bundleOf("billId" to details.bill.id, "apartmentId" to details.bill.apartmentId)
            )
        }

        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnAddPayment.setOnClickListener { showAddPaymentDialog() }
        binding.btnTogglePaid.setOnClickListener {
            currentDetails?.bill?.let { viewModel.togglePaid(it) }
        }

        viewModel.billWithDetails.observe(viewLifecycleOwner) { details ->
            details ?: return@observe
            currentDetails = details
            bindDetails(details)
        }

        viewModel.billWithPayments.observe(viewLifecycleOwner) { withPayments ->
            val payments = withPayments?.payments.orEmpty().sortedByDescending { it.dateMillis }
            paymentsAdapter.submitList(payments)
            binding.tvEmptyPayments.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
            binding.rvPayments.visibility = if (payments.isEmpty()) View.GONE else View.VISIBLE

            val paidSum = payments.sumOf { it.totalAmount }
            val amount = currentDetails?.bill?.amount ?: 0.0
            val remaining = (amount - paidSum).coerceAtLeast(0.0)
            binding.tvRemaining.text = if (remaining <= 0.0) {
                getString(R.string.fully_paid)
            } else {
                getString(R.string.remaining_to_pay, Formatters.money(remaining))
            }
        }
    }

    private fun bindDetails(details: BillWithDetails) {
        val bill = details.bill
        binding.tvUtilityLetter.text = ServiceIcons.forKey(details.utilityType.iconKey)
        binding.tvApartmentPeriod.text = "${details.utilityType.name} · ${Formatters.periodLabel(bill.period)}"
        binding.tvAmount.text = "${Formatters.money(bill.amount)} ₽"

        val isOverdue = !bill.isPaid && bill.dueDateMillis < System.currentTimeMillis()
        binding.chipStatus.text = when {
            bill.isPaid -> getString(R.string.status_pill_paid)
            isOverdue -> getString(R.string.status_pill_overdue)
            else -> getString(R.string.status_pill_unpaid)
        }
        binding.btnTogglePaid.text = if (bill.isPaid) getString(R.string.action_unmark_paid) else getString(R.string.action_mark_paid)

        val hasReadings = bill.previousReading != null && bill.currentReading != null
        binding.cardReadings.visibility = if (hasReadings) View.VISIBLE else View.GONE
        if (hasReadings) {
            val prev = bill.previousReading!!
            val cur = bill.currentReading!!
            binding.tvReadingPrev.text = formatReading(prev)
            binding.tvReadingCur.text = formatReading(cur)
            binding.tvReadingDelta.text = formatReading(cur - prev)
        }

        binding.tvDueDate.text = Formatters.date(bill.dueDateMillis)

        val hasComment = !bill.comment.isNullOrBlank()
        binding.dividerComment.visibility = if (hasComment) View.VISIBLE else View.GONE
        binding.tvCommentLabel.visibility = if (hasComment) View.VISIBLE else View.GONE
        binding.tvComment.visibility = if (hasComment) View.VISIBLE else View.GONE
        if (hasComment) binding.tvComment.text = bill.comment

        val hasPhoto = !bill.meterPhotoPath.isNullOrBlank() && File(bill.meterPhotoPath).exists()
        binding.ivMeterPhoto.visibility = if (hasPhoto) View.VISIBLE else View.GONE
        binding.photoEmptyState.visibility = if (hasPhoto) View.GONE else View.VISIBLE
        if (hasPhoto) binding.ivMeterPhoto.setImageURI(Uri.fromFile(File(bill.meterPhotoPath!!)))
    }

    private fun formatReading(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun confirmDelete() {
        val details = currentDetails ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteBill(details.bill) { findNavController().navigateUp() }
            }
            .show()
    }

    private fun showAddPaymentDialog() {
        val dialogBinding = DialogAddPaymentBinding.inflate(LayoutInflater.from(requireContext()))
        val methods = listOf(
            getString(R.string.method_card), getString(R.string.method_cash), getString(R.string.method_online)
        )
        dialogBinding.dropdownPaymentMethod.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, methods)
        )
        dialogBinding.dropdownPaymentMethod.setText(methods.first(), false)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_add_payment)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val amount = dialogBinding.etPaymentAmount.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
                val method = dialogBinding.dropdownPaymentMethod.text?.toString()?.ifBlank { methods.first() }
                    ?: methods.first()
                if (amount != null && amount > 0) {
                    viewModel.addPayment(amount, method)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
