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
    val DEFAULT_THEME       = stringPreferencesKey("default_theme")
    val DEFAULT_VIEWPORT    = stringPreferencesKey("default_viewport")
    val DEFAULT_HTML_MODE   = stringPreferencesKey("default_html_mode")
    val FONT_SCALE          = floatPreferencesKey("font_scale")
    val APP_THEME           = stringPreferencesKey("app_theme")
    val IS_FIRST_RUN        = booleanPreferencesKey("is_first_run")
    // 卡片导出参数
    val CARD_FONT_SIZE_PX   = floatPreferencesKey("card_font_size_px")   // 14–22, default 18
    val CARD_PAD_PX         = floatPreferencesKey("card_pad_px")         // 16–40, default 28
    val CARD_WITH_COVER     = booleanPreferencesKey("card_with_cover")
    val CARD_THEME_ID       = stringPreferencesKey("card_theme_id")
    // 字体来源手动覆盖
    val FONT_SOURCE_OVERRIDE = stringPreferencesKey("font_source_override") // "auto"|"local"|"cn_mirror"
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

    val appTheme: Flow<String> = context.dataStore.data
        .map { it[AppSettingsKeys.APP_THEME] ?: "system" }

    val isFirstRun: Flow<Boolean> = context.dataStore.data
        .map { it[AppSettingsKeys.IS_FIRST_RUN] ?: true }

    val cardFontSizePx: Flow<Float> = context.dataStore.data
        .map { it[AppSettingsKeys.CARD_FONT_SIZE_PX] ?: 18f }

    val cardPadPx: Flow<Float> = context.dataStore.data
        .map { it[AppSettingsKeys.CARD_PAD_PX] ?: 28f }

    val cardWithCover: Flow<Boolean> = context.dataStore.data
        .map { it[AppSettingsKeys.CARD_WITH_COVER] ?: true }

    val cardThemeId: Flow<String> = context.dataStore.data
        .map { it[AppSettingsKeys.CARD_THEME_ID] ?: "aireport" }

    val fontSourceOverride: Flow<String> = context.dataStore.data
        .map { it[AppSettingsKeys.FONT_SOURCE_OVERRIDE] ?: "auto" }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { it[AppSettingsKeys.APP_THEME] = theme }
    }

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

    suspend fun setCardFontSizePx(v: Float) {
        context.dataStore.edit { it[AppSettingsKeys.CARD_FONT_SIZE_PX] = v }
    }
    suspend fun setCardPadPx(v: Float) {
        context.dataStore.edit { it[AppSettingsKeys.CARD_PAD_PX] = v }
    }
    suspend fun setCardWithCover(v: Boolean) {
        context.dataStore.edit { it[AppSettingsKeys.CARD_WITH_COVER] = v }
    }
    suspend fun setCardThemeId(v: String) {
        context.dataStore.edit { it[AppSettingsKeys.CARD_THEME_ID] = v }
    }
    suspend fun setFontSourceOverride(v: String) {
        context.dataStore.edit { it[AppSettingsKeys.FONT_SOURCE_OVERRIDE] = v }
        FontManager.invalidate()
    }
}
