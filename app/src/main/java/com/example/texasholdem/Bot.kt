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