package com.github.origamiwolf.lomp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.origamiwolf.lomp.data.model.DiceCombo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class DiceComboRepository(private val context: Context) {

    companion object {
        private val DICE_COMBOS_KEY = stringPreferencesKey("dice_combos")
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * A Flow of all saved combinations, ordered alphabetically by name.
     * Emits a new list whenever the stored data changes.
     */
    val allCombos: Flow<List<DiceCombo>> = context.dataStore.data
        .map { preferences ->
            val raw = preferences[DICE_COMBOS_KEY]
            if (raw == null) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<DiceCombo>>(raw)
                        .sortedWith(compareByDescending<DiceCombo> { it.useCount }
                            .thenBy { it.name.lowercase() })
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    sealed class SaveResult {
        object Success : SaveResult()
        data class DuplicateName(val existing: DiceCombo) : SaveResult()
    }

    /**
     * Save a new combination.
     * Returns DuplicateName if a combo with that name already exists.
     */
    suspend fun saveCombo(name: String, expression: String): SaveResult {
        val current = getCurrentCombos()
        val existing = current.find {
            it.name.equals(name.trim(), ignoreCase = true)
        }
        return if (existing != null) {
            SaveResult.DuplicateName(existing)
        } else {
            val newCombo = DiceCombo(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                expression = expression
            )
            saveCombos(current + newCombo)
            SaveResult.Success
        }
    }

    /**
     * Overwrite an existing combo's expression.
     * Finds by ID so the name stays intact.
     */
    suspend fun overwriteCombo(existing: DiceCombo, newExpression: String) {
        val current = getCurrentCombos().toMutableList()
        val index = current.indexOfFirst { it.id == existing.id }
        if (index != -1) {
            current[index] = existing.copy(expression = newExpression)
            saveCombos(current)
        }
    }

    /**
     * Delete a combo by ID.
     */
    suspend fun deleteCombo(combo: DiceCombo) {
        val current = getCurrentCombos().filter { it.id != combo.id }
        saveCombos(current)
    }

    suspend fun incrementUseCount(combo: DiceCombo) {
        val current = getCurrentCombos().toMutableList()
        val index = current.indexOfFirst { it.id == combo.id }
        if (index != -1) {
            current[index] = current[index].copy(useCount = current[index].useCount + 1)
            saveCombos(current)
        }
    }

    // --- Private helpers ---

    /**
     * Read the current combo list once synchronously.
     * Uses .first() to take just the first emission from the flow
     * rather than observing it continuously.
     */
    private suspend fun getCurrentCombos(): List<DiceCombo> {
        return context.dataStore.data.map { preferences ->
            val raw = preferences[DICE_COMBOS_KEY]
            if (raw != null) {
                try {
                    json.decodeFromString<List<DiceCombo>>(raw)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }.first()
    }

    private suspend fun saveCombos(combos: List<DiceCombo>) {
        context.dataStore.edit { preferences ->
            preferences[DICE_COMBOS_KEY] = json.encodeToString(combos)
        }
    }
}