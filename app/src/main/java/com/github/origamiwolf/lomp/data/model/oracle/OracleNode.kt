package com.github.origamiwolf.lomp.data.model.oracle

/**
 * Represents a node in the oracle folder tree.
 * A node is either a folder containing other nodes,
 * or a table that can be rolled on.
 *
 * This mirrors the actual folder/file structure on disk.
 */
sealed class OracleNode {

    abstract val name: String

    /**
     * A folder containing other nodes.
     * Maps directly to a folder on disk.
     */
    data class Folder(
        override val name: String,
        val children: List<OracleNode>
    ) : OracleNode()

    /**
     * A loadable oracle table.
     * Maps directly to a JSON file on disk.
     */
    data class Table(
        override val name: String,
        val table: OracleTable
    ) : OracleNode()
}