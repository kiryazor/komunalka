package com.example.komunalka.ui.bills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.komunalka.KomunalkaApp
import com.example.komunalka.R
import com.example.komunalka.data.Apartment
import com.example.komunalka.databinding.FragmentBillsListBinding
import com.example.komunalka.repository.BillSort
import com.example.komunalka.repository.PaidFilter
import com.example.komunalka.utils.Formatters
import com.example.komunalka.utils.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog

class BillsListFragment : Fragment() {

    private var _binding: FragmentBillsListBinding? = null
    private val binding get() = _binding!!

    private var apartmentsList: List<Apartment> = emptyList()
    private val adapter = BillsAdapter { _ ->
        // TODO: экран деталей счёта пока в разработке
        android.widget.Toast.makeText(requireContext(), "Экран в разработке", android.widget.Toast.LENGTH_SHORT).show()
    }

    private val viewModel: BillsViewModel by viewModels {
        val app = requireActivity().application as KomunalkaApp
        ViewModelFactory { BillsViewModel(app.repository, app.prefs) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvBills.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBills.adapter = adapter

        binding.fabAdd.setOnClickListener {
            // TODO: экран добавления счёта пока в разработке
            android.widget.Toast.makeText(requireContext(), "Экран в разработке", android.widget.Toast.LENGTH_SHORT).show()
        }

        binding.etSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                binding.chipPaid.id -> PaidFilter.PAID
                binding.chipUnpaid.id -> PaidFilter.UNPAID
                else -> PaidFilter.ALL
            }
            viewModel.setFilter(filter)
            updateSectionTitle(filter)
        }

        binding.btnSort.setOnClickListener { showSortMenu() }

        // ===== Изюминка: оранжевая "таблетка"-селектор открывает список квартир =====
        binding.btnAptSelector.setOnClickListener { showApartmentPicker() }

        viewModel.apartments.observe(viewLifecycleOwner) { apartments ->
            apartmentsList = apartments
            viewModel.ensureApartmentSelected(apartments)
            val selectedId = viewModel.currentApartmentId.value
            val selected = apartments.firstOrNull { it.id == selectedId } ?: apartments.firstOrNull()
            selected?.let {
                binding.tvAptName.text = it.name
                binding.tvAptAddress.text = it.address
            }
        }

        viewModel.bills.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.rvBills.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            binding.tvSectionCount.text = list.size.toString()

            val unpaid = list.filter { !it.bill.isPaid }
            if (unpaid.isNotEmpty()) {
                binding.cardHeroUnpaid.visibility = View.VISIBLE
                val total = unpaid.sumOf { it.bill.amount }
                binding.tvHeroAmount.text = "${Formatters.money(total)} ₽"
                binding.tvHeroCount.text = getString(R.string.hero_unpaid_bills_count, unpaid.size)
            } else {
                binding.cardHeroUnpaid.visibility = View.GONE
            }
        }

        updateSectionTitle(viewModel.currentFilter.value)
    }

    private fun updateSectionTitle(filter: PaidFilter) {
        binding.tvSectionTitle.text = getString(
            when (filter) {
                PaidFilter.ALL -> R.string.section_all_bills
                PaidFilter.PAID -> R.string.section_paid_bills
                PaidFilter.UNPAID -> R.string.section_unpaid_bills
            }
        )
    }

    private fun showApartmentPicker() {
        if (apartmentsList.isEmpty()) return
        val dialog = BottomSheetDialog(requireContext())
        val listView = androidx.recyclerview.widget.RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            val vPad = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, vPad, 0, vPad)
            clipToPadding = false
        }
        val selectedId = viewModel.currentApartmentId.value
        listView.adapter = ApartmentPickerAdapter(apartmentsList, selectedId) { apartment ->
            viewModel.selectApartment(apartment.id)
            binding.tvAptName.text = apartment.name
            binding.tvAptAddress.text = apartment.address
            dialog.dismiss()
        }
        dialog.setContentView(listView)
        dialog.show()
    }

    private fun showSortMenu() {
        val popup = PopupMenu(requireContext(), binding.btnSort)
        popup.menu.add(0, 0, 0, R.string.sort_date)
        popup.menu.add(0, 1, 1, R.string.sort_amount)
        popup.menu.add(0, 2, 2, R.string.sort_name)
        popup.setOnMenuItemClickListener { item ->
            val sort = when (item.itemId) {
                1 -> BillSort.AMOUNT
                2 -> BillSort.NAME
                else -> BillSort.DATE
            }
            viewModel.setSort(sort)
            true
        }
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
