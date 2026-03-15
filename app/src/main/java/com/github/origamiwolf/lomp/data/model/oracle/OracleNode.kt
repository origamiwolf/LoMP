package com.github.origamiwolf.lomp.data.model.oracle

sealed class OracleNode {

    abstract val name: String

    data class Folder(
        override val name: String,
        val children: List<OracleNode>
    ) : OracleNode()

    data class Table(
        override val name: String,
        val table: OracleTable
    ) : OracleNode()

    /**
     * A name generation table consisting of multiple rolled parts.
     */
    data class NameTable(
        override val name: String,
        val table: com.github.origamiwolf.lomp.data.model.oracle.NameTable
    ) : OracleNode()
}