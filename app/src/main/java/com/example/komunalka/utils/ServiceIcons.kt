package com.example.komunalka.utils

/**
 * Эмодзи-иконки типов услуг — как в макете Figma. Ключ соответствует
 * UtilityType.iconKey, задаваемому при создании/сидировании типов услуг.
 */
object ServiceIcons {
    private val map = mapOf(
        "electricity" to "⚡",
        "water_cold" to "💧",
        "water_hot" to "🔥",
        "gas" to "🔵",
        "heating" to "🏠",
        "internet" to "🌐",
        "garbage" to "♻️"
    )

    fun forKey(key: String?): String = map[key] ?: "🧾"
}
