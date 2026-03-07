package com.github.origamiwolf.lomp.oracle

import com.github.origamiwolf.lomp.data.model.oracle.OracleEntry
import com.github.origamiwolf.lomp.data.model.oracle.OracleResult
import com.github.origamiwolf.lomp.data.model.oracle.OracleTable

object OracleTableVerifier {

    data class VerificationResult(
        val fileName: String,
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>
    )

    private const val MAX_SUBTABLE_DEPTH = 5

    fun verify(table: OracleTable, fileName: String): VerificationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        verifyTable(table, fileName, depth = 0, errors, warnings)
        return VerificationResult(
            fileName = fileName,
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun verifyTable(
        table: OracleTable,
        context: String,
        depth: Int,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        if (table.name.isBlank()) {
            errors.add("$context: table name is blank")
        }
        if (table.totalSides <= 0) {
            errors.add("$context: totalSides must be greater than 0")
        }
        if (table.entries.isEmpty()) {
            errors.add("$context: table has no entries")
            return
        }
        if (depth > 2) {
            warnings.add(
                "$context: subtable depth $depth exceeds " +
                        "recommended maximum of 2"
            )
        }
        if (depth >= MAX_SUBTABLE_DEPTH) {
            errors.add(
                "$context: subtable depth exceeds maximum of $MAX_SUBTABLE_DEPTH"
            )
            return
        }

        table.entries.forEachIndexed { index, entry ->
            verifyEntry(
                entry,
                "$context entry ${index + 1}",
                depth,
                errors,
                warnings
            )
        }

        verifyCoverage(table, context, errors)
    }

    private fun verifyEntry(
        entry: OracleEntry,
        context: String,
        depth: Int,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        if (entry.minRoll > entry.maxRoll) {
            errors.add(
                "$context: minRoll (${entry.minRoll}) > maxRoll (${entry.maxRoll})"
            )
        }
        if (entry.minRoll <= 0) {
            errors.add("$context: minRoll must be >= 1")
        }

        when (val result = entry.result) {
            is OracleResult.SubTable -> {
                val subTable = OracleTable(
                    name = result.name,
                    totalSides = result.totalSides,
                    entries = result.entries
                )
                verifyTable(
                    subTable,
                    "$context subtable '${result.name}'",
                    depth + 1,
                    errors,
                    warnings
                )
            }
            is OracleResult.Value -> {
                if (result.text.isBlank()) {
                    errors.add("$context: result text is blank")
                }
            }
            is OracleResult.RollTwice -> { /* nothing to verify */ }
        }
    }

    private fun verifyCoverage(
        table: OracleTable,
        context: String,
        errors: MutableList<String>
    ) {
        val covered = mutableSetOf<Int>()
        table.entries.forEach { entry ->
            for (roll in entry.minRoll..entry.maxRoll) {
                if (roll in covered) {
                    errors.add("$context: roll $roll is covered by multiple entries")
                }
                covered.add(roll)
            }
        }
        for (roll in 1..table.totalSides) {
            if (roll !in covered) {
                errors.add("$context: no entry covers roll $roll (gap in range)")
            }
        }
        table.entries.forEach { entry ->
            if (entry.maxRoll > table.totalSides) {
                errors.add(
                    "$context: entry maxRoll (${entry.maxRoll}) " +
                            "exceeds totalSides (${table.totalSides})"
                )
            }
        }
    }
}