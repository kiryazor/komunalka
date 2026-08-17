package com.example.komunalka.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.komunalka.KomunalkaApp
import com.example.komunalka.R
import com.example.komunalka.data.Apartment
import com.example.komunalka.data.UtilityType
import com.example.komunalka.databinding.DialogAddApartmentBinding
import com.example.komunalka.databinding.DialogAddUtilityTypeBinding
import com.example.komunalka.databinding.FragmentSettingsBinding
import com.example.komunalka.utils.Prefs
import com.example.komunalka.utils.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private val apartmentsAdapter = ApartmentsAdapter(
        onEdit = { showApartmentDialog(it) },
        onDelete = { confirmDeleteApartment(it) }
    )
    private val utilityTypesAdapter = UtilityTypesAdapter(
        onEdit = { showUtilityTypeDialog(it) },
        onDelete = { viewModel.deleteUtilityType(it) }
    )

    private val viewModel: SettingsViewModel by viewModels {
        val app = requireActivity().application as KomunalkaApp
        ViewModelFactory { SettingsViewModel(app.repository) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = (requireActivity().application as KomunalkaApp).prefs

        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
            setDrawable(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.divider_thin)!!)
        }
        binding.rvApartments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApartments.adapter = apartmentsAdapter
        binding.rvApartments.addItemDecoration(divider)

        binding.rvUtilityTypes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUtilityTypes.adapter = utilityTypesAdapter
        binding.rvUtilityTypes.addItemDecoration(divider)

        setupThemeSwitch()
        setupSortRadio()
        setupUnitsRows()

        binding.btnAddApartment.setOnClickListener { showApartmentDialog(null) }
        binding.btnAddUtilityType.setOnClickListener { showUtilityTypeDialog(null) }

        viewModel.apartments.observe(viewLifecycleOwner) { apartmentsAdapter.submitList(it) }
        viewModel.utilityTypes.observe(viewLifecycleOwner) { utilityTypesAdapter.submitList(it) }
    }

    private fun setupThemeSwitch() {
        val isDark = prefs.theme == Prefs.THEME_DARK
        binding.switchDarkTheme.isChecked = isDark
        updateThemeLabel(isDark)
        binding.switchDarkTheme.setOnCheckedChangeListener { _, checked ->
            prefs.theme = if (checked) Prefs.THEME_DARK else Prefs.THEME_LIGHT
            updateThemeLabel(checked)
        }
    }

    private fun updateThemeLabel(isDark: Boolean) {
        binding.tvThemeIcon.text = if (isDark) "🌙" else "☀️"
        binding.tvThemeState.text = getString(if (isDark) R.string.theme_on else R.string.theme_off)
    }

    private fun setupSortRadio() {
        val checkedId = when (prefs.defaultSortIndex) {
            1 -> R.id.radio_sort_amount
            2 -> R.id.radio_sort_name
            else -> R.id.radio_sort_date
        }
        binding.radioSort.check(checkedId)
        binding.radioSort.setOnCheckedChangeListener { _, id ->
            prefs.defaultSortIndex = when (id) {
                R.id.radio_sort_amount -> 1
                R.id.radio_sort_name -> 2
                else -> 0
            }
        }
    }

    private fun setupUnitsRows() {
        fun refresh() {
            val isLiters = prefs.waterUnits == Prefs.UNITS_LITERS
            binding.ivCheckCubic.visibility = if (isLiters) View.GONE else View.VISIBLE
            binding.ivCheckLiters.visibility = if (isLiters) View.VISIBLE else View.GONE
        }
        refresh()
        binding.rowUnitsCubic.setOnClickListener { prefs.waterUnits = Prefs.UNITS_CUBIC; refresh() }
        binding.rowUnitsLiters.setOnClickListener { prefs.waterUnits = Prefs.UNITS_LITERS; refresh() }
    }

    private fun showApartmentDialog(existing: Apartment?) {
        val dialogBinding = DialogAddApartmentBinding.inflate(LayoutInflater.from(requireContext()))
        existing?.let {
            dialogBinding.etName.setText(it.name)
            dialogBinding.etAddress.setText(it.address)
            dialogBinding.etArea.setText(it.areaSqm?.toString().orEmpty())
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.settings_apartments else R.string.action_edit)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = dialogBinding.etName.text?.toString()?.trim().orEmpty()
                val address = dialogBinding.etAddress.text?.toString()?.trim().orEmpty()
                val area = dialogBinding.etArea.text?.toString()?.toDoubleOrNull()
                if (name.isBlank()) return@setPositiveButton
                if (existing == null) {
                    viewModel.addApartment(name, address, area)
                } else {
                    viewModel.updateApartment(existing.copy(name = name, address = address, areaSqm = area))
                }
            }
            .show()
    }

    private fun confirmDeleteApartment(apartment: Apartment) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete_apartment)
            .setMessage(R.string.confirm_delete_apartment_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteApartment(apartment) }
            .show()
    }

    private fun showUtilityTypeDialog(existing: UtilityType?) {
        val dialogBinding = DialogAddUtilityTypeBinding.inflate(LayoutInflater.from(requireContext()))
        existing?.let {
            dialogBinding.etTypeName.setText(it.name)
            dialogBinding.etTypeUnit.setText(it.unit)
            dialogBinding.etTypeTariff.setText(it.tariff.toString())
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.settings_utility_types else R.string.action_edit)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = dialogBinding.etTypeName.text?.toString()?.trim().orEmpty()
                val unit = dialogBinding.etTypeUnit.text?.toString()?.trim().orEmpty()
                val tariff = dialogBinding.etTypeTariff.text?.toString()?.toDoubleOrNull() ?: 0.0
                if (name.isBlank() || unit.isBlank()) return@setPositiveButton
                if (existing == null) {
                    viewModel.addUtilityType(name, unit, tariff)
                } else {
                    viewModel.updateUtilityType(existing.copy(name = name, unit = unit, tariff = tariff))
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
