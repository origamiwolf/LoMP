package com.github.origamiwolf.lomp.ui.oracle

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.origamiwolf.lomp.data.DicePreferencesRepository
import com.github.origamiwolf.lomp.data.OracleRepository
import com.github.origamiwolf.lomp.data.model.oracle.OracleHistoryEntry
import com.github.origamiwolf.lomp.data.model.oracle.OracleHistoryResult
import com.github.origamiwolf.lomp.data.model.oracle.OracleNode
import com.github.origamiwolf.lomp.data.model.oracle.OracleRollOutput
import com.github.origamiwolf.lomp.oracle.NameRoller
import com.github.origamiwolf.lomp.oracle.OracleRoller
import com.github.origamiwolf.lomp.oracle.OracleTableLoader
import com.github.origamiwolf.lomp.oracle.OracleTableVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OracleViewModel(
    application: android.app.Application,
    private val repository: OracleRepository,
    private val preferencesRepository: DicePreferencesRepository
) : androidx.lifecycle.AndroidViewModel(application) {

    private val context = getApplication<android.app.Application>()

    // --- Folder URI ---
    private val _folderUri = MutableStateFlow<Uri?>(null)
    val folderUri: StateFlow<Uri?> = _folderUri.asStateFlow()

    // --- Loading ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Root nodes (the full tree) ---
    private var rootNodes: List<OracleNode> = emptyList()

    // --- Current navigation stack ---
    private val _navigationStack = MutableStateFlow<List<Pair<String, List<OracleNode>>>>(
        emptyList()
    )

    // --- Current visible nodes ---
    private val _currentNodes = MutableStateFlow<List<OracleNode>>(emptyList())
    val currentNodes: StateFlow<List<OracleNode>> = _currentNodes.asStateFlow()

    // --- Breadcrumb path ---
    private val _breadcrumbs = MutableStateFlow<List<String>>(emptyList())
    val breadcrumbs: StateFlow<List<String>> = _breadcrumbs.asStateFlow()

    // --- Roll output ---
    private val _rollOutput = MutableStateFlow<OracleRollOutput?>(null)
    val rollOutput: StateFlow<OracleRollOutput?> = _rollOutput.asStateFlow()

    // --- Oracle history ---
    private val _oracleHistory = MutableStateFlow<List<OracleHistoryEntry>>(emptyList())
    val oracleHistory: StateFlow<List<OracleHistoryEntry>> = _oracleHistory.asStateFlow()

    // --- Verification results ---
    private val _verificationResults = MutableStateFlow<List<OracleTableVerifier.VerificationResult>>(emptyList())
    val verificationResults: StateFlow<List<OracleTableVerifier.VerificationResult>> =
        _verificationResults.asStateFlow()

    // --- Error panel ---
    private val _showErrorPanel = MutableStateFlow(false)
    val showErrorPanel: StateFlow<Boolean> = _showErrorPanel.asStateFlow()

    init {
        viewModelScope.launch {
            // Load folder URI
            val uri = preferencesRepository.run {
                repository.folderUriFlow.first()
            }
            _folderUri.value = uri

            // Load from cache first — instant, no file IO
            val cached = preferencesRepository.oracleNodeCacheFlow.first()
            if (cached.isNotEmpty()) {
                rootNodes = cached
                _currentNodes.value = rootNodes
            }
        }
        viewModelScope.launch {
            preferencesRepository.oracleHistoryFlow.collect { history ->
                _oracleHistory.value = history
            }
        }
    }

    // --- Folder selection ---

    /**
     * Called when user selects a new folder.
     * Scans the folder, updates cache, updates UI.
     */
    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            repository.saveFolderUri(uri)
            _folderUri.value = uri
            scanFolderAndUpdateCache(uri)
        }
    }

    /**
     * Explicit reload — rescans the folder, updates cache, updates UI.
     * Only available when a folder URI is known.
     */
    fun reloadTables() {
        val uri = _folderUri.value ?: return
        viewModelScope.launch {
            scanFolderAndUpdateCache(uri)
        }
    }

    /**
     * Scan the folder from disk, verify tables, update cache and UI.
     */
    private suspend fun scanFolderAndUpdateCache(uri: Uri) {
        _isLoading.value = true
        _navigationStack.value = emptyList()
        _breadcrumbs.value = emptyList()
        _rollOutput.value = null

        val result = withContext(Dispatchers.IO) {
            OracleTableLoader.loadFromFolder(context, uri)
        }

        rootNodes = result.rootNodes
        _currentNodes.value = rootNodes
        _verificationResults.value = result.verificationResults

        // Save to cache for next app open
        preferencesRepository.saveOracleNodeCache(rootNodes)

        val hasIssues = result.verificationResults.any {
            it.errors.isNotEmpty() || it.warnings.isNotEmpty()
        }
        _showErrorPanel.value = hasIssues
        _isLoading.value = false
    }

    // --- Navigation ---

    fun navigateIntoFolder(folder: OracleNode.Folder) {
        val newStack = _navigationStack.value + Pair(folder.name, folder.children)
        _navigationStack.value = newStack
        _breadcrumbs.value = newStack.map { it.first }
        _currentNodes.value = folder.children
        _rollOutput.value = null
    }

    fun navigateToBreadcrumb(index: Int) {
        if (index == -1) {
            _navigationStack.value = emptyList()
            _breadcrumbs.value = emptyList()
            _currentNodes.value = rootNodes
        } else {
            val newStack = _navigationStack.value.take(index + 1)
            _navigationStack.value = newStack
            _breadcrumbs.value = newStack.map { it.first }
            _currentNodes.value = newStack.last().second
        }
        _rollOutput.value = null
    }

    // --- Rolling ---

    fun rollOnTable(tableNode: OracleNode.Table) {
        val output = OracleRoller.roll(tableNode.table)
        _rollOutput.value = output
        persistOracleResult(output)
    }

    fun rollOnNameTable(tableNode: OracleNode.NameTable) {
        val output = NameRoller.roll(tableNode.table)
        _rollOutput.value = output
        persistOracleResult(output)
    }

    // --- History ---

    private fun persistOracleResult(output: OracleRollOutput) {
        val entry = OracleHistoryEntry(
            tableName = output.tableName,
            results = output.results.map { result ->
                OracleHistoryResult(
                    rolls = result.rolls,
                    text = result.text
                )
            }
        )
        val updated = (_oracleHistory.value.toMutableList().also {
            it.add(0, entry)
        }).take(10)
        _oracleHistory.value = updated
        viewModelScope.launch {
            preferencesRepository.saveOracleHistory(updated)
        }
    }

    fun clearOracleHistory() {
        viewModelScope.launch {
            preferencesRepository.clearOracleHistory()
            _oracleHistory.value = emptyList()
        }
    }

    // --- Error panel ---

    fun dismissErrorPanel() {
        _showErrorPanel.value = false
    }

    fun showErrorPanel() {
        _showErrorPanel.value = true
    }

    // --- Factory ---

    class Factory(
        private val application: android.app.Application,
        private val repository: OracleRepository,
        private val preferencesRepository: DicePreferencesRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return OracleViewModel(application, repository, preferencesRepository) as T
        }
    }
}