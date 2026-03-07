package com.github.origamiwolf.lomp.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.origamiwolf.lomp.data.DiceComboRepository

@Composable
fun SettingsScreen(
    diceComboRepository: DiceComboRepository,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            LocalContext.current.applicationContext,
            diceComboRepository
        )
    )
) {
    val context = LocalContext.current
    val exportState by viewModel.exportState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val showImportWarning by viewModel.showImportWarning.collectAsState()

    // File creator — opens save dialog letting user pick location and filename
    val fileSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val json = (exportState as? SettingsViewModel.ExportState.ReadyToSave)
                ?.jsonString
            if (json != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray())
                    }
                    viewModel.onExportSaved()
                } catch (e: Exception) {
                    viewModel.onExportSaveError()
                }
            }
        } else {
            // User cancelled — go back to idle
            viewModel.onExportHandled()
        }
    }

    // Launch file saver when export is ready
    LaunchedEffect(exportState) {
        if (exportState is SettingsViewModel.ExportState.ReadyToSave) {
            fileSaver.launch("lomp_dice_combos.json")
        }
    }

    // File picker for import
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        Text(
            text = "Dice Combinations",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedButton(
            onClick = { viewModel.exportCombos() },
            modifier = Modifier.fillMaxWidth(),
            enabled = exportState !is SettingsViewModel.ExportState.Loading
        ) {
            Icon(
                Icons.Default.FileUpload,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Combinations")
        }

        Text(
            text = "Saves your dice combinations to a file that can be " +
                    "imported on another device.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = { filePicker.launch(arrayOf("application/json")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = importState !is SettingsViewModel.ImportState.Loading
        ) {
            Icon(
                Icons.Default.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Combinations")
        }

        Text(
            text = "Loads dice combinations from a previously exported file. " +
                    "This will replace all current combinations.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Status messages
        when (val state = importState) {
            is SettingsViewModel.ImportState.Success -> {
                Text(
                    text = "✅ Imported ${state.count} combinations successfully.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is SettingsViewModel.ImportState.Error -> {
                Text(
                    text = "❌ ${state.message}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }

        when (val state = exportState) {
            is SettingsViewModel.ExportState.Success -> {
                Text(
                    text = "✅ Combinations exported successfully.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is SettingsViewModel.ExportState.Error -> {
                Text(
                    text = "❌ ${state.message}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }

    // Import warning dialog
    if (showImportWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportWarning() },
            title = { Text("Replace Combinations?") },
            text = {
                Text(
                    "This will permanently replace all your current saved dice " +
                            "combinations with the ones from the imported file. " +
                            "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport() }) {
                    Text("Replace", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportWarning() }) {
                    Text("Cancel")
                }
            }
        )
    }
}