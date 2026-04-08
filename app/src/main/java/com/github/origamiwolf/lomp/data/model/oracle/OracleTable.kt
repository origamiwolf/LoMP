package com.github.origamiwolf.lomp.data.model.oracle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OracleTable(
    val name: String,
    val totalSides: Int,
    val entries: List<OracleEntry>
)

@Serializable
data class OracleEntry(
    val minRoll: Int,
    val maxRoll: Int,
    val result: OracleResult
)

/**
 * Sealed class for the three result types.
 * The "type" field in JSON determines which subclass is used.
 */
@Serializable
sealed class OracleResult {

    @Serializable
    @SerialName("value")
    data class Value(val text: String) : OracleResult()

    @Serializable
    @SerialName("rollTwice")
    object RollTwice : OracleResult()

    @Serializable
    @SerialName("table")
    data class SubTable(
        val text: String = "",
        val name: String,
        val totalSides: Int,
        val entries: List<OracleEntry>
    ) : OracleResult()
}