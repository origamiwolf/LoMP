
package com.github.origamiwolf.lomp.ui.dice
import com.github.origamiwolf.lomp.data.DicePreferencesRepository
import androidx.lifecycle.viewmodel.compose.viewModel

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
import com.github.origamiwolf.lomp.data.model.DiceHistoryEntry

@Composable
fun DiceScreen(
    diceRepository: DicePreferencesRepository,
    viewModel: DiceViewModel = viewModel(
        factory = DiceViewModel.Factory(diceRepository)
    )
) {
    val expression by viewModel.expression.collectAsState()
    val result by viewModel.result.collectAsState()
    val error by viewModel.error.collectAsState()
    val history by viewModel.history.collectAsState()
    val orderedDice by viewModel.orderedDice.collectAsState()

    // Single vertical scroll for the whole screen
    // This way history is accessible by scrolling down naturally
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
        // Horizontally scrollable row of standard dice buttons
        // Ordered by usage frequency, most used first

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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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

        Button(
            onClick = { viewModel.roll() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Roll", fontSize = 18.sp)
        }

        // ── Current Result ──────────────────────────────────────────
        // Only shown after a roll has been made

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
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${result!!.historyEntry.total}",
                        fontSize = 48.sp,
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

            // Breakdown lines - not lazy since max ~5 lines
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                result!!.breakdown.forEach { line ->
                    Text(text = line, fontSize = 14.sp)
                }
            }
        }

        // ── Roll History ────────────────────────────────────────────
        // Always visible once at least one roll has been made

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

        // Bottom padding so last history item isn't flush against
        // the navigation bar
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * A single row in the history list.
 * Formats the entry using the parser's formatting function
 * so display logic stays out of the UI layer.
 */
@Composable
fun HistoryRow(entry: DiceHistoryEntry) {
    Text(
        text = DiceParser.formatHistoryEntry(entry),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}