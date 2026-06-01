package studio.acks.reader

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

object AppSettingsKeys {
    val DEFAULT_THEME    = stringPreferencesKey("default_theme")
    val DEFAULT_VIEWPORT = stringPreferencesKey("default_viewport")
    val DEFAULT_HTML_MODE = stringPreferencesKey("default_html_mode")
    val FONT_SCALE       = floatPreferencesKey("font_scale")
    val IS_FIRST_RUN     = booleanPreferencesKey("is_first_run")
}

class AppSettings(private val context: Context) {

    val defaultTheme: Flow<String> = context.dataStore.data
        .map { it[AppSettingsKeys.DEFAULT_THEME] ?: "aireport" }

    val defaultViewport: Flow<String> = context.dataStore.data
        .map { it[AppSettingsKeys.DEFAULT_VIEWPORT] ?: "phone" }

    val defaultHtmlMode: Flow<String> = context.dataStore.data
        .map { it[AppSettingsKeys.DEFAULT_HTML_MODE] ?: "safe" }

    val fontScale: Flow<Float> = context.dataStore.data
        .map { it[AppSettingsKeys.FONT_SCALE] ?: 1.0f }

    val isFirstRun: Flow<Boolean> = context.dataStore.data
        .map { it[AppSettingsKeys.IS_FIRST_RUN] ?: true }

    suspend fun setDefaultTheme(themeId: String) {
        context.dataStore.edit { it[AppSettingsKeys.DEFAULT_THEME] = themeId }
    }

    suspend fun setDefaultViewport(viewport: String) {
        context.dataStore.edit { it[AppSettingsKeys.DEFAULT_VIEWPORT] = viewport }
    }

    suspend fun setDefaultHtmlMode(mode: String) {
        context.dataStore.edit { it[AppSettingsKeys.DEFAULT_HTML_MODE] = mode }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[AppSettingsKeys.FONT_SCALE] = scale }
    }

    suspend fun markFirstRunDone() {
        context.dataStore.edit { it[AppSettingsKeys.IS_FIRST_RUN] = false }
    }
}
