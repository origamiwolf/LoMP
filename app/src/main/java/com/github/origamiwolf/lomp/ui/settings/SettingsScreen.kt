package com.github.origamiwolf.lomp.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.origamiwolf.lomp.BuildConfig
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
    val hasAnyCombos by viewModel.hasAnyCombos.collectAsState()
    val uriHandler = LocalUriHandler.current

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
            viewModel.onExportHandled()
        }
    }

    LaunchedEffect(exportState) {
        if (exportState is SettingsViewModel.ExportState.ReadyToSave) {
            fileSaver.launch("lomp_dice_combos.json")
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        // ── Dice Combinations ───────────────────────────────────────
        Text(
            text = "Dice Combinations",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedButton(
            onClick = { viewModel.exportCombos() },
            modifier = Modifier.fillMaxWidth(),
            enabled = hasAnyCombos &&
                    exportState !is SettingsViewModel.ExportState.Loading
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

        if (!hasAnyCombos) {
            Text(
                text = "No saved combinations to export yet.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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

        // ── About ───────────────────────────────────────────────────
        HorizontalDivider()

        Text(
            text = "About",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Version", fontSize = 14.sp)
            Text(
                text = BuildConfig.VERSION_NAME,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Source Code", fontSize = 14.sp)
            TextButton(
                onClick = {
                    uriHandler.openUri("https://github.com/origamiwolf/lomp")
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "github.com/origamiwolf/lomp",
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // ── Import Warning Dialog ────────────────────────────────────────
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