package com.github.origamiwolf.lomp.oracle

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.github.origamiwolf.lomp.data.model.oracle.NameTable
import com.github.origamiwolf.lomp.data.model.oracle.OracleNode
import com.github.origamiwolf.lomp.data.model.oracle.OracleTable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object OracleTableLoader {

    data class LoadResult(
        val rootNodes: List<OracleNode>,
        val verificationResults: List<OracleTableVerifier.VerificationResult>
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadFromFolder(context: Context, folderUri: Uri): LoadResult {
        val verificationResults = mutableListOf<OracleTableVerifier.VerificationResult>()

        val rootFolder = DocumentFile.fromTreeUri(context, folderUri)
            ?: return LoadResult(
                rootNodes = emptyList(),
                verificationResults = listOf(
                    OracleTableVerifier.VerificationResult(
                        fileName = "root",
                        isValid = false,
                        errors = listOf("Could not access selected folder"),
                        warnings = emptyList()
                    )
                )
            )

        val nodes = buildNodes(context, rootFolder, verificationResults)
        return LoadResult(rootNodes = nodes, verificationResults = verificationResults)
    }

    private fun buildNodes(
        context: Context,
        folder: DocumentFile,
        verificationResults: MutableList<OracleTableVerifier.VerificationResult>
    ): List<OracleNode> {
        val nodes = mutableListOf<OracleNode>()

        val files = folder.listFiles()
            .sortedWith(compareBy({ !it.isDirectory }, { it.name }))

        files.forEach { file ->
            when {
                file.isDirectory -> {
                    val children = buildNodes(context, file, verificationResults)
                    if (children.isNotEmpty()) {
                        nodes.add(
                            OracleNode.Folder(
                                name = file.name ?: "Unknown",
                                children = children
                            )
                        )
                    }
                }
                file.isFile && file.name?.endsWith(".json") == true -> {
                    val fileName = file.name ?: "unknown.json"
                    val node = loadNode(context, file, fileName, verificationResults)
                    if (node != null) nodes.add(node)
                }
            }
        }

        return nodes
    }

    /**
     * Load a JSON file as either an oracle table or a name table
     * depending on the "type" field. Missing type defaults to "oracle".
     */
    private fun loadNode(
        context: Context,
        file: DocumentFile,
        fileName: String,
        verificationResults: MutableList<OracleTableVerifier.VerificationResult>
    ): OracleNode? {
        val jsonString = try {
            context.contentResolver
                .openInputStream(file.uri)
                ?.bufferedReader()
                ?.readText()
                ?: run {
                    verificationResults.add(
                        OracleTableVerifier.VerificationResult(
                            fileName = fileName,
                            isValid = false,
                            errors = listOf("Could not read file"),
                            warnings = emptyList()
                        )
                    )
                    return null
                }
        } catch (e: Exception) {
            verificationResults.add(
                OracleTableVerifier.VerificationResult(
                    fileName = fileName,
                    isValid = false,
                    errors = listOf("Unexpected error: ${e.message}"),
                    warnings = emptyList()
                )
            )
            return null
        }

        // Peek at the type field to decide how to parse
        val tableType = try {
            val jsonObject = json.decodeFromString<JsonObject>(jsonString)
            jsonObject["type"]?.jsonPrimitive?.content ?: "oracle"
        } catch (e: Exception) {
            verificationResults.add(
                OracleTableVerifier.VerificationResult(
                    fileName = fileName,
                    isValid = false,
                    errors = listOf("Invalid JSON: ${e.message}"),
                    warnings = emptyList()
                )
            )
            return null
        }

        return when (tableType) {
            "name" -> loadNameTable(jsonString, fileName, verificationResults)
            else -> loadOracleTable(jsonString, fileName, verificationResults)
        }
    }

    private fun loadOracleTable(
        jsonString: String,
        fileName: String,
        verificationResults: MutableList<OracleTableVerifier.VerificationResult>
    ): OracleNode? {
        val table = try {
            json.decodeFromString<OracleTable>(jsonString)
        } catch (e: Exception) {
            verificationResults.add(
                OracleTableVerifier.VerificationResult(
                    fileName = fileName,
                    isValid = false,
                    errors = listOf("Invalid JSON: ${e.message}"),
                    warnings = emptyList()
                )
            )
            return null
        }

        val verification = OracleTableVerifier.verify(table, fileName)
        verificationResults.add(verification)

        return if (verification.isValid) {
            OracleNode.Table(name = table.name, table = table)
        } else null
    }

    private fun loadNameTable(
        jsonString: String,
        fileName: String,
        verificationResults: MutableList<OracleTableVerifier.VerificationResult>
    ): OracleNode? {
        val table = try {
            json.decodeFromString<NameTable>(jsonString)
        } catch (e: Exception) {
            verificationResults.add(
                OracleTableVerifier.VerificationResult(
                    fileName = fileName,
                    isValid = false,
                    errors = listOf("Invalid JSON: ${e.message}"),
                    warnings = emptyList()
                )
            )
            return null
        }

        val verification = OracleTableVerifier.verifyNameTable(table, fileName)
        verificationResults.add(verification)

        return if (verification.isValid) {
            OracleNode.NameTable(name = table.name, table = table)
        } else null
    }
}