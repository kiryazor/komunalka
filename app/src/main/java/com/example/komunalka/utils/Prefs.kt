package com.example.komunalka.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Настройки приложения (экран "Настройки"): тема, сортировка по умолчанию,
 * единицы измерения воды, id последней выбранной квартиры.
 */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("komunalka_prefs", Context.MODE_PRIVATE)

    companion object {
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"

        const val UNITS_LITERS = "liters"
        const val UNITS_CUBIC = "cubic"
    }

    var theme: String
        get() = sp.getString("theme", THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) {
            sp.edit().putString("theme", value).apply()
            applyTheme(value)
        }

    var defaultSortIndex: Int
        get() = sp.getInt("default_sort", 0)
        set(value) = sp.edit().putInt("default_sort", value).apply()

    var waterUnits: String
        get() = sp.getString("water_units", UNITS_CUBIC) ?: UNITS_CUBIC
        set(value) = sp.edit().putString("water_units", value).apply()

    var lastApartmentId: Long
        get() = sp.getLong("last_apartment_id", -1L)
        set(value) = sp.edit().putLong("last_apartment_id", value).apply()

    fun applyTheme(value: String = theme) {
        val mode = when (value) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
