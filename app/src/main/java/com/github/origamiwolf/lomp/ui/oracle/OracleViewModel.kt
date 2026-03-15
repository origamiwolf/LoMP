package com.github.origamiwolf.lomp.ui.oracle

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.origamiwolf.lomp.data.OracleRepository
import com.github.origamiwolf.lomp.data.model.oracle.OracleNode
import com.github.origamiwolf.lomp.data.model.oracle.OracleRollOutput
import com.github.origamiwolf.lomp.oracle.OracleRoller
import com.github.origamiwolf.lomp.oracle.OracleTableLoader
import com.github.origamiwolf.lomp.oracle.OracleTableVerifier
import com.github.origamiwolf.lomp.oracle.NameRoller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OracleViewModel(
    private val context: Context,
    private val repository: OracleRepository
) : ViewModel() {

    // --- Folder URI ---
    private val _folderUri = MutableStateFlow<Uri?>(null)
    val folderUri: StateFlow<Uri?> = _folderUri.asStateFlow()

    // --- Loading ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Root nodes (the full tree) ---
    private var rootNodes: List<OracleNode> = emptyList()

    // --- Current navigation stack ---
    // Each entry is a Pair of (folder name, folder's children)
    // Empty stack means we're at root
    private val _navigationStack = MutableStateFlow<List<Pair<String, List<OracleNode>>>>(
        emptyList()
    )

    // --- Current visible nodes ---
    // What's shown in the UI right now
    private val _currentNodes = MutableStateFlow<List<OracleNode>>(emptyList())
    val currentNodes: StateFlow<List<OracleNode>> = _currentNodes.asStateFlow()

    // --- Breadcrumb path ---
    // List of folder names from root to current location
    // Empty list means we're at root
    private val _breadcrumbs = MutableStateFlow<List<String>>(emptyList())
    val breadcrumbs: StateFlow<List<String>> = _breadcrumbs.asStateFlow()

    // --- Roll output ---
    private val _rollOutput = MutableStateFlow<OracleRollOutput?>(null)
    val rollOutput: StateFlow<OracleRollOutput?> = _rollOutput.asStateFlow()

    // --- Verification results ---
    // --- Verification results ---
    private val _verificationResults = MutableStateFlow<List<OracleTableVerifier.VerificationResult>>(emptyList())
    val verificationResults: StateFlow<List<OracleTableVerifier.VerificationResult>> =
        _verificationResults.asStateFlow()

    // --- Error panel ---
    private val _showErrorPanel = MutableStateFlow(false)
    val showErrorPanel: StateFlow<Boolean> = _showErrorPanel.asStateFlow()

    init {
        viewModelScope.launch {
            repository.folderUriFlow.collect { uri ->
                _folderUri.value = uri
                if (uri != null) {
                    loadTables(uri)
                }
            }
        }
    }

    // --- Folder selection ---

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            repository.saveFolderUri(uri)
            loadTables(uri)
        }
    }

    fun reloadTables() {
        val uri = _folderUri.value ?: return
        viewModelScope.launch {
            loadTables(uri)
        }
    }

    private suspend fun loadTables(uri: Uri) {
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

        val hasIssues = result.verificationResults.any {
            it.errors.isNotEmpty() || it.warnings.isNotEmpty()
        }
        _showErrorPanel.value = hasIssues
        _isLoading.value = false
    }

    // --- Navigation ---

    /**
     * Navigate into a folder.
     * Pushes the folder onto the navigation stack and
     * updates currentNodes to show the folder's children.
     */
    fun navigateIntoFolder(folder: OracleNode.Folder) {
        val newStack = _navigationStack.value + Pair(folder.name, folder.children)
        _navigationStack.value = newStack
        _breadcrumbs.value = newStack.map { it.first }
        _currentNodes.value = folder.children
        _rollOutput.value = null
    }

    /**
     * Navigate to a specific breadcrumb index.
     * Index -1 means root.
     * Index 0 means first folder in the stack, etc.
     */
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
        _rollOutput.value = OracleRoller.roll(tableNode.table)
    }

    fun rollOnNameTable(tableNode: OracleNode.NameTable) {
        _rollOutput.value = NameRoller.roll(tableNode.table)
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
        private val context: Context,
        private val repository: OracleRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return OracleViewModel(context, repository) as T
        }
    }
}