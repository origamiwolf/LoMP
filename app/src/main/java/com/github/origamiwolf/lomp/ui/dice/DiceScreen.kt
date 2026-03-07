package com.github.origamiwolf.lomp.ui.dice

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.origamiwolf.lomp.data.DiceComboRepository
import com.github.origamiwolf.lomp.data.DicePreferencesRepository
import com.github.origamiwolf.lomp.data.model.DiceCombo
import com.github.origamiwolf.lomp.data.model.DiceHistoryEntry

@Composable
fun DiceScreen(
    dicePreferencesRepository: DicePreferencesRepository,
    diceComboRepository: DiceComboRepository,
    viewModel: DiceViewModel = viewModel(
        factory = DiceViewModel.Factory(
            dicePreferencesRepository,
            diceComboRepository
        )
    )
) {
    val expression by viewModel.expression.collectAsState()
    val result by viewModel.result.collectAsState()
    val error by viewModel.error.collectAsState()
    val history by viewModel.history.collectAsState()
    val orderedDice by viewModel.orderedDice.collectAsState()
    val savedCombos by viewModel.savedCombos.collectAsState()
    val showSaveDialog by viewModel.showSaveDialog.collectAsState()
    val duplicateCombo by viewModel.duplicateCombo.collectAsState()
    val comboToDelete by viewModel.comboToDelete.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Dice Roller",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        // ── Quick Roll Row ──────────────────────────────────────────
        Text(
            text = "Quick Roll",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            orderedDice.forEach { sides ->
                OutlinedButton(
                    onClick = { viewModel.rollQuickDie(sides) },
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
                ) {
                    Text("d$sides", fontSize = 14.sp)
                }
            }
        }

        HorizontalDivider()

        // ── Custom Expression ───────────────────────────────────────
        Text(
            text = "Custom Roll",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = expression,
            onValueChange = { viewModel.onExpressionChanged(it) },
            label = { Text("Dice expression") },
            placeholder = { Text("e.g. 2d6+1d4+3") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error != null,
            supportingText = {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            },
            trailingIcon = {
                if (expression.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clear() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { viewModel.roll() }
            )
        )

        // Roll and Save buttons side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.roll() },
                modifier = Modifier.weight(3f)
            ) {
                Text("Roll", fontSize = 18.sp)
            }
            OutlinedButton(
                onClick = { viewModel.requestSaveCombo() },
                modifier = Modifier.weight(2f)
            ) {
                Text("Save", fontSize = 18.sp)
            }
        }

        // ── Saved Combinations ──────────────────────────────────────
        if (savedCombos.isNotEmpty()) {
            HorizontalDivider()

            Text(
                text = "Saved Combinations",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savedCombos.forEach { combo ->
                    SavedComboButton(
                        combo = combo,
                        onClick = { viewModel.loadCombo(combo) },
                        onLongClick = { viewModel.requestDeleteCombo(combo) }
                    )
                }
            }
        }

        // ── Current Result ──────────────────────────────────────────
        if (result != null) {
            HorizontalDivider()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${result!!.historyEntry.total}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                result!!.breakdown.forEach { line ->
                    Text(text = line, fontSize = 14.sp)
                }
            }
        }

        // ── Roll History ────────────────────────────────────────────
        if (history.isNotEmpty()) {
            HorizontalDivider()

            Text(
                text = "Last ${history.size} Rolls",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                history.forEach { entry ->
                    HistoryRow(entry = entry)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // ── Dialogs ─────────────────────────────────────────────────────
    if (showSaveDialog) {
        SaveComboDialog(
            duplicateCombo = duplicateCombo,
            onConfirmSave = { name -> viewModel.confirmSave(name) },
            onConfirmOverwrite = { viewModel.confirmOverwrite() },
            onDismiss = { viewModel.dismissSaveDialog() }
        )
    }

    if (comboToDelete != null) {
        DeleteComboDialog(
            combo = comboToDelete!!,
            onConfirm = { viewModel.confirmDeleteCombo() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
}

// ── Supporting Composables ───────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedComboButton(
    combo: DiceCombo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Text(
            text = combo.name,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun SaveComboDialog(
    duplicateCombo: DiceCombo?,
    onConfirmSave: (String) -> Unit,
    onConfirmOverwrite: () -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (duplicateCombo != null) "Name Already Exists"
                else "Save Combination"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (duplicateCombo != null) {
                    Text(
                        "A combination named \"${duplicateCombo.name}\" already " +
                                "exists with expression \"${duplicateCombo.expression}\". " +
                                "Do you want to overwrite it?"
                    )
                } else {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Name") },
                        placeholder = { Text("e.g. Attack Roll") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (duplicateCombo != null) {
                        onConfirmOverwrite()
                    } else {
                        onConfirmSave(nameInput)
                    }
                },
                enabled = duplicateCombo != null || nameInput.isNotBlank()
            ) {
                Text(if (duplicateCombo != null) "Overwrite" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteComboDialog(
    combo: DiceCombo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Combination") },
        text = {
            Text(
                "Delete \"${combo.name}\" (${combo.expression})? " +
                        "This cannot be undone."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HistoryRow(entry: DiceHistoryEntry) {
    Text(
        text = DiceParser.formatHistoryEntry(entry),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}