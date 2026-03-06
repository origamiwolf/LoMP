package com.github.origamiwolf.lomp.ui.dice

import androidx.lifecycle.ViewModel
import com.github.origamiwolf.lomp.data.model.DiceHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The standard TTRPG dice, in default order
// Steps 3 and 4 will reorder these by usage frequency
val DEFAULT_DICE = listOf(3, 4, 5, 6, 8, 10, 12, 14, 16, 20, 24, 30, 100)

class DiceViewModel : ViewModel() {

    // --- Expression input state ---

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    // --- Roll result state ---

    private val _result = MutableStateFlow<DiceParser.RollResult?>(null)
    val result: StateFlow<DiceParser.RollResult?> = _result.asStateFlow()

    // --- Error state ---

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // --- History state ---
    // Kept as a list of DiceHistoryEntry, max 5 entries
    // Most recent is at index 0
    private val _history = MutableStateFlow<List<DiceHistoryEntry>>(emptyList())
    val history: StateFlow<List<DiceHistoryEntry>> = _history.asStateFlow()

    // --- Dice usage frequency ---
    // Maps die sides to number of times rolled eg {6: 12, 20: 8}
    // Steps 3 and 4 will persist this to DataStore
    private val _diceUsage = MutableStateFlow<Map<Int, Int>>(emptyMap())

    // The ordered list of dice sides, sorted by usage frequency
    // Dice never rolled appear at the end in default order
    private val _orderedDice = MutableStateFlow(DEFAULT_DICE)
    val orderedDice: StateFlow<List<Int>> = _orderedDice.asStateFlow()

    // --- Public actions ---

    fun onExpressionChanged(newExpression: String) {
        _expression.value = newExpression
        _error.value = null
    }

    /**
     * Roll the current custom expression.
     * Called by the Roll button and the keyboard Done action.
     */
    fun roll() {
        if (_expression.value.isBlank()) {
            _error.value = "Enter a dice expression"
            return
        }

        DiceParser.parse(_expression.value)
            .onSuccess { rollResult ->
                _result.value = rollResult
                _error.value = null
                addToHistory(rollResult.historyEntry)
            }
            .onFailure { exception ->
                _error.value = "Invalid expression: ${exception.message}"
                _result.value = null
            }
    }

    /**
     * Roll a single standard die from the quick-roll list.
     * Called by tapping a die button.
     */
    fun rollQuickDie(sides: Int) {
        val rollResult = DiceParser.rollSingleDie(sides)
        _result.value = rollResult
        _error.value = null
        addToHistory(rollResult.historyEntry)
        incrementUsage(sides)
    }

    fun clear() {
        _expression.value = ""
        _result.value = null
        _error.value = null
    }

    // --- Private helpers ---

    /**
     * Add an entry to the front of the history list.
     * Trims to 5 entries maximum.
     */
    private fun addToHistory(entry: DiceHistoryEntry) {
        val current = _history.value.toMutableList()
        current.add(0, entry)
        _history.value = current.take(5)
    }

    /**
     * Increment the usage counter for a die and reorder the list.
     * Note: currently in-memory only. Step 3 adds persistence.
     */
    private fun incrementUsage(sides: Int) {
        val current = _diceUsage.value.toMutableMap()
        current[sides] = (current[sides] ?: 0) + 1
        _diceUsage.value = current
        reorderDice(current)
    }

    /**
     * Sort dice by usage count descending.
     * Dice with equal or zero usage retain their default order.
     */
    private fun reorderDice(usage: Map<Int, Int>) {
        _orderedDice.value = DEFAULT_DICE.sortedByDescending {
            usage[it] ?: 0
        }
    }
}