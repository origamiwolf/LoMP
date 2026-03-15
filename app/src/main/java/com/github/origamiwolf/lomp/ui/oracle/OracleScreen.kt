package com.github.origamiwolf.lomp.ui.oracle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.origamiwolf.lomp.data.OracleRepository
import com.github.origamiwolf.lomp.data.model.oracle.OracleNode
import com.github.origamiwolf.lomp.data.model.oracle.OracleRollOutput
import com.github.origamiwolf.lomp.oracle.OracleTableVerifier

@Composable
fun OracleScreen(
    oracleRepository: OracleRepository,
    viewModel: OracleViewModel = viewModel(
        factory = OracleViewModel.Factory(
            LocalContext.current.applicationContext,
            oracleRepository
        )
    )
) {
    val folderUri by viewModel.folderUri.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentNodes by viewModel.currentNodes.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val rollOutput by viewModel.rollOutput.collectAsState()
    val verificationResults by viewModel.verificationResults.collectAsState()
    val showErrorPanel by viewModel.showErrorPanel.collectAsState()

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.onFolderSelected(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Oracle",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // ── Folder Controls ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { folderPicker.launch(null) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (folderUri != null) "Change Folder" else "Select Folder",
                    fontSize = 14.sp
                )
            }

            if (folderUri != null) {
                OutlinedButton(
                    onClick = { viewModel.reloadTables() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reload", fontSize = 14.sp)
                }
            }
        }

        // ── Loading Indicator ───────────────────────────────────────
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // ── Error Panel ─────────────────────────────────────────────
        if (showErrorPanel && verificationResults.isNotEmpty()) {
            ErrorPanel(
                results = verificationResults,
                onDismiss = { viewModel.dismissErrorPanel() }
            )
        } else if (!showErrorPanel && verificationResults.any {
                it.errors.isNotEmpty() || it.warnings.isNotEmpty()
            }) {
            TextButton(onClick = { viewModel.showErrorPanel() }) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Show verification issues",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }

        // ── Breadcrumbs ─────────────────────────────────────────────
        if (folderUri != null) {
            Breadcrumbs(
                breadcrumbs = breadcrumbs,
                onNavigate = { index -> viewModel.navigateToBreadcrumb(index) }
            )
        }

        // ── Node Buttons ────────────────────────────────────────────
        if (currentNodes.isNotEmpty()) {
            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                currentNodes.forEach { node ->
                    when (node) {
                        is OracleNode.Folder -> FolderButton(
                            folder = node,
                            onClick = { viewModel.navigateIntoFolder(node) }
                        )
                        is OracleNode.Table -> TableButton(
                            table = node,
                            onClick = { viewModel.rollOnTable(node) }
                        )
                        is OracleNode.NameTable -> NameTableButton(
                            table = node,
                            onClick = { viewModel.rollOnNameTable(node) }
                        )
                    }
                }
            }
        } else if (!isLoading) {
            if (folderUri == null) {
                EmptyStateHint(
                    text = "Tap Select Folder to load your oracle tables."
                )
            } else {
                EmptyStateHint(
                    text = "No valid tables found in this folder. " +
                            "Check the verification panel for details."
                )
            }
        }

        // ── Roll Result ─────────────────────────────────────────────
        if (rollOutput != null) {
            HorizontalDivider()
            RollOutputDisplay(output = rollOutput!!)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Supporting Composables ───────────────────────────────────────────

/**
 * Breadcrumb navigation row.
 * "Oracle" is always the root crumb at index -1.
 * Each folder in the stack gets its own crumb.
 * The last crumb (current location) is not clickable.
 */
@Composable
fun Breadcrumbs(
    breadcrumbs: List<String>,
    onNavigate: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Root crumb
        Text(
            text = "Oracle",
            fontSize = 14.sp,
            color = if (breadcrumbs.isEmpty())
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.primary,
            fontWeight = if (breadcrumbs.isEmpty())
                FontWeight.SemiBold else FontWeight.Normal,
            modifier = if (breadcrumbs.isNotEmpty())
                Modifier.clickable { onNavigate(-1) }
            else
                Modifier
        )

        // Folder crumbs
        breadcrumbs.forEachIndexed { index, name ->
            Text(
                text = " > ",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val isLast = index == breadcrumbs.lastIndex
            Text(
                text = name,
                fontSize = 14.sp,
                color = if (isLast)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.primary,
                fontWeight = if (isLast)
                    FontWeight.SemiBold else FontWeight.Normal,
                modifier = if (!isLast)
                    Modifier.clickable { onNavigate(index) }
                else
                    Modifier
            )
        }
    }
}

@Composable
fun FolderButton(
    folder: OracleNode.Folder,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = folder.name,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${folder.children.size} items",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TableButton(
    table: OracleNode.Table,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            Icons.Default.Casino,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = table.name,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "d${table.table.totalSides}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun NameTableButton(
    table: OracleNode.NameTable,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = table.name,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${table.table.parts.size} parts",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ErrorPanel(
    results: List<OracleTableVerifier.VerificationResult>,
    onDismiss: () -> Unit
) {
    val errored = results.filter { it.errors.isNotEmpty() }
    val warned = results.filter {
        it.errors.isEmpty() && it.warnings.isNotEmpty()
    }
    val valid = results.filter {
        it.errors.isEmpty() && it.warnings.isEmpty()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Verification Results",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        "Dismiss",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Text(
                text = "✅ ${valid.size} loaded  " +
                        "⚠️ ${warned.size} warnings  " +
                        "❌ ${errored.size} errors",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            errored.forEach { result ->
                Text(
                    text = "❌ ${result.fileName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                result.errors.forEach { error ->
                    Text(
                        text = "  • $error",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            warned.forEach { result ->
                Text(
                    text = "⚠️ ${result.fileName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                result.warnings.forEach { warning ->
                    Text(
                        text = "  • $warning",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun RollOutputDisplay(output: OracleRollOutput) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = output.tableName,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        output.results.forEach { result ->
            val rollString = result.rolls.joinToString(", ")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "[$rollString]",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = result.text,
                    fontSize = 14.sp
                )
            }
        }

        if (output.errors.isNotEmpty()) {
            output.errors.forEach { error ->
                Text(
                    text = "⚠️ $error",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EmptyStateHint(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}