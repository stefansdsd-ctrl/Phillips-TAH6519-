package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_theme_preferences")

class ThemePreferencesRepository(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ACTIVE_APP_THEME = stringPreferencesKey("active_app_theme")
    }

    val themeModeFlow: Flow<ThemeMode> = context.themeDataStore.data.map { preferences ->
        val modeStr = preferences[KEY_THEME_MODE] ?: ThemeMode.DARK.name
        try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    val activeAppThemeFlow: Flow<AppTheme> = context.themeDataStore.data.map { preferences ->
        val themeStr = preferences[KEY_ACTIVE_APP_THEME] ?: AppTheme.PHILIPS_STUDIO.name
        try {
            AppTheme.valueOf(themeStr)
        } catch (e: Exception) {
            AppTheme.PHILIPS_STUDIO
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun saveActiveAppTheme(theme: AppTheme) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_ACTIVE_APP_THEME] = theme.name
        }
    }
}
