package com.pennywiseai.tracker.locale

import android.app.Activity
import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.text.Collator
import java.util.Locale

/**
 * The in-app language choice: Settings → Language and the onboarding picker
 * both read [options] and write through [set], so they can't drift apart.
 *
 * Android 13+ stores the choice in the system's per-app locale
 * ([LocaleManager]); the OS persists it, recreates the activities and shows
 * the same list under System settings → App languages. Older versions have no
 * per-app locale, so the tag is kept in a small private preferences file and
 * applied to each activity's base context ([wrap]) and the application's
 * resources ([applyToApplication]). Text built by workers and notifications
 * reads the application resources, so it follows the choice from the next
 * process start on those versions.
 */
object AppLanguage {

    /**
     * BCP-47 tags offered besides "follow the device", one per `values-*`
     * folder that is complete enough to ship. `res/xml/locales_config.xml`
     * declares the same set to the system; `AppLanguageConfigTest` keeps the
     * two in step and fails if a listed locale's translations fall below the
     * coverage bar. Locales with a folder but too few strings (ne-NP, th-TH,
     * be-BY, es, sw-KE) stay out until they are filled in.
     */
    val supportedTags: List<String> = listOf(
        "en-US",
        "ar-SA",
        "bn",
        "cs-CZ",
        "de-DE",
        "fr-FR",
        "gu",
        "hi-IN",
        "mr",
        "pt-BR",
        "pt-PT",
        "ru-RU",
        "ta",
        "te",
    )

    /** One row of the picker. [tag] null means "follow the device language". */
    data class Option(val tag: String?, val nativeName: String)

    /**
     * "System default" first, then each language in its own name and script
     * (हिन्दी, தமிழ்), sorted for the current locale. The region shows only
     * where one language has several variants (pt-BR / pt-PT). [current] is
     * kept even when it isn't offered (a language picked earlier under System
     * settings), so the picker can still show what's active.
     */
    fun options(current: Locale?): List<Option> {
        val offered = supportedTags.map(Locale::forLanguageTag)
        val extra = current?.takeIf { matchTag(it) == null }
        val all = offered + listOfNotNull(extra)
        val multiRegion = all.groupBy { it.language }.filterValues { it.size > 1 }.keys
        val collator = Collator.getInstance()
        return listOf(Option(null, "")) + all
            .map { Option(it.toLanguageTag(), nativeName(it, it.language in multiRegion)) }
            .sortedWith(compareBy(collator) { it.nativeName })
    }

    /** The tag of the [options] row [current] selects; null for "System default". */
    fun selectedTag(current: Locale?): String? =
        current?.let { matchTag(it) ?: it.toLanguageTag() }

    /** [locale]'s name in its own language, e.g. "मराठी" or "Português (Brasil)". */
    fun nativeName(locale: Locale, withRegion: Boolean = false): String {
        val name = if (withRegion && locale.country.isNotEmpty()) {
            locale.getDisplayName(locale)
        } else {
            locale.getDisplayLanguage(locale)
        }
        return name.replaceFirstChar { it.titlecase(locale) }
    }

    /** The offered tag that [locale] maps to: exact first, then same language. */
    internal fun matchTag(locale: Locale): String? {
        val tag = locale.toLanguageTag()
        return supportedTags.firstOrNull { it.equals(tag, ignoreCase = true) }
            ?: supportedTags.firstOrNull { Locale.forLanguageTag(it).language == locale.language }
    }

    /** The app-specific language, or null when the app follows the device. */
    fun current(context: Context): Locale? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales?.takeUnless { it.isEmpty }?.get(0)
        } else {
            storedTag(context)?.let(Locale::forLanguageTag)
        }

    /**
     * Switch the app to [tag] (null = follow the device). On Android 13+ the
     * system recreates the activities itself; older versions store the tag
     * and recreate [activity] so it re-reads its resources.
     */
    fun set(activity: Activity, tag: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales =
                tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
        } else {
            if (tag == storedTag(activity)) return
            prefs(activity).edit().apply {
                if (tag == null) remove(KEY_TAG) else putString(KEY_TAG, tag)
            }.apply()
            applyToApplication(activity.application)
            activity.recreate()
        }
    }

    /**
     * Pre-13 only: [base] with the chosen locale applied. Call from each
     * activity's `attachBaseContext`; a no-op when the app follows the device
     * or the system handles per-app locales.
     */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val locale = storedTag(base)?.let(Locale::forLanguageTag) ?: return base
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    /**
     * Pre-13: push the chosen locale into the application's resources, which
     * workers, notifications and widgets read. 13+: carry a choice made
     * before an OS upgrade over to the system's per-app locale, once.
     */
    fun applyToApplication(app: Application) {
        val stored = storedTag(app)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (stored == null) return
            val manager = app.getSystemService(LocaleManager::class.java) ?: return
            if (manager.applicationLocales.isEmpty) {
                manager.applicationLocales = LocaleList.forLanguageTags(stored)
            }
            prefs(app).edit().remove(KEY_TAG).apply()
            return
        }
        val locale = stored?.let(Locale::forLanguageTag) ?: return
        Locale.setDefault(locale)
        val resources = app.resources
        val config = Configuration(resources.configuration)
        config.setLocales(LocaleList(locale))
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun storedTag(context: Context): String? =
        prefs(context).getString(KEY_TAG, null)?.takeIf { it.isNotBlank() }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val PREFS = "app_language"
    private const val KEY_TAG = "language_tag"
}
