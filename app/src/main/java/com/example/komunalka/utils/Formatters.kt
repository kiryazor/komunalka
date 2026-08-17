package com.example.komunalka.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

object Formatters {
    private val currencyFormat = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU"))
    private val periodMonthFormat = SimpleDateFormat("yyyy-MM", Locale("ru", "RU"))
    private val periodLabelFormat = SimpleDateFormat("LLLL yyyy", Locale("ru", "RU"))

    fun money(value: Double): String = currencyFormat.format(value)

    fun date(millis: Long): String = dateFormat.format(millis)

    fun currentPeriod(): String = periodMonthFormat.format(System.currentTimeMillis())

    /** "2026-09" -> "сентябрь 2026" */
    fun periodLabel(period: String): String = try {
        val date = periodMonthFormat.parse("$period")
        if (date != null) periodLabelFormat.format(date).replaceFirstChar { it.uppercase() } else period
    } catch (e: Exception) {
        period
    }
}
