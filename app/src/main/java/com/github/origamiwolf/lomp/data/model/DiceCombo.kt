package com.github.origamiwolf.lomp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DiceCombo(
    val id: String,
    val name: String,
    val expression: String,
    val useCount: Int = 0
)