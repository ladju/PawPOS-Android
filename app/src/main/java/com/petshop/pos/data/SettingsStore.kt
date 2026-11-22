package com.petshop.pos.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persisted app settings exposed as StateFlows, so every screen reads the same value and
 * settings survive rotation, process death and app restarts.
 * Uses the same preferences file as before, so the saved language is preserved.
 */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("pawpos_prefs", Context.MODE_PRIVATE)

    private val _darkTheme = MutableStateFlow(prefs.getBoolean(KEY_DARK, true))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _taxPercent = MutableStateFlow(prefs.getInt(KEY_TAX_PERCENT, 0).coerceIn(0, 100))
    /** Whole percent applied to every sale (0 = no tax, typical for UMKM). */
    val taxPercent: StateFlow<Int> = _taxPercent.asStateFlow()

    private val _qrisMdrRate = MutableStateFlow(prefs.getInt(KEY_MDR_BPS, 30) / 10_000.0)
    /** QRIS merchant discount rate as a fraction (0.003 = 0.3%). Only used for the settlement estimate in Recap. */
    val qrisMdrRate: StateFlow<Double> = _qrisMdrRate.asStateFlow()

    private val _languageCode = MutableStateFlow(prefs.getString(KEY_LANGUAGE, null) ?: "en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    fun setDarkTheme(value: Boolean) {
        _darkTheme.value = value
        prefs.edit().putBoolean(KEY_DARK, value).apply()
    }

    fun setTaxPercent(value: Int) {
        val v = value.coerceIn(0, 100)
        _taxPercent.value = v
        prefs.edit().putInt(KEY_TAX_PERCENT, v).apply()
    }

    fun setQrisMdrRate(rate: Double) {
        val bps = Math.round(rate.coerceIn(0.0, 0.1) * 10_000).toInt()
        _qrisMdrRate.value = bps / 10_000.0
        prefs.edit().putInt(KEY_MDR_BPS, bps).apply()
    }

    fun setLanguageCode(code: String) {
        _languageCode.value = code
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
    }

    private companion object {
        const val KEY_DARK = "dark_theme"
        const val KEY_TAX_PERCENT = "tax_percent"
        const val KEY_MDR_BPS = "qris_mdr_bps"
        const val KEY_LANGUAGE = "app_language"
    }
}
