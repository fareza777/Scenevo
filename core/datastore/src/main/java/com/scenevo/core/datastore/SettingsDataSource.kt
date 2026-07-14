package com.scenevo.core.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.scenevo.domain.model.AiProvider
import com.scenevo.domain.model.AiProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("scenevo_settings")

class SettingsDataSource(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val aiConfigKey = stringPreferencesKey("ai_config")

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "scenevo_secrets",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun observeAiConfig(): Flow<AiProviderConfig> =
        context.settingsDataStore.data.map { prefs ->
            prefs[aiConfigKey]?.let {
                runCatching { json.decodeFromString<AiProviderConfig>(it) }.getOrNull()
            } ?: AiProviderConfig()
        }

    suspend fun updateAiConfig(config: AiProviderConfig) {
        context.settingsDataStore.edit { prefs ->
            prefs[aiConfigKey] = json.encodeToString(config)
        }
    }

    fun saveApiKey(providerKey: String, rawKey: String) {
        encryptedPrefs.edit().putString(providerKey, rawKey).apply()
    }

    fun clearApiKey(providerKey: String) {
        encryptedPrefs.edit().remove(providerKey).apply()
    }

    fun getApiKey(providerKey: String): String? = encryptedPrefs.getString(providerKey, null)

    fun providerKeyName(provider: AiProvider): String = "api_key_${provider.name.lowercase()}"
}
