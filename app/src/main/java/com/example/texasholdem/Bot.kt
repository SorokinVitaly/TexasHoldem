package com.example.texasholdem

enum class BettingStrategy {
    DROP,
    PASSIVE,
    AGGRESSIVE
}

fun selectBotStrategy(
    strength: HandStrength,
    numOfRaise: Int,
    isLatePosition: Boolean,
    drawIsWorthIt: Boolean,
    isFacingBet: Boolean,
    isPreDraw: Boolean
): BettingStrategy {
    return when (strength) {
        HandStrength.MONSTER -> BettingStrategy.AGGRESSIVE

        HandStrength.STRONG -> when {
            !isFacingBet && isLatePosition -> BettingStrategy.AGGRESSIVE
            !isFacingBet                   -> BettingStrategy.PASSIVE
            isFacingBet && numOfRaise == 0 -> BettingStrategy.AGGRESSIVE
            else                           -> BettingStrategy.PASSIVE
        }

        HandStrength.MEDIUM -> when {
            !isFacingBet                                 -> BettingStrategy.PASSIVE
            isFacingBet && numOfRaise == 0               -> BettingStrategy.PASSIVE
            isFacingBet && numOfRaise == 1 && !isPreDraw -> BettingStrategy.PASSIVE
            else                                         -> BettingStrategy.DROP
        }

        HandStrength.DRAWING -> when {
            !isFacingBet                                                  -> BettingStrategy.PASSIVE
            isFacingBet && drawIsWorthIt && numOfRaise == 0               -> BettingStrategy.PASSIVE
            isFacingBet && drawIsWorthIt && numOfRaise == 1 && !isPreDraw -> BettingStrategy.PASSIVE
            else                                                          -> BettingStrategy.DROP
        }

        HandStrength.WEAK -> if (isFacingBet) BettingStrategy.DROP else {
            if (isLatePosition && numOfRaise == 0 && isPreDraw) BettingStrategy.AGGRESSIVE
            else BettingStrategy.PASSIVE
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