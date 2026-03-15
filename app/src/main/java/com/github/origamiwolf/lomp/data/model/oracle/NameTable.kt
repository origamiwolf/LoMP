package com.github.origamiwolf.lomp.data.model.oracle

import kotlinx.serialization.Serializable

/**
 * A name table consisting of 2-4 parts rolled independently.
 * The results are concatenated with spaces to form a complete name.
 */
@Serializable
data class NameTable(
    val name: String,
    val parts: List<NamePart>
)

/**
 * A single part of a name table e.g. firstname, lastname.
 * Entries use weighted ranges just like oracle tables.
 */
@Serializable
data class NamePart(
    val name: String,
    val totalSides: Int,
    val entries: List<NameEntry>
)

/**
 * A single entry in a name part.
 * Simpler than OracleEntry — just a range and a text result.
 */
@Serializable
data class NameEntry(
    val minRoll: Int,
    val maxRoll: Int,
    val text: String
)