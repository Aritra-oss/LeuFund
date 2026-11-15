package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fundTrackDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "fund_track_prefs")

class SelectedBusinessDataStore(private val context: Context) {
  private val selectedBusinessKey = longPreferencesKey("selected_business_id")
  private val nextThemeIndexKey = intPreferencesKey("next_theme_index")
  private val themeModeKey = stringPreferencesKey("theme_mode")
  private val lastExportMillisKey = longPreferencesKey("last_export_millis")

  val selectedBusinessId: Flow<Long?> =
    context.fundTrackDataStore.data.map { prefs -> prefs[selectedBusinessKey] }

  val themeMode: Flow<ThemeMode> =
    context.fundTrackDataStore.data.map { prefs -> ThemeMode.fromStorage(prefs[themeModeKey]) }

  val lastExportMillis: Flow<Long?> =
    context.fundTrackDataStore.data.map { prefs -> prefs[lastExportMillisKey] }

  suspend fun recordExportTimestamp(timestampMillis: Long = System.currentTimeMillis()) {
    context.fundTrackDataStore.edit { prefs -> prefs[lastExportMillisKey] = timestampMillis }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.fundTrackDataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
  }

  suspend fun setSelectedBusinessId(businessId: Long?) {
    context.fundTrackDataStore.edit { prefs ->
      if (businessId == null) {
        prefs.remove(selectedBusinessKey)
      } else {
        prefs[selectedBusinessKey] = businessId
      }
    }
  }

  suspend fun consumeNextThemeId(paletteCount: Int): Int {
    var assigned = 0
    context.fundTrackDataStore.edit { prefs ->
      val current = prefs[nextThemeIndexKey] ?: 0
      assigned = ((current % paletteCount) + paletteCount) % paletteCount
      prefs[nextThemeIndexKey] = (assigned + 1) % paletteCount
    }
    return assigned
  }
}
