package com.github.origamiwolf.lomp.ui.dice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.origamiwolf.lomp.data.DicePreferencesRepository
import com.github.origamiwolf.lomp.data.model.DiceHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val DEFAULT_DICE = listOf(3, 4, 5, 6, 8, 10, 12, 14, 16, 20, 24, 30, 100)

class DiceViewModel(
    private val repository: DicePreferencesRepository
) : ViewModel() {

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
    private val _history = MutableStateFlow<List<DiceHistoryEntry>>(emptyList())
    val history: StateFlow<List<DiceHistoryEntry>> = _history.asStateFlow()

    // --- Ordered dice state ---
    private val _orderedDice = MutableStateFlow(DEFAULT_DICE)
    val orderedDice: StateFlow<List<Int>> = _orderedDice.asStateFlow()

    // Initialise by loading persisted data from DataStore.
    // viewModelScope.launch runs this in the background — the UI
    // doesn't wait for it, it just updates when the data arrives.
    // collect is how you observe a Flow — every time the Flow emits
    // a new value, the block inside runs with that value.
    init {
        viewModelScope.launch {
            repository.diceUsageFlow.collect { usage ->
                _orderedDice.value = DEFAULT_DICE.sortedByDescending {
                    usage[it] ?: 0
                }
            }
        }

        viewModelScope.launch {
            repository.diceHistoryFlow.collect { history ->
                _history.value = history
            }
        }
    }

    // --- Public actions ---

    fun onExpressionChanged(newExpression: String) {
        _expression.value = newExpression
        _error.value = null
    }

    fun roll() {
        if (_expression.value.isBlank()) {
            _error.value = "Enter a dice expression"
            return
        }

        DiceParser.parse(_expression.value)
            .onSuccess { rollResult ->
                _result.value = rollResult
                _error.value = null
                persistRollResult(rollResult.historyEntry)
            }
            .onFailure { exception ->
                _error.value = "Invalid expression: ${exception.message}"
                _result.value = null
            }
    }

    fun rollQuickDie(sides: Int) {
        val rollResult = DiceParser.rollSingleDie(sides)
        _result.value = rollResult
        _error.value = null
        persistRollResult(rollResult.historyEntry)

        // Persist the incremented usage count
        viewModelScope.launch {
            repository.incrementDiceUsage(sides)
        }
    }

    fun clear() {
        _expression.value = ""
        _result.value = null
        _error.value = null
    }

    // --- Private helpers ---

    /**
     * Add entry to history and persist the updated list.
     * The history list is updated in memory first so the UI
     * responds immediately, then persisted in the background.
     */
    private fun persistRollResult(entry: DiceHistoryEntry) {
        val updated = (_history.value.toMutableList().also {
            it.add(0, entry)
        }).take(5)

        _history.value = updated

        viewModelScope.launch {
            repository.saveHistory(updated)
        }
    }

    // --- Factory ---

    // ViewModels can't normally take constructor parameters —
    // the system creates them and doesn't know what to pass in.
    // A Factory tells the system how to create this ViewModel,
    // specifically what repository instance to inject into it.
    class Factory(
        private val repository: DicePreferencesRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return DiceViewModel(repository) as T
        }
    }
}