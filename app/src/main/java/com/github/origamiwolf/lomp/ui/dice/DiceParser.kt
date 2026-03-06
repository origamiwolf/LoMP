package com.github.origamiwolf.lomp.ui.dice

import com.github.origamiwolf.lomp.data.model.DiceHistoryEntry

object DiceParser {

    /**
     * The full result of parsing and rolling a dice expression.
     *
     * @param historyEntry Structured data for history display
     * @param breakdown Human readable list of lines for the result card
     */
    data class RollResult(
        val historyEntry: DiceHistoryEntry,
        val breakdown: List<String>
    )

    /**
     * Parse and roll a dice expression like "2d6+1d4+3".
     * Returns Result.success with a RollResult, or Result.failure with
     * a descriptive error if the expression is invalid.
     */
    fun parse(expression: String): Result<RollResult> {
        return try {
            // Clean and normalise the input
            val cleaned = expression
                .replace(" ", "")
                .lowercase()

            // Convert subtraction to addition of negative
            // "2d6-3" becomes "2d6+-3"
            val normalised = cleaned.replace("-", "+-")

            val tokens = normalised
                .split("+")
                .filter { it.isNotEmpty() }

            if (tokens.isEmpty()) {
                return Result.failure(
                    IllegalArgumentException("Empty expression")
                )
            }

            // These accumulate as we process each token
            val diceGroups = mutableListOf<List<Int>>()
            val breakdownLines = mutableListOf<String>()
            var modifier = 0
            var total = 0

            for (token in tokens) {
                if (token.contains("d")) {
                    // Dice term eg "2d6" or "-1d4"
                    val diceResult = parseDiceTerm(token)
                        ?: return Result.failure(
                            IllegalArgumentException("Invalid dice term: $token")
                        )

                    diceGroups.add(diceResult.rolls)
                    total += diceResult.sum
                    breakdownLines.add(diceResult.description)

                } else {
                    // Flat modifier eg "3" or "-2"
                    val mod = token.toIntOrNull()
                        ?: return Result.failure(
                            IllegalArgumentException("Invalid modifier: $token")
                        )
                    modifier += mod
                    total += mod
                }
            }

            // Determine if this is a simple roll:
            // exactly one die, no modifier
            val isSimple = diceGroups.size == 1
                    && diceGroups[0].size == 1
                    && modifier == 0

            // Add modifier line to breakdown if there is one
            if (modifier != 0) {
                val sign = if (modifier > 0) "+$modifier" else "$modifier"
                breakdownLines.add("Modifier: $sign")
            }

            val historyEntry = DiceHistoryEntry(
                expression = expression.replace(" ", ""),
                diceGroups = diceGroups,
                modifier = modifier,
                total = total,
                isSimple = isSimple
            )

            Result.success(
                RollResult(
                    historyEntry = historyEntry,
                    breakdown = breakdownLines
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Internal data class used only within the parser
    private data class DiceTermResult(
        val rolls: List<Int>,
        val sum: Int,
        val description: String
    )

    /**
     * Parse a single dice term like "2d6" or "-1d4".
     * Returns null if the term is malformed.
     */
    private fun parseDiceTerm(token: String): DiceTermResult? {
        val negative = token.startsWith("-")
        val cleanToken = if (negative) token.removePrefix("-") else token

        val parts = cleanToken.split("d")
        if (parts.size != 2) return null

        // "d6" with nothing before d means 1d6
        val count = if (parts[0].isEmpty()) 1
        else parts[0].toIntOrNull() ?: return null
        val sides = parts[1].toIntOrNull() ?: return null

        if (count <= 0 || sides <= 0) return null
        if (count > 100) return null  // prevent absurdly large rolls

        val rolls = (1..count).map { (1..sides).random() }
        val sum = rolls.sum()
        val finalSum = if (negative) -sum else sum

        val sign = if (negative) "-" else ""
        val description = "${sign}${count}d${sides}: " +
                "[${rolls.joinToString(", ")}] = $finalSum"

        return DiceTermResult(
            rolls = if (negative) rolls.map { -it } else rolls,
            sum = finalSum,
            description = description
        )
    }

    /**
     * Format a history entry into a single display string.
     * This is the single source of truth for history formatting.
     *
     * Examples:
     * Simple:   1d20 → [20]
     * Modified: 1d6+2 → [4] + 2 = 6
     * Multi:    2d6 → [3, 5] = 8
     * Complex:  2d4+2 → [1, 3] + 2 = 6
     */
    fun formatHistoryEntry(entry: DiceHistoryEntry): String {
        return if (entry.isSimple) {
            // Single die, no modifier - just show the roll
            "${entry.expression} → [${entry.diceGroups[0][0]}]"
        } else {
            // Build the right hand side piece by piece
            val sb = StringBuilder()
            sb.append("${entry.expression} → ")

            // Each dice group as [x, y]
            val groupStrings = entry.diceGroups.map { group ->
                "[${group.map { kotlin.math.abs(it) }.joinToString(", ")}]"
            }
            sb.append(groupStrings.joinToString(" + "))

            // Modifier if present
            if (entry.modifier != 0) {
                val sign = if (entry.modifier > 0) "+ ${entry.modifier}"
                else "- ${kotlin.math.abs(entry.modifier)}"
                sb.append(" $sign")
            }

            // Total
            sb.append(" = ${entry.total}")

            sb.toString()
        }
    }

    /**
     * Roll a single standard die immediately.
     * Used by the quick-roll buttons.
     */
    fun rollSingleDie(sides: Int): RollResult {
        val roll = (1..sides).random()
        val historyEntry = DiceHistoryEntry(
            expression = "1d$sides",
            diceGroups = listOf(listOf(roll)),
            modifier = 0,
            total = roll,
            isSimple = true
        )
        return RollResult(
            historyEntry = historyEntry,
            breakdown = listOf("1d$sides: [$roll] = $roll")
        )
    }
}