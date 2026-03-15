package com.github.origamiwolf.lomp.ui.dice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.origamiwolf.lomp.data.DiceComboRepository
import com.github.origamiwolf.lomp.data.DicePreferencesRepository
import com.github.origamiwolf.lomp.data.model.DiceCombo
import com.github.origamiwolf.lomp.data.model.DiceHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val DEFAULT_DICE = listOf(3, 4, 5, 6, 8, 10, 12, 14, 16, 20, 24, 30, 100)

class DiceViewModel(
    private val preferencesRepository: DicePreferencesRepository,
    private val comboRepository: DiceComboRepository
) : ViewModel() {

    // --- Expression input ---
    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    // --- Roll result ---
    private val _result = MutableStateFlow<DiceParser.RollResult?>(null)
    val result: StateFlow<DiceParser.RollResult?> = _result.asStateFlow()

    // --- Error ---
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // --- History ---
    private val _history = MutableStateFlow<List<DiceHistoryEntry>>(emptyList())
    val history: StateFlow<List<DiceHistoryEntry>> = _history.asStateFlow()

    // --- Ordered dice ---
    private val _orderedDice = MutableStateFlow(DEFAULT_DICE)
    val orderedDice: StateFlow<List<Int>> = _orderedDice.asStateFlow()

    // --- Saved combos ---
    private val _savedCombos = MutableStateFlow<List<DiceCombo>>(emptyList())
    val savedCombos: StateFlow<List<DiceCombo>> = _savedCombos.asStateFlow()

    // --- Save dialog state ---
    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val _duplicateCombo = MutableStateFlow<DiceCombo?>(null)
    val duplicateCombo: StateFlow<DiceCombo?> = _duplicateCombo.asStateFlow()

    // --- Delete dialog state ---
    private val _comboToDelete = MutableStateFlow<DiceCombo?>(null)
    val comboToDelete: StateFlow<DiceCombo?> = _comboToDelete.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.diceUsageFlow.collect { usage ->
                _orderedDice.value = DEFAULT_DICE.sortedByDescending {
                    usage[it] ?: 0
                }
            }
        }
        viewModelScope.launch {
            preferencesRepository.diceHistoryFlow.collect { history ->
                _history.value = history
            }
        }
        viewModelScope.launch {
            comboRepository.allCombos.collect { combos ->
                _savedCombos.value = combos
            }
        }
    }

    // --- Expression actions ---

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
        viewModelScope.launch {
            preferencesRepository.incrementDiceUsage(sides)
        }
    }

    fun clear() {
        _expression.value = ""
        _result.value = null
        _error.value = null
    }

    // --- Save combo actions ---

    fun requestSaveCombo() {
        if (_expression.value.isBlank()) {
            _error.value = "Enter a dice expression to save"
            return
        }
        _duplicateCombo.value = null
        _showSaveDialog.value = true
    }

    fun dismissSaveDialog() {
        _showSaveDialog.value = false
        _duplicateCombo.value = null
    }

    fun confirmSave(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            when (val saveResult = comboRepository.saveCombo(
                name.trim(),
                _expression.value
            )) {
                is DiceComboRepository.SaveResult.Success -> {
                    _showSaveDialog.value = false
                    _duplicateCombo.value = null
                }
                is DiceComboRepository.SaveResult.DuplicateName -> {
                    _duplicateCombo.value = saveResult.existing
                }
            }
        }
    }

    fun confirmOverwrite() {
        val duplicate = _duplicateCombo.value ?: return
        viewModelScope.launch {
            comboRepository.overwriteCombo(duplicate, _expression.value)
            _showSaveDialog.value = false
            _duplicateCombo.value = null
        }
    }

    // --- Load combo ---

    fun loadCombo(combo: DiceCombo) {
        _expression.value = combo.expression
        _error.value = null
        viewModelScope.launch {
            comboRepository.incrementUseCount(combo)
        }
        roll()
    }

    // --- Delete combo actions ---

    fun requestDeleteCombo(combo: DiceCombo) {
        _comboToDelete.value = combo
    }

    fun confirmDeleteCombo() {
        val combo = _comboToDelete.value ?: return
        viewModelScope.launch {
            comboRepository.deleteCombo(combo)
            _comboToDelete.value = null
        }
    }

    fun dismissDeleteDialog() {
        _comboToDelete.value = null
    }

    // --- Private helpers ---

    private fun persistRollResult(entry: DiceHistoryEntry) {
        val updated = (_history.value.toMutableList().also {
            it.add(0, entry)
        }).take(5)
        _history.value = updated
        viewModelScope.launch {
            preferencesRepository.saveHistory(updated)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            preferencesRepository.clearHistory()
            _history.value = emptyList()
        }
    }

    // --- Factory ---

    class Factory(
        private val preferencesRepository: DicePreferencesRepository,
        private val comboRepository: DiceComboRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return DiceViewModel(preferencesRepository, comboRepository) as T
        }
    }
}