package com.example.texasholdem

import kotlin.random.Random


enum class BettingStrategy {
    DROP,
    PASSIVE,
    AGGRESSIVE
}

enum class TablePosition {
    BTN,
    SB,
    BB,
    UTG,
    HJ,
    CO
}

fun selectPreFlopStrategy(
    handPercent: Float,
    position: TablePosition,
    numOfRaise: Int,
    numOfCall: Int
): BettingStrategy {
    val openThreshold = when (position) {
        TablePosition.UTG -> 0.15f
        TablePosition.HJ  -> 0.20f
        TablePosition.CO  -> 0.27f
        TablePosition.BTN -> 0.45f
        TablePosition.SB  -> 0.35f
        TablePosition.BB  -> 0.60f
    }
    val raiseTightening = 1f - (numOfRaise * 0.30f).coerceIn(0f, 0.85f)
    val callThreshold = openThreshold * raiseTightening
    val raiseThreshold = callThreshold * 0.35f
    val impliedOddsBonus = (numOfCall * 0.02f).coerceAtMost(0.06f)
    val effectiveCallThreshold = callThreshold + impliedOddsBonus
    return when {
        handPercent <= raiseThreshold ->
            mixedDecision(handPercent, raiseThreshold, AGGRESSIVE_MIX_WIDTH,
                ifInside = BettingStrategy.AGGRESSIVE, ifOutside = BettingStrategy.PASSIVE)
        handPercent <= effectiveCallThreshold ->
            mixedDecision(handPercent, effectiveCallThreshold, PASSIVE_MIX_WIDTH,
                ifInside = BettingStrategy.PASSIVE, ifOutside = BettingStrategy.DROP)
        else -> BettingStrategy.DROP
    }
}

fun selectPostFlopStrategy(
    equity: Float,
    isFacingBet: Boolean,
    numOfRaise: Int,
    position: TablePosition
): BettingStrategy {
    val random = Random.nextFloat()
    val inPosition = position == TablePosition.BTN || position == TablePosition.CO
    return when {
        !isFacingBet && !inPosition -> when {
            equity < 0.30f -> BettingStrategy.DROP
            equity < 0.45f -> if (random < 0.8f) BettingStrategy.DROP else BettingStrategy.AGGRESSIVE
            equity < 0.60f -> if (random < 0.5f) BettingStrategy.DROP else BettingStrategy.AGGRESSIVE
            equity < 0.75f -> if (random < 0.2f) BettingStrategy.DROP else BettingStrategy.AGGRESSIVE
            else           -> BettingStrategy.AGGRESSIVE
        }
        !isFacingBet && inPosition -> when {
            equity < 0.25f -> BettingStrategy.DROP
            equity < 0.40f -> if (random < 0.6f) BettingStrategy.DROP else BettingStrategy.AGGRESSIVE
            equity < 0.55f -> if (random < 0.3f) BettingStrategy.DROP else BettingStrategy.AGGRESSIVE
            equity < 0.70f -> if (random < 0.1f) BettingStrategy.DROP else BettingStrategy.AGGRESSIVE
            else           -> BettingStrategy.AGGRESSIVE
        }
        numOfRaise == 0 && !inPosition -> when {
            equity < 0.25f -> BettingStrategy.DROP
            equity < 0.45f -> if (random < 0.2f) BettingStrategy.DROP else BettingStrategy.PASSIVE
            equity < 0.65f -> if (random < 0.8f) BettingStrategy.PASSIVE else BettingStrategy.AGGRESSIVE
            equity < 0.80f -> if (random < 0.4f) BettingStrategy.PASSIVE else BettingStrategy.AGGRESSIVE
            else           -> BettingStrategy.AGGRESSIVE
        }
        numOfRaise == 0 && inPosition -> when {
            equity < 0.20f -> BettingStrategy.DROP
            equity < 0.40f -> if (random < 0.1f) BettingStrategy.DROP else BettingStrategy.PASSIVE
            equity < 0.60f -> if (random < 0.6f) BettingStrategy.PASSIVE else BettingStrategy.AGGRESSIVE
            equity < 0.75f -> if (random < 0.2f) BettingStrategy.PASSIVE else BettingStrategy.AGGRESSIVE
            else           -> BettingStrategy.AGGRESSIVE
        }
        numOfRaise == 1 && !inPosition -> when {
            equity < 0.40f -> BettingStrategy.DROP
            equity < 0.60f -> if (random < 0.2f) BettingStrategy.DROP else BettingStrategy.PASSIVE
            equity < 0.80f -> if (random < 0.7f) BettingStrategy.PASSIVE else BettingStrategy.AGGRESSIVE
            else           -> if (random < 0.2f) BettingStrategy.PASSIVE else BettingStrategy.AGGRESSIVE
        }
        numOfRaise == 1 && inPosition -> when {
            equity < 0.35f -> BettingStrategy.DROP
            equity < 0.55f -> if (random < 0.1f) BettingStrategy.DROP else BettingStrategy.PASSIVE
            equity < 0.75f -> if (random < 0.6f) BettingStrategy.PASSIVE else BettingStrategy.AGGRESSIVE
            else           -> if (random < 0.2f) BettingStrategy.PASSIVE else BettingStrategy.AGGRESSIVE
        }
        else -> when {
            equity < 0.55f -> BettingStrategy.DROP
            equity < 0.75f -> BettingStrategy.PASSIVE
            else           -> if (random < 0.7f) BettingStrategy.PASSIVE else BettingStrategy.AGGRESSIVE
        }
    }
}

fun resolveAction(
    strategy: BettingStrategy,
    availableActions: List<ActionType>,
): ActionType {
    if (strategy == BettingStrategy.AGGRESSIVE) {
        availableActions.find { it is ActionType.Raise }?.let { return it }
        availableActions.find { it is ActionType.Bet   }?.let { return it }
    }
    if (strategy >= BettingStrategy.PASSIVE) {
        availableActions.find { it is ActionType.Call  }?.let { return it }
    }
    availableActions.find { it is ActionType.Check }?.let { return it }
    return ActionType.Fold()
}

private fun mixedDecision(
    handPercent: Float,
    threshold: Float,
    mixWidth: Float,
    ifInside: BettingStrategy,
    ifOutside: BettingStrategy
): BettingStrategy {
    val distanceFromThreshold = threshold - handPercent
    return when {
        distanceFromThreshold > mixWidth  -> ifInside
        distanceFromThreshold < -mixWidth -> ifOutside
        else -> {
            val probabilityInside = (distanceFromThreshold + mixWidth) / (2 * mixWidth)
            if (Random.nextFloat() < probabilityInside) ifInside else ifOutside
        }
    }
}

private const val AGGRESSIVE_MIX_WIDTH = 0.04f
private const val PASSIVE_MIX_WIDTH = 0.06f