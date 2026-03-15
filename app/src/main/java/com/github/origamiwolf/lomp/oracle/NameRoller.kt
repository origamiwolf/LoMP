package com.github.origamiwolf.lomp.oracle

import com.github.origamiwolf.lomp.data.model.oracle.NameTable
import com.github.origamiwolf.lomp.data.model.oracle.OracleRollOutput
import com.github.origamiwolf.lomp.data.model.oracle.RolledResult

object NameRoller {

    /**
     * Roll on each part of a name table independently and
     * assemble the results into a single name.
     *
     * The rolls list contains one roll per part in order,
     * and the assembled name is the text result.
     *
     * Example: [3, 7] John Smith
     */
    fun roll(table: NameTable): OracleRollOutput {
        val rolls = mutableListOf<Int>()
        val nameParts = mutableListOf<String>()
        val errors = mutableListOf<String>()

        table.parts.forEach { part ->
            val roll = (1..part.totalSides).random()
            rolls.add(roll)

            val entry = part.entries.firstOrNull {
                roll in it.minRoll..it.maxRoll
            }

            if (entry == null) {
                errors.add(
                    "No entry found for roll $roll on part '${part.name}'"
                )
            } else {
                nameParts.add(entry.text)
            }
        }

        val assembledName = nameParts.joinToString(" ")

        return OracleRollOutput(
            tableName = table.name,
            results = listOf(
                RolledResult(
                    rolls = rolls,
                    text = assembledName
                )
            ),
            errors = errors
        )
    }
}