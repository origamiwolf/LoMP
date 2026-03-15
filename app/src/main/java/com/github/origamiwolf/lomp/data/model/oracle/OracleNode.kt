package com.github.origamiwolf.lomp.data.model.oracle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class OracleNode {

    abstract val name: String

    @Serializable
    @SerialName("folder")
    data class Folder(
        override val name: String,
        val children: List<OracleNode>
    ) : OracleNode()

    @Serializable
    @SerialName("table")
    data class Table(
        override val name: String,
        val table: OracleTable
    ) : OracleNode()

    @Serializable
    @SerialName("nameTable")
    data class NameTable(
        override val name: String,
        val table: com.github.origamiwolf.lomp.data.model.oracle.NameTable
    ) : OracleNode()
}