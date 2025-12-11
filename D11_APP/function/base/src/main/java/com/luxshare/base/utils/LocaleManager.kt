package com.luxshare.base.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.Log
import com.luxshare.fastsp.FastSharedPreferences
import java.util.*

object LocaleManager {
    private const val TAG = "LocaleManager"
    private const val PREF_NAME = "AppLocalePreferences"
    private const val KEY_SELECTED_LANGUAGE = "selected_language"
    private const val DEFAULT_LANGUAGE = MultiLanguageUtils.CHINESE

    fun applyLocale(context: Context): Context {
        val language = getSelectedLanguage(context)
        Log.d(TAG, "Selected language: $language")
        return setAppLocale(context, language)
    }

    fun setAppLocale(context: Context, language: String): Context {
        Log.d(TAG, "Setting app locale: $language")
        if (getSelectedLanguage(context) == language) {
            return context
        }

        persistLanguage(context, language)
        return updateResources(context, language)
    }

    fun getSelectedLanguage(context: Context): String {
        return getPreferences(context).getString(KEY_SELECTED_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    private fun persistLanguage(context: Context, language: String) {
        getPreferences(context).edit()
            .putString(KEY_SELECTED_LANGUAGE, language)
            .apply()
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                updateResourcesNew(context, locale)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> {
                updateResourcesLegacy(context, locale)
            }
            else -> {
                updateResourcesPreJBMR1(context, locale)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun updateResourcesNew(context: Context, locale: Locale): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(context: Context, locale: Locale): Context {
        val resources = context.resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        return context
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesPreJBMR1(context: Context, locale: Locale): Context {
        val resources = context.resources
        val config = resources.configuration
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
        return context
    }
}