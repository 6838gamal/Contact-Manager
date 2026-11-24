package gamalsolutions.gamalprojects.contactmanager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.statsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "contact_hub_prefs"
)

data class StoredPreferences(
    val exportedCount: Int = 0,
    val importedCount: Int = 0,
    val whatsAppOpenedCount: Int = 0,
    val lastBackupTimestamp: Long? = null,
    val appLanguage: String = "ar"
)

/**
 * مخزن DataStore للإحصائيات المحلية وتفضيل لغة الواجهة (العربية افتراضياً).
 */
class StatsPreferences(private val context: Context) {

    private object Keys {
        val EXPORTED_COUNT = intPreferencesKey("exported_count")
        val IMPORTED_COUNT = intPreferencesKey("imported_count")
        val WHATSAPP_OPENED_COUNT = intPreferencesKey("whatsapp_opened_count")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val preferencesFlow: Flow<StoredPreferences> = context.statsDataStore.data.map { prefs ->
        StoredPreferences(
            exportedCount = prefs[Keys.EXPORTED_COUNT] ?: 0,
            importedCount = prefs[Keys.IMPORTED_COUNT] ?: 0,
            whatsAppOpenedCount = prefs[Keys.WHATSAPP_OPENED_COUNT] ?: 0,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TIMESTAMP],
            appLanguage = prefs[Keys.APP_LANGUAGE] ?: "ar"
        )
    }

    suspend fun incrementExported(delta: Int = 1) {
        context.statsDataStore.edit { prefs ->
            prefs[Keys.EXPORTED_COUNT] = (prefs[Keys.EXPORTED_COUNT] ?: 0) + delta
        }
    }

    suspend fun incrementImported(count: Int) {
        if (count <= 0) return
        context.statsDataStore.edit { prefs ->
            prefs[Keys.IMPORTED_COUNT] = (prefs[Keys.IMPORTED_COUNT] ?: 0) + count
        }
    }

    suspend fun incrementWhatsAppOpened() {
        context.statsDataStore.edit { prefs ->
            prefs[Keys.WHATSAPP_OPENED_COUNT] = (prefs[Keys.WHATSAPP_OPENED_COUNT] ?: 0) + 1
        }
    }

    suspend fun updateLastBackupTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.statsDataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.statsDataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = if (languageCode == "en") "en" else "ar"
        }
    }
}
