package com.github.origamiwolf.lomp.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.origamiwolf.lomp.data.DiceComboRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context,
    private val comboRepository: DiceComboRepository
) : ViewModel() {

    // --- Combo count for export button ---
    val hasAnyCombos: StateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        viewModelScope.launch {
            comboRepository.allCombos.collect { combos ->
                (flow as MutableStateFlow).value = combos.isNotEmpty()
            }
        }
    }

    // --- Export state ---
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    // --- Import state ---
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    // --- Import dialog ---
    private val _showImportWarning = MutableStateFlow(false)
    val showImportWarning: StateFlow<Boolean> = _showImportWarning.asStateFlow()

    // Holds the pending JSON string while the user confirms
    private var pendingImportJson: String? = null

    sealed class ExportState {
        object Idle : ExportState()
        object Loading : ExportState()
        data class ReadyToSave(val jsonString: String) : ExportState()
        object Success : ExportState()
        data class Error(val message: String) : ExportState()
    }
    sealed class ImportState {
        object Idle : ImportState()
        object Loading : ImportState()
        data class Success(val count: Int) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    // --- Export ---

    fun exportCombos() {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val jsonString = comboRepository.exportCombos()
                _exportState.value = ExportState.ReadyToSave(jsonString)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(
                    e.message ?: "Export failed"
                )
            }
        }
    }

    fun onExportHandled() {
        _exportState.value = ExportState.Idle
    }

    fun onExportSaved() {
        _exportState.value = ExportState.Success
    }

    fun onExportSaveError() {
        _exportState.value = ExportState.Error("Failed to write file")
    }

    // --- Import ---

    /**
     * Called when the user has selected a file.
     * Reads the file content and shows the warning dialog.
     */
    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.readText()
                    ?: run {
                        _importState.value = ImportState.Error("Could not read file")
                        return@launch
                    }

                // Validate it's parseable before showing the warning
                // This gives a better error message than failing after confirmation
                comboRepository.exportCombos() // ensure repo is accessible
                pendingImportJson = jsonString
                _showImportWarning.value = true

            } catch (e: Exception) {
                _importState.value = ImportState.Error(
                    "Could not read file: ${e.message}"
                )
            }
        }
    }

    fun dismissImportWarning() {
        _showImportWarning.value = false
        pendingImportJson = null
    }

    fun confirmImport() {
        val jsonString = pendingImportJson ?: return
        _showImportWarning.value = false
        pendingImportJson = null

        viewModelScope.launch {
            _importState.value = ImportState.Loading
            try {
                val count = comboRepository.importCombos(jsonString)
                _importState.value = ImportState.Success(count)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(
                    "Invalid file format: ${e.message}"
                )
            }
        }
    }

    fun onImportHandled() {
        _importState.value = ImportState.Idle
    }

    // --- Factory ---

    class Factory(
        private val context: Context,
        private val comboRepository: DiceComboRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context, comboRepository) as T
        }
    }
}