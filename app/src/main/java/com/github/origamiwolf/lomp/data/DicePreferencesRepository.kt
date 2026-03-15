package com.github.origamiwolf.lomp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.origamiwolf.lomp.data.model.DiceHistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import com.github.origamiwolf.lomp.data.model.oracle.OracleHistoryEntry
import kotlinx.serialization.builtins.ListSerializer
import com.github.origamiwolf.lomp.data.model.oracle.OracleNode

// This creates a single DataStore instance tied to the application context.
// The delegate pattern here means Context.dataStore is available anywhere
// you have a Context — it's created lazily on first access and reused
// thereafter. Declaring it at the top level outside the class ensures
// only one instance ever exists, which DataStore requires.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dice_preferences"
)

class DicePreferencesRepository(private val context: Context) {

    // Keys are how DataStore identifies stored values.
    // They're typed — a stringPreferencesKey can only store strings.
    // We store both usage counts and history as JSON strings because
    // DataStore's built-in types don't include Maps or custom objects.
    companion object {
        private val DICE_USAGE_KEY = stringPreferencesKey("dice_usage")
        private val DICE_HISTORY_KEY = stringPreferencesKey("dice_history")
        // Add to companion object
        private val ORACLE_HISTORY_KEY = stringPreferencesKey("oracle_history")
        private val ORACLE_NODE_CACHE_KEY = stringPreferencesKey("oracle_node_cache")
    }

    // A configured Json instance.
    // ignoreUnknownKeys means if we ever add fields to DiceHistoryEntry,
    // old stored data won't crash when decoded — it just ignores new fields
    // it doesn't recognise. Good defensive practice.
    private val json = Json { ignoreUnknownKeys = true }

    // --- Usage Counts ---

    /**
     * A Flow of the current usage map.
     * Flow here means this is a stream of values — whenever the stored
     * data changes, anything observing this flow gets the new value
     * automatically. The ViewModel will collect this flow.
     */
    val diceUsageFlow: Flow<Map<Int, Int>> = context.dataStore.data
        .map { preferences ->
            val raw = preferences[DICE_USAGE_KEY]
            if (raw == null) {
                emptyMap()
            } else {
                try {
                    // Decode from JSON string back to Map
                    json.decodeFromString<Map<String, Int>>(raw)
                        .mapKeys { it.key.toInt() }
                } catch (e: Exception) {
                    // If stored data is corrupted, start fresh
                    emptyMap()
                }
            }
        }

    /**
     * Increment the usage count for a given die and persist it.
     * suspend means this function must be called from a coroutine —
     * it may need to wait for the disk write to complete.
     */
    suspend fun incrementDiceUsage(sides: Int) {
        context.dataStore.edit { preferences ->
            val current = try {
                val raw = preferences[DICE_USAGE_KEY]
                if (raw != null) {
                    json.decodeFromString<Map<String, Int>>(raw)
                        .mapKeys { it.key.toInt() }
                        .toMutableMap()
                } else {
                    mutableMapOf()
                }
            } catch (e: Exception) {
                mutableMapOf()
            }

            current[sides] = (current[sides] ?: 0) + 1

            // DataStore requires string keys in JSON maps,
            // so we convert Int keys to String before encoding
            preferences[DICE_USAGE_KEY] = json.encodeToString(
                current.mapKeys { it.key.toString() }
            )
        }
    }

    // --- Roll History ---

    /**
     * A Flow of the current history list.
     * Most recent entry is at index 0, max 5 entries.
     */
    val diceHistoryFlow: Flow<List<DiceHistoryEntry>> = context.dataStore.data
        .map { preferences ->
            val raw = preferences[DICE_HISTORY_KEY]
            if (raw == null) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<DiceHistoryEntry>>(raw)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

// Add these two functions alongside the existing history functions

    val oracleHistoryFlow: Flow<List<OracleHistoryEntry>> = context.dataStore.data
        .map { preferences ->
            val raw = preferences[ORACLE_HISTORY_KEY]
            if (raw == null) {
                emptyList()
            } else {
                try {
                    Json.decodeFromString(
                        ListSerializer(OracleHistoryEntry.serializer()),
                        raw
                    )
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    val oracleNodeCacheFlow: Flow<List<OracleNode>> = context.dataStore.data
        .map { preferences ->
            val raw = preferences[ORACLE_NODE_CACHE_KEY]
            if (raw == null) {
                emptyList()
            } else {
                try {
                    Json.decodeFromString(
                        ListSerializer(OracleNode.serializer()),
                        raw
                    )
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    suspend fun saveOracleNodeCache(nodes: List<OracleNode>) {
        context.dataStore.edit { preferences ->
            preferences[ORACLE_NODE_CACHE_KEY] = Json.encodeToString(
                ListSerializer(OracleNode.serializer()),
                nodes
            )
        }
    }

    suspend fun clearOracleNodeCache() {
        context.dataStore.edit { preferences ->
            preferences.remove(ORACLE_NODE_CACHE_KEY)
        }
    }

    suspend fun saveOracleHistory(history: List<OracleHistoryEntry>) {
        context.dataStore.edit { preferences ->
            preferences[ORACLE_HISTORY_KEY] = Json.encodeToString(
                ListSerializer(OracleHistoryEntry.serializer()),
                history
            )
        }
    }

    suspend fun clearOracleHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(ORACLE_HISTORY_KEY)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(DICE_HISTORY_KEY)
        }
    }

    /**
     * Save an updated history list to disk.
     * The ViewModel manages the list itself (trimming to 5 etc)
     * and passes the final list here for persistence.
     * This keeps the business logic in the ViewModel where it belongs.
     */
    suspend fun saveHistory(history: List<DiceHistoryEntry>) {
        context.dataStore.edit { preferences ->
            preferences[DICE_HISTORY_KEY] = json.encodeToString(history)
        }
    }
}

