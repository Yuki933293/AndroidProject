package com.luxshare.base.utils

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.core.os.ConfigurationCompat
import com.luxshare.fastsp.FastSharedPreferences
import java.util.Locale


/**
 *  * 多语言切换工具类
 *
 * @author chence
 * @time 2023年10月25日17:17:43
 *
 * 使用步骤：
 * 1、在activity基类中，实现EventBus的注册和消息监听
 *      override fun onCreate(savedInstanceState: Bundle?) {
 *          super.onCreate(savedInstanceState)
 *          if (!EventBus.getDefault().isRegistered(this)){
 *              EventBus.getDefault().register(this)
 *          }
 *      }
 *      
 *      @Subscribe(threadMode = ThreadMode.MAIN)
 *      fun onMessageEvent(msg: MessageEvent) {
 *          when (msg.message) {
 *              MultiLanguageUtils.SWITCH_LANGUAGE -> {
 *                  MultiLanguageUtils.changeLanguage(this)
 *                  recreate() //刷新界面
 *              }
 *          }
 *      }
 *      
 *      override fun onDestroy() {
 *          super.onDestroy()
 *          if (EventBus.getDefault().isRegistered(this)){
 *              EventBus.getDefault().unregister(this)
 *          }
 *      }
 *      
 * 2、调用 MultiLanguageUtils.changeLanguage(context: Context, language: String, country: String) 保存并切换语言;
 * 3. 调用 EventBus.getDefault().post(MessageEvent(MultiLanguageUtils.SWITCH_LANGUAGE)) 切换所有activity语言
 */

object MultiLanguageUtils {
    const val TAG = "MultiLanguageUtils"
    const val SWITCH_LANGUAGE:String = "switch_language"

    const val LANGUAGE = "language"
    const val COUNTRY = "country"
    const val CHINESE = "zh"
    const val CHINA = "CN"
    const val TAIWAN = "TW"
    const val ENGLISH = "en"
    const val US = "US"
    const val FOLLOW_SYSTEM = ""

    const val LANGUAGE_TAG = "language_tag"

    /**
     * 根据保存的数据修改应用内语言设置
     *
     */
    fun changeLanguage(context: Context) {
        val languageSharedPreferences = FastSharedPreferences.get(LANGUAGE)
        val language = languageSharedPreferences.getString(LANGUAGE, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM
        val country = languageSharedPreferences.getString(COUNTRY, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM
        if (language == FOLLOW_SYSTEM || country == FOLLOW_SYSTEM) {
            //如果语言和地区任一是跟随系统，跟随系统
            //获取手机系统语言,跟随系统
            val locale: Locale = getSystemLanguage()
            changeAppLanguage(context, locale)
        } else {
            //不跟随系统，则修改app语言
            changeAppLanguage(context, Locale(language, country))
        }
    }

    /**
     * 保存并修改应用内语言设置
     *
     * @param language    语言  zh/en
     * @param country      地区
     */
    fun changeLanguage(context: Context, language: String, country: String) {
        Log.i(TAG, "language=$language; country=$country")
        val languageSharedPreferences = FastSharedPreferences.get(LANGUAGE)
        if (language == FOLLOW_SYSTEM || country == FOLLOW_SYSTEM) {
            //如果语言和地区任一是跟随系统，跟随系统
            languageSharedPreferences.edit().putString(LANGUAGE, FOLLOW_SYSTEM).commit()
            languageSharedPreferences.edit().putString(COUNTRY, FOLLOW_SYSTEM).commit()

            //获取手机系统语言,跟随系统
            val locale: Locale = getSystemLanguage()
            changeAppLanguage(context, locale)
        } else {
            languageSharedPreferences.edit().putString(LANGUAGE, language).commit()
            languageSharedPreferences.edit().putString(COUNTRY, country).commit()
            //不跟随系统，则修改app语言
            changeAppLanguage(context, Locale(language, country))
        }
    }

    /**
     * 更改应用语言
     *
     * @param
     * @param locale      语言地区
     */
    private fun changeAppLanguage(context: Context, locale: Locale) {
        Locale.setDefault(locale)

//        val resources = context.resources
//        val configuration = resources.configuration
//        configuration.setLocale(locale)
//        Log.i(TAG, "locale=$locale")
//        resources.updateConfiguration(configuration, resources.displayMetrics)

        // 获取应用的Resources
        val appResources = context.applicationContext.resources
        val appConfiguration = appResources.configuration
        appConfiguration.setLocale(locale)

        // 更新Application的资源配置
        appResources.updateConfiguration(appConfiguration, appResources.displayMetrics)

        // 如果传入的是Activity Context，也更新它的资源配置
        if (context != context.applicationContext) {
            val activityResources = context.resources
            val activityConfiguration = activityResources.configuration
            activityConfiguration.setLocale(locale)
            activityResources.updateConfiguration(activityConfiguration, activityResources.displayMetrics)
        }

        Log.i(TAG, "locale=$locale")
    }

    /**
     * 获取系统语言
     */
    fun getSystemLanguage(): Locale {
        val configuration = Resources.getSystem().configuration
        return ConfigurationCompat.getLocales(configuration).get(0) ?: Locale(CHINESE)
    }

    /**
     * 获取应用语言
     */
    private fun getAppLocale(context: Context): Locale {
        val configuration = context.resources.configuration
        return ConfigurationCompat.getLocales(configuration).get(0) ?: Locale(CHINESE)
    }


    /**
     * 判断sp中和app中的多语言信息是否相同
     */
    private fun isSameWithSetting(context: Context): Boolean {
        val systemLocale = getSystemLanguage()
        val appLocale = getAppLocale(context)

        val languageSharedPreferences = FastSharedPreferences.get(LANGUAGE)
        val language = languageSharedPreferences.getString(LANGUAGE, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM
        val country = languageSharedPreferences.getString(COUNTRY, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM

        return if (language == FOLLOW_SYSTEM || country == FOLLOW_SYSTEM) {
            systemLocale.language == appLocale.language && systemLocale.country == appLocale.country
        } else {
            language == appLocale.language && country == appLocale.country
        }
    }


    private fun createConfigurationResources(context: Context): Context {
        val resources = context.resources
        val configuration = resources.configuration
        val appLocale = getAppLocale(context)

        Log.d("TAG", "attachBaseContext:")
        //如果本地有语言信息，以本地为主，如果本地没有使用默认Locale
        val languageSharedPreferences = FastSharedPreferences.get(LANGUAGE)
        val language = languageSharedPreferences.getString(LANGUAGE, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM
        val country = languageSharedPreferences.getString(COUNTRY, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM
        val locale = if (language == FOLLOW_SYSTEM || country == FOLLOW_SYSTEM) {
            appLocale
        } else {
            Locale(language, country)
        }

        configuration.setLocale(locale)

        return context.createConfigurationContext(configuration)
    }


    /**
     * 设置语言
     *
     * @param context
     */
    private fun setConfiguration(context: Context) {
        val appLocale: Locale = getAppLocale(context)

        //如果本地有语言信息，以本地为主，如果本地没有使用默认Locale
        val languageSharedPreferences = FastSharedPreferences.get(LANGUAGE)
        val language = languageSharedPreferences.getString(LANGUAGE, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM
        val country = languageSharedPreferences.getString(COUNTRY, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM
        val locale = if (language == FOLLOW_SYSTEM || country == FOLLOW_SYSTEM) {
            appLocale
        } else {
            Locale(language, country)
        }
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics) //语言更换生效的代码
    }
}
