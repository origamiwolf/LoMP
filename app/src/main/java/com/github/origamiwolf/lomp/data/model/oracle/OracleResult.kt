package com.github.origamiwolf.lomp.data.model.oracle

/**
 * A single rolled result line for display.
 * rolls contains the sequence of dice rolls eg [6, 3]
 * text is the final result eg "Chihuahua"
 */
data class RolledResult(
    val rolls: List<Int>,
    val text: String
)

/**
 * The complete output of consulting an oracle table.
 */
data class OracleRollOutput(
    val tableName: String,
    val results: List<RolledResult>,
    val errors: List<String> = emptyList()
)