package com.example.komunalka.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.komunalka.KomunalkaApp
import com.example.komunalka.R
import com.example.komunalka.databinding.FragmentStatisticsBinding
import com.example.komunalka.utils.Formatters
import com.example.komunalka.utils.ViewModelFactory
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

/**
 * Экран статистики. Показывает данные для той же квартиры, что выбрана
 * на экране счетов (id квартиры хранится в Prefs), поэтому статистика
 * тоже фактически "переключается" вместе с квартирой.
 */
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val chartColors = listOf(
        R.color.chart_1, R.color.chart_2, R.color.chart_3,
        R.color.chart_4, R.color.chart_5, R.color.chart_6
    )

    private val viewModel: StatisticsViewModel by viewModels {
        val app = requireActivity().application as KomunalkaApp
        ViewModelFactory { StatisticsViewModel(app.repository) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()

        viewModel.totalsByUtility.observe(viewLifecycleOwner) { totals ->
            val hasData = totals.isNotEmpty()
            binding.groupSummary.visibility = if (hasData) View.VISIBLE else View.GONE
            binding.emptyState.visibility = if (hasData) View.GONE else View.VISIBLE
            bindPieChart(totals.map { it.utilityName to it.total })
        }

        viewModel.monthlyTotals.observe(viewLifecycleOwner) { months ->
            bindBarChart(months.map { Formatters.periodLabel(it.period) to it.total })
        }

        viewModel.totalPaid.observe(viewLifecycleOwner) { binding.tvTotalPaid.text = "${Formatters.money(it)} ₽" }
        viewModel.totalUnpaid.observe(viewLifecycleOwner) { binding.tvTotalUnpaid.text = "${Formatters.money(it)} ₽" }
        viewModel.averageBill.observe(viewLifecycleOwner) { binding.tvAvgBill.text = "${Formatters.money(it)} ₽" }
        viewModel.forecastNextMonth.observe(viewLifecycleOwner) { forecast ->
            val text = if (forecast == null) "—" else "${Formatters.money(forecast)} ₽"
            binding.tvForecast.text = text
            binding.tvForecastKpi.text = text
        }
    }

    override fun onResume() {
        super.onResume()
        // Экран может открываться после того, как пользователь переключил
        // квартиру на экране счетов — подхватываем актуальный выбор.
        val app = requireActivity().application as KomunalkaApp
        val id = app.prefs.lastApartmentId
        if (id > 0) viewModel.setApartment(id)
    }

    private fun setupCharts() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            legend.apply {
                orientation = Legend.LegendOrientation.VERTICAL
                verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                textSize = 12f
            }
            holeRadius = 55f
            transparentCircleRadius = 58f
            setEntryLabelColor(Color.TRANSPARENT)
        }

        binding.barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(true)
        }
    }

    private fun bindPieChart(data: List<Pair<String, Double>>) {
        if (data.isEmpty()) {
            binding.pieChart.clear()
            return
        }
        val entries = data.map { PieEntry(it.second.toFloat(), it.first) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = chartColors.map { requireContext().getColor(it) }
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.invalidate()
    }

    private fun bindBarChart(data: List<Pair<String, Double>>) {
        if (data.isEmpty()) {
            binding.barChart.clear()
            return
        }
        val entries = data.mapIndexed { index, pair -> BarEntry(index.toFloat(), pair.second.toFloat()) }
        val dataSet = BarDataSet(entries, "").apply {
            color = requireContext().getColor(R.color.orange_500)
            valueTextSize = 10f
        }
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.first })
        binding.barChart.data = BarData(dataSet).apply { barWidth = 0.6f }
        binding.barChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
