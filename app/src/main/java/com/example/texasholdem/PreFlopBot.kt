package com.example.texasholdem

data class Limits(val limitAggressive: Float, val limitPassive: Float) {
    init {
        require(limitPassive == NEVER || limitPassive > limitAggressive)
    }
}

fun selectPreFlopStrategy(
    handPercent: Float,
    position: TablePosition,
    numOfRaise: Int,
    numOfCall: Int,
    isPaid: Boolean
): BettingStrategy {
    val map = when {
        numOfRaise == 0 && numOfCall == 0 -> openRaise
        numOfRaise == 0 && numOfCall == 1 -> oneLimper
        numOfRaise == 0 && numOfCall >= 2 -> manyLimper
        numOfRaise == 1 && !isPaid -> oneRaise
        numOfRaise == 1 && isPaid -> oneRaisePaid
        numOfRaise >= 2 && !isPaid -> manyRaise
        numOfRaise >= 2 && isPaid -> manyRaisePaid
        else -> throw IllegalStateException("Wrong condition")
    }
    val limits = map[position.ordinal]
    return when {
        handPercent <= limits.limitAggressive -> BettingStrategy.AGGRESSIVE
        handPercent <= limits.limitPassive -> BettingStrategy.PASSIVE
        else -> BettingStrategy.DROP
    }
}

private const val NEVER = Float.NEGATIVE_INFINITY

/* Values order:
    BTN,
    SB,
    BB,
    UTG,
    HJ,
    CO
*/
private val openRaise = arrayOf(
    Limits(0.60f, NEVER),
    Limits(0.35f, 0.60f),
    Limits(NEVER, 1.00f),
    Limits(0.20f, NEVER),
    Limits(0.25f, NEVER),
    Limits(0.35f, NEVER)
)

private val oneLimper = arrayOf(
    Limits(0.38f, 0.60f),
    Limits(0.30f, 0.55f),
    Limits(0.25f, 0.70f),
    Limits(0.18f, 0.30f),
    Limits(0.22f, 0.35f),
    Limits(0.30f, 0.45f)
)

private val manyLimper = arrayOf(
    Limits(0.30f, 0.75f),
    Limits(0.25f, 0.70f),
    Limits(0.20f, 1.00f),
    Limits(0.15f, 0.35f),
    Limits(0.18f, 0.40f),
    Limits(0.25f, 0.55f)
)

private val oneRaise = arrayOf(
    Limits(0.15f, 0.40f),
    Limits(0.10f, 0.30f),
    Limits(0.12f, 0.45f),
    Limits(0.08f, 0.18f),
    Limits(0.08f, 0.18f),
    Limits(0.10f, 0.28f)
)

private val oneRaisePaid = arrayOf(
    Limits(0.20f, 0.45f),
    Limits(0.15f, 0.40f),
    Limits(0.18f, 0.55f),
    Limits(0.12f, 0.35f),
    Limits(0.12f, 0.35f),
    Limits(0.15f, 0.40f)
)

private val manyRaise = arrayOf(
    Limits(0.08f, 0.18f),
    Limits(0.06f, 0.15f),
    Limits(0.08f, 0.20f),
    Limits(0.05f, 0.12f),
    Limits(0.05f, 0.12f),
    Limits(0.06f, 0.15f)
)

private val manyRaisePaid = arrayOf(
    Limits(0.12f, 0.25f),
    Limits(0.10f, 0.22f),
    Limits(0.12f, 0.28f),
    Limits(0.08f, 0.20f),
    Limits(0.08f, 0.18f),
    Limits(0.10f, 0.20f)
)