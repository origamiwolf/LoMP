package com.github.origamiwolf.lomp.data.model

/**
 * Represents a single roll entry in the history list.
 *
 * @param expression The original expression typed or tapped eg "2d6+1d4"
 * @param diceGroups Each group of dice rolls eg listOf([3,5], [2])
 * @param modifier The flat modifier eg +2, -1, or 0 if none
 * @param total The final sum of all dice and modifier
 * @param isSimple True when exactly one die, no modifier - history shows [roll] only
 */
data class DiceHistoryEntry(
    val expression: String,
    val diceGroups: List<List<Int>>,
    val modifier: Int,
    val total: Int,
    val isSimple: Boolean
)