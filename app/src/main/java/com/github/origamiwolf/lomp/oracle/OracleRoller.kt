package com.github.origamiwolf.lomp.oracle

import com.github.origamiwolf.lomp.data.model.oracle.OracleEntry
import com.github.origamiwolf.lomp.data.model.oracle.OracleResult
import com.github.origamiwolf.lomp.data.model.oracle.OracleRollOutput
import com.github.origamiwolf.lomp.data.model.oracle.OracleTable
import com.github.origamiwolf.lomp.data.model.oracle.RolledResult
import com.github.origamiwolf.lomp.data.model.oracle.SubTableDefinition

object OracleRoller {

    private const val MAX_RESULTS = 10

    fun roll(table: OracleTable): OracleRollOutput {
        val results = mutableListOf<RolledResult>()
        val errors = mutableListOf<String>()
        rollOnTable(
            table = table,
            parentRolls = emptyList(),
            results = results,
            errors = errors,
            parentText = "",
            subtables = table.subtables
        )
        return OracleRollOutput(
            tableName = table.name,
            results = results,
            errors = errors
        )
    }

    private fun rollOnTable(
        table: OracleTable,
        parentRolls: List<Int>,
        results: MutableList<RolledResult>,
        errors: MutableList<String>,
        parentText: String,
        subtables: Map<String, SubTableDefinition>
    ) {
        if (results.size >= MAX_RESULTS) {
            errors.add("Maximum result limit ($MAX_RESULTS) reached")
            return
        }

        val roll = (1..table.totalSides).random()
        val currentRolls = parentRolls + roll

        val entry = table.entries.firstOrNull {
            roll in it.minRoll..it.maxRoll
        }

        if (entry == null) {
            errors.add("No entry found for roll $roll on table '${table.name}'")
            return
        }

        processEntry(
            entry = entry,
            currentRolls = currentRolls,
            parentRolls = parentRolls,
            results = results,
            errors = errors,
            parentText = parentText,
            subtables = subtables,
            currentTable = table
        )
    }

    private fun rollOnSubTableDefinition(
        definition: SubTableDefinition,
        subtableName: String,
        parentRolls: List<Int>,
        results: MutableList<RolledResult>,
        errors: MutableList<String>,
        parentText: String,
        subtables: Map<String, SubTableDefinition>
    ) {
        if (results.size >= MAX_RESULTS) {
            errors.add("Maximum result limit ($MAX_RESULTS) reached")
            return
        }

        val roll = (1..definition.totalSides).random()
        val currentRolls = parentRolls + roll

        val entry = definition.entries.firstOrNull {
            roll in it.minRoll..it.maxRoll
        }

        if (entry == null) {
            errors.add(
                "No entry found for roll $roll on subtable '$subtableName'"
            )
            return
        }

        // Subtable definitions only support value, rollTwice, and inline
        // subtables — not tableRef. Build a temporary OracleTable to reuse
        // processEntry for inline subtable handling.
        val tempTable = OracleTable(
            name = subtableName,
            totalSides = definition.totalSides,
            entries = definition.entries
        )

        processEntry(
            entry = entry,
            currentRolls = currentRolls,
            parentRolls = parentRolls,
            results = results,
            errors = errors,
            parentText = parentText,
            subtables = subtables,
            currentTable = tempTable
        )
    }

    /**
     * Process a single rolled entry, handling all result types.
     * Extracted to avoid duplication between rollOnTable and
     * rollOnSubTableDefinition.
     */
    private fun processEntry(
        entry: OracleEntry,
        currentRolls: List<Int>,
        parentRolls: List<Int>,
        results: MutableList<RolledResult>,
        errors: MutableList<String>,
        parentText: String,
        subtables: Map<String, SubTableDefinition>,
        currentTable: OracleTable
    ) {
        when (val result = entry.result) {
            is OracleResult.Value -> {
                val fullText = listOf(parentText, result.text)
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
                results.add(RolledResult(rolls = currentRolls, text = fullText))
            }

            is OracleResult.RollTwice -> {
                rollOnTable(currentTable, parentRolls, results, errors,
                    parentText, subtables)
                rollOnTable(currentTable, parentRolls, results, errors,
                    parentText, subtables)
            }

            is OracleResult.SubTable -> {
                // Inline subtable — backward compatible
                val combinedText = listOf(parentText, result.text)
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
                val subTable = OracleTable(
                    name = result.name,
                    totalSides = result.totalSides,
                    entries = result.entries
                )
                rollOnTable(subTable, currentRolls, results, errors,
                    combinedText, subtables)
            }

            is OracleResult.TableRef -> {
                // Referenced subtable — look up by name
                val definition = subtables[result.ref]
                if (definition == null) {
                    errors.add(
                        "tableRef '${result.ref}' not found in subtables map"
                    )
                    return
                }
                val combinedText = listOf(parentText, result.text)
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
                rollOnSubTableDefinition(
                    definition = definition,
                    subtableName = result.ref,
                    parentRolls = currentRolls,
                    results = results,
                    errors = errors,
                    parentText = combinedText,
                    subtables = subtables
                )
            }
        }
    }
}