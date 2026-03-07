package com.github.origamiwolf.lomp.oracle

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.github.origamiwolf.lomp.data.model.oracle.OracleNode
import com.github.origamiwolf.lomp.data.model.oracle.OracleTable
import kotlinx.serialization.json.Json

object OracleTableLoader {

    data class LoadResult(
        val rootNodes: List<OracleNode>,
        val verificationResults: List<OracleTableVerifier.VerificationResult>
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Load the folder tree from the selected root URI.
     * Returns a list of top-level nodes mirroring the folder structure.
     */
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

        return LoadResult(
            rootNodes = nodes,
            verificationResults = verificationResults
        )
    }

    /**
     * Recursively build the node tree from a DocumentFile folder.
     * Folders become OracleNode.Folder, JSON files become OracleNode.Table.
     * Non-JSON files are silently ignored.
     */
    private fun buildNodes(
        context: Context,
        folder: DocumentFile,
        verificationResults: MutableList<OracleTableVerifier.VerificationResult>
    ): List<OracleNode> {
        val nodes = mutableListOf<OracleNode>()

        // Sort: folders first, then files, both alphabetically
        val files = folder.listFiles()
            .sortedWith(compareBy({ !it.isDirectory }, { it.name }))

        files.forEach { file ->
            when {
                file.isDirectory -> {
                    val children = buildNodes(context, file, verificationResults)
                    // Only add folder if it contains at least one valid node
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
                    val node = loadTableNode(context, file, fileName, verificationResults)
                    if (node != null) {
                        nodes.add(node)
                    }
                }
            }
        }

        return nodes
    }

    private fun loadTableNode(
        context: Context,
        file: DocumentFile,
        fileName: String,
        verificationResults: MutableList<OracleTableVerifier.VerificationResult>
    ): OracleNode.Table? {
        return try {
            val jsonString = context.contentResolver
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

            if (verification.isValid) {
                OracleNode.Table(name = table.name, table = table)
            } else {
                null
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
            null
        }
    }
}