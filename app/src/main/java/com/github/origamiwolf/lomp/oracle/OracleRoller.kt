package com.github.origamiwolf.lomp.oracle

import com.github.origamiwolf.lomp.data.model.oracle.OracleResult
import com.github.origamiwolf.lomp.data.model.oracle.OracleRollOutput
import com.github.origamiwolf.lomp.data.model.oracle.OracleTable
import com.github.origamiwolf.lomp.data.model.oracle.RolledResult

object OracleRoller {

    private const val MAX_RESULTS = 10

    fun roll(table: OracleTable): OracleRollOutput {
        val results = mutableListOf<RolledResult>()
        val errors = mutableListOf<String>()
        rollOnTable(table, emptyList(), results, errors, parentText = "")
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
        parentText: String
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

        when (val result = entry.result) {
            is OracleResult.Value -> {
                // Prepend parent text if present
                val fullText = if (parentText.isNotEmpty()) {
                    "$parentText ${result.text}"
                } else {
                    result.text
                }
                results.add(RolledResult(rolls = currentRolls, text = fullText))
            }

            is OracleResult.RollTwice -> {
                // Each roll is independent, passing parentText through
                rollOnTable(table, parentRolls, results, errors, parentText)
                rollOnTable(table, parentRolls, results, errors, parentText)
            }

            is OracleResult.SubTable -> {
                // Combine parent text with subtable text
                val combinedText = listOf(parentText, result.text)
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")

                val subTable = OracleTable(
                    name = result.name,
                    totalSides = result.totalSides,
                    entries = result.entries
                )
                // Pass combined text down so subtable result appends to it
                rollOnTable(subTable, currentRolls, results, errors, combinedText)
            }
        }
    }
}