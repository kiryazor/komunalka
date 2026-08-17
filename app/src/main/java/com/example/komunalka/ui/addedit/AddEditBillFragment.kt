package com.example.komunalka.ui.addedit

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.komunalka.KomunalkaApp
import com.example.komunalka.R
import com.example.komunalka.data.Bill
import com.example.komunalka.data.UtilityType
import com.example.komunalka.databinding.FragmentAddEditBillBinding
import com.example.komunalka.utils.Formatters
import com.example.komunalka.utils.MeterScanner
import com.example.komunalka.utils.ViewModelFactory
import com.google.android.material.datepicker.MaterialDatePicker
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class AddEditBillFragment : Fragment() {

    private var _binding: FragmentAddEditBillBinding? = null
    private val binding get() = _binding!!
    private val args: AddEditBillFragmentArgs by navArgs()

    private val viewModel: AddEditBillViewModel by viewModels {
        val app = requireActivity().application as KomunalkaApp
        ViewModelFactory { AddEditBillViewModel(app.repository) }
    }

    private var utilityTypes: List<UtilityType> = emptyList()
    private var selectedUtilityTypeId: Long = -1
    private var dueDateMillis: Long = System.currentTimeMillis()
    private var meterPhotoUri: Uri? = null
    private var meterPhotoPath: String? = null
    private var pendingBill: Bill? = null
    private var formPrefilled = false

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera() else
                Toast.makeText(requireContext(), R.string.scan_camera_denied, Toast.LENGTH_LONG).show()
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            // Сразу после съёмки — на обрезку: пользователь выделяет только табло счётчика,
            // это отсекает серийные номера и прочие цифры, которые сбивали распознавание.
            if (success) meterPhotoUri?.let { launchCrop(it) }
        }

    private val cropPhoto =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val cropped = if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
                UCrop.getOutput(data)
            } else null

            if (cropped != null) {
                meterPhotoPath = cropped.path
                meterPhotoUri = cropped
                runOcr(cropped)
            } else {
                // Пользователь отменил обрезку — всё равно пробуем распознать по исходному фото.
                meterPhotoUri?.let { runOcr(it) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditBillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text = getString(
            if (args.billId > 0) R.string.title_edit_bill else R.string.title_add_bill
        )
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }

        updateDueDateField()
        binding.etDueDate.setOnClickListener { showDatePicker() }
        binding.tilDueDate.setEndIconOnClickListener { showDatePicker() }

        binding.btnScanMeter.setOnClickListener { onScanMeterClicked() }

        viewModel.utilityTypes.observe(viewLifecycleOwner) { types ->
            utilityTypes = types
            val names = types.map { "${it.name} (${it.unit})" }
            binding.dropdownUtilityType.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
            )
            binding.dropdownUtilityType.setOnItemClickListener { _, _, position, _ ->
                selectedUtilityTypeId = types[position].id
            }
            if (selectedUtilityTypeId == -1L && types.isNotEmpty() && args.billId <= 0) {
                selectedUtilityTypeId = types.first().id
                binding.dropdownUtilityType.setText(names.first(), false)
            }
            applyPendingBillIfReady()
        }

        viewModel.existingBill.observe(viewLifecycleOwner) { existing ->
            pendingBill = existing
            applyPendingBillIfReady()
        }
        viewModel.loadBill(args.billId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scanResult.collect { reading ->
                if (reading != null) {
                    binding.etCurrentReading.setText(reading)
                    Toast.makeText(requireContext(), getString(R.string.scan_recognized, reading), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), R.string.scan_not_recognized, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnSave.setOnClickListener { onSaveClicked() }
    }

    /** Заполняет форму данными существующего счёта — только один раз и только когда готовы оба источника. */
    private fun applyPendingBillIfReady() {
        if (formPrefilled) return
        val existing = pendingBill ?: return
        if (utilityTypes.isEmpty()) return

        formPrefilled = true
        selectedUtilityTypeId = existing.utilityTypeId
        val typeIndex = utilityTypes.indexOfFirst { it.id == existing.utilityTypeId }
        if (typeIndex >= 0) {
            binding.dropdownUtilityType.setText(
                "${utilityTypes[typeIndex].name} (${utilityTypes[typeIndex].unit})", false
            )
        }
        binding.etPeriod.setText(existing.period)
        binding.etPreviousReading.setText(existing.previousReading?.toString().orEmpty())
        binding.etCurrentReading.setText(existing.currentReading?.toString().orEmpty())
        binding.etAmount.setText(existing.amount.toString())
        dueDateMillis = existing.dueDateMillis
        updateDueDateField()
        binding.etComment.setText(existing.comment.orEmpty())
        meterPhotoPath = existing.meterPhotoPath
        meterPhotoPath?.let { path -> if (File(path).exists()) showSavedPhoto(path) }
    }

    private fun showSavedPhoto(path: String) {
        binding.ivMeterPhoto.visibility = View.VISIBLE
        binding.photoPlaceholder.visibility = View.GONE
        binding.ivMeterPhoto.setImageURI(Uri.fromFile(File(path)))
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(dueDateMillis)
            .setTitleText(R.string.label_due_date)
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            dueDateMillis = selection
            updateDueDateField()
        }
        picker.show(childFragmentManager, "due_date_picker")
    }

    private fun updateDueDateField() {
        binding.etDueDate.setText(Formatters.date(dueDateMillis))
    }

    // ===== Изюминка: сканирование показаний счётчика по фото =====

    private fun onScanMeterClicked() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) launchCamera() else requestCameraPermission.launch(android.Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val photosDir = File(requireContext().getExternalFilesDir(null), "meter_photos").apply { mkdirs() }
        val file = File(photosDir, "meter_${System.currentTimeMillis()}.jpg")
        meterPhotoPath = file.absolutePath
        meterPhotoUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        takePicture.launch(meterPhotoUri)
    }

    private fun launchCrop(sourceUri: Uri) {
        val photosDir = File(requireContext().getExternalFilesDir(null), "meter_photos").apply { mkdirs() }
        val destFile = File(photosDir, "meter_cropped_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)

        val options = UCrop.Options().apply {
            setToolbarTitle(getString(R.string.crop_meter_title))
            setToolbarColor(requireContext().getColor(R.color.orange_500))
            setStatusBarColor(requireContext().getColor(R.color.orange_700))
            setActiveControlsWidgetColor(requireContext().getColor(R.color.orange_500))
            setToolbarWidgetColor(requireContext().getColor(R.color.white))
            setFreeStyleCropEnabled(true)
            setHideBottomControls(false)
            setCompressionQuality(95)
        }

        val intent = UCrop.of(sourceUri, destUri)
            .withOptions(options)
            .getIntent(requireContext())
        cropPhoto.launch(intent)
    }

    private fun runOcr(uri: Uri) {
        binding.ivMeterPhoto.visibility = View.VISIBLE
        binding.photoPlaceholder.visibility = View.GONE
        binding.ivMeterPhoto.setImageURI(uri)
        binding.tvScanHint.visibility = View.VISIBLE
        binding.tvScanHint.setText(R.string.scan_hint)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = MeterScanner.recognizeReading(requireContext(), uri)
            viewModel.onMeterPhotoScanned(result.recognizedValue)
        }
    }

    // ===== Сохранение с валидацией =====

    private fun onSaveClicked() {
        var valid = true
        if (binding.etPeriod.text.isNullOrBlank()) {
            binding.tilPeriod.error = getString(R.string.error_field_required); valid = false
        } else binding.tilPeriod.error = null

        val amount = binding.etAmount.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
        if (amount == null) {
            binding.tilAmount.error = getString(R.string.error_amount_invalid); valid = false
        } else binding.tilAmount.error = null

        if (selectedUtilityTypeId <= 0) {
            binding.tilUtilityType.error = getString(R.string.error_field_required); valid = false
        } else binding.tilUtilityType.error = null

        if (!valid) return

        val bill = Bill(
            id = args.billId.takeIf { it > 0 } ?: 0L,
            apartmentId = pendingBill?.apartmentId ?: args.apartmentId,
            utilityTypeId = selectedUtilityTypeId,
            period = binding.etPeriod.text.toString().trim(),
            previousReading = binding.etPreviousReading.text?.toString()?.toDoubleOrNull(),
            currentReading = binding.etCurrentReading.text?.toString()?.toDoubleOrNull(),
            amount = amount!!,
            dueDateMillis = dueDateMillis,
            isPaid = pendingBill?.isPaid ?: false,
            meterPhotoPath = meterPhotoPath,
            comment = binding.etComment.text?.toString()?.trim()?.ifBlank { null }
        )

        viewModel.save(bill) {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
