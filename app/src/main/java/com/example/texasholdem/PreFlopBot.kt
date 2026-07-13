package com.example.texasholdem

data class Limits(val limitAggressive: ChenStrength, val limitPassive: ChenStrength) {
    init {
        require(limitPassive == NEVER || limitPassive < limitAggressive)
    }
}

fun calcTableIndex(
    numOfRaise: Int,
    numOfCall: Int,
    isPaid: Boolean
) = when {
    numOfRaise == 0 && numOfCall == 0 -> 0
    numOfRaise == 0 && numOfCall == 1 -> 1
    numOfRaise == 0 && numOfCall >= 2 -> 2
    numOfRaise == 1 && !isPaid -> 3
    numOfRaise == 1 && isPaid -> 4
    numOfRaise >= 2 && !isPaid -> 5
    numOfRaise >= 2 && isPaid -> 6
    else -> throw IllegalStateException("Wrong table index condition")
}

fun selectPreFlopStrategy(
    handStrength: ChenStrength,
    position: TablePosition,
    numOfRaise: Int,
    numOfCall: Int,
    isPaid: Boolean
): BettingStrategy {
    val tableIndex = calcTableIndex(numOfRaise, numOfCall, isPaid)
    val table = tables[tableIndex]
    val limits = table[position.ordinal]
    return when {
        limits == UNUSED -> throw IllegalStateException("UNUSED Limits: ${position.name}, tableIndex = $tableIndex")
        handStrength >= limits.limitAggressive -> BettingStrategy.AGGRESSIVE
        handStrength >= limits.limitPassive -> BettingStrategy.PASSIVE
        else -> BettingStrategy.DROP
    }
}

private const val ALWAYS = Int.MIN_VALUE
private const val NEVER = Int.MAX_VALUE
private val UNUSED = Limits(NEVER, NEVER)

/* Values order:
    BTN,
    SB,
    BB,
    UTG,
    HJ,
    CO
*/
private val openRaise = arrayOf(
    Limits(6, NEVER),
    Limits(10, 6),
    UNUSED,
    Limits(13, NEVER),
    Limits(12, NEVER),
    Limits(10, NEVER)
)

private val oneLimper = arrayOf(
    Limits(10, 6),
    Limits(11, 7),
    Limits(12, ALWAYS),
    UNUSED,
    Limits(13, 10),
    Limits(11, 9)
)

private val manyLimper = arrayOf(
    Limits(11, 4),
    Limits(12, 5),
    Limits(13, ALWAYS),
    UNUSED,
    UNUSED,
    Limits(12, 7)
)

private val oneRaise = arrayOf(
    Limits(14, 9),
    Limits(18, 12),
    Limits(15, 9),
    UNUSED,
    Limits(16, 13),
    Limits(16, 11)
)

private val oneRaisePaid = arrayOf(
    Limits(13, 9),
    Limits(14, 9),
    Limits(14, 7),
    UNUSED,
    Limits(15, 10),
    Limits(14, 9)
)
private val manyRaise = arrayOf(
    Limits(16, 13),
    Limits(18, 14),
    Limits(16, 13),
    UNUSED,
    UNUSED,
    Limits(18, 14)
)

private val manyRaisePaid = arrayOf(
    Limits(15, 12),
    Limits(16, 13),
    Limits(15, 12),
    Limits(16, 13),
    Limits(16, 14),
    Limits(16, 13)
)

private val tables = arrayOf(
    openRaise,
    oneLimper,
    manyLimper,
    oneRaise,
    oneRaisePaid,
    manyRaise,
    manyRaisePaid
)