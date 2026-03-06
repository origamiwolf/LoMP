package com.github.origamiwolf.lomp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DiceHistoryEntry(
    val expression: String,
    val diceGroups: List<List<Int>>,
    val modifier: Int,
    val total: Int,
    val isSimple: Boolean
)