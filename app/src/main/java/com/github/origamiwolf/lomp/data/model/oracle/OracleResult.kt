package com.github.origamiwolf.lomp.data.model.oracle

import kotlinx.serialization.Serializable

data class RolledResult(
    val rolls: List<Int>,
    val text: String
)

data class OracleRollOutput(
    val tableName: String,
    val results: List<RolledResult>,
    val errors: List<String> = emptyList()
)

/**
 * A single entry in the oracle history.
 * Stores the table name and all rolled results for display.
 * Serializable so it can be persisted via DataStore.
 */
@Serializable
data class OracleHistoryEntry(
    val tableName: String,
    val results: List<OracleHistoryResult>
)

/**
 * A single result line within a history entry.
 * Mirrors RolledResult but is @Serializable for persistence.
 */
@Serializable
data class OracleHistoryResult(
    val rolls: List<Int>,
    val text: String
)