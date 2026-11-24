package gamalsolutions.gamalprojects.contactmanager.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/**
 * أداة مساعدة لتطبيق اللغة (العربية RTL أو الإنجليزية LTR) مع الحفاظ على سياق Activity
 * لضمان عمل ActivityResultRegistryOwner و BackHandler بشكل سليم.
 */
object LocaleHelper {

    fun createLocalizedContext(baseActivity: Activity, languageCode: String): Context {
        val tag = if (languageCode == "en") "en" else "ar"
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)

        val config = Configuration(baseActivity.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val localizedConfigContext = baseActivity.createConfigurationContext(config)

        return object : ContextWrapper(baseActivity) {
            override fun getResources(): Resources = localizedConfigContext.resources
        }
    }

    fun layoutDirectionFor(languageCode: String): LayoutDirection {
        return if (languageCode == "en") LayoutDirection.Ltr else LayoutDirection.Rtl
    }
}
