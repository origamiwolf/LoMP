package com.github.origamiwolf.lomp.data.model.oracle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OracleTable(
    val name: String,
    val totalSides: Int,
    val entries: List<OracleEntry>,
    val subtables: Map<String, SubTableDefinition> = emptyMap()
)

/**
 * A named subtable definition stored at the top level of a table file.
 * Referenced by name from OracleResult.TableRef entries.
 * Does not have a name field of its own — the name is the map key.
 */
@Serializable
data class SubTableDefinition(
    val totalSides: Int,
    val entries: List<OracleEntry>
)

@Serializable
data class OracleEntry(
    val minRoll: Int,
    val maxRoll: Int,
    val result: OracleResult
)

@Serializable
sealed class OracleResult {

    @Serializable
    @SerialName("value")
    data class Value(val text: String) : OracleResult()

    @Serializable
    @SerialName("rollTwice")
    object RollTwice : OracleResult()

    /**
     * Inline subtable — still supported for backward compatibility.
     * The subtable definition is embedded directly in the entry.
     */
    @Serializable
    @SerialName("table")
    data class SubTable(
        val text: String = "",
        val name: String,
        val totalSides: Int,
        val entries: List<OracleEntry>
    ) : OracleResult()

    /**
     * Referenced subtable — looks up the subtable by name from
     * the parent table's subtables map.
     * More efficient when the same subtable is used by multiple entries.
     */
    @Serializable
    @SerialName("tableRef")
    data class TableRef(
        val text: String = "",
        val ref: String
    ) : OracleResult()
}