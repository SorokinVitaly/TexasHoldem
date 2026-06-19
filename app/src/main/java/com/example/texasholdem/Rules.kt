package com.example.texasholdem

const val SMALL_BLIND = 1
const val BIG_BLIND = 2
const val SMALL_BET = 2
const val BIG_BET = 4
const val MAX_NUM_OF_RAISE = 3

val drawOdds = mapOf(
    IncompleteCombinationType.FOUR_TO_FLUSH to 0.19f,
    IncompleteCombinationType.FOUR_TO_STRAIGHT_OPEN to 0.17f,
    IncompleteCombinationType.FOUR_TO_STRAIGHT to 0.09f,
    IncompleteCombinationType.THREE_TO_STRAIGHT_FLUSH to 0.12f,
    IncompleteCombinationType.NO_INCOMPLETE to 0f
)