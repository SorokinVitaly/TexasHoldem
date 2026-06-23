package com.example.texasholdem

enum class BettingStrategy {
    DROP,
    PASSIVE,
    AGGRESSIVE
}

data class TableFactor(
    val numOfRaise: Int,
    val isLatePosition: Boolean,
    val isFacingBet: Boolean,
    val semiBluffPotential: Boolean,
    val tableIsAggressive: Boolean,
    val tableIsPassive: Boolean
)

fun preFlopStrength(pocket: List<Card>): HandStrength {
    val (c1, c2) = pocket
    val isPair = c1.rank == c2.rank
    val isSuited = c1.suit == c2.suit
    val gap = c2.rank.ordinal - c1.rank.ordinal

    return when {
        isPair && c2.rank >= CardRank.TEN                   -> HandStrength.MONSTER
        isPair && c2.rank >= CardRank.SEVEN                 -> HandStrength.STRONG
        c2.rank == CardRank.ACE && c1.rank >= CardRank.TEN  -> HandStrength.STRONG
        isPair                                              -> HandStrength.MEDIUM
        c2.rank >= CardRank.JACK && c1.rank >= CardRank.TEN -> HandStrength.MEDIUM
        isSuited && gap <= 2 && c1.rank >= CardRank.SIX     -> HandStrength.DRAWING
        else                                                -> HandStrength.WEAK
    }
}

private const val MONSTER_EQUITY = 0.85f
private const val VALUE_RAISE_EDGE = 0.30f
private const val MEDIUM_EDGE = 0.10f
private const val MARGINAL_CALL_EDGE = 0f

fun equityToStrength(equity: Float, potOdds: Float): HandStrength {
    val edge = equity - potOdds
    return when {
        equity >= MONSTER_EQUITY    -> HandStrength.MONSTER
        edge >= VALUE_RAISE_EDGE    -> HandStrength.STRONG
        edge >= MEDIUM_EDGE         -> HandStrength.MEDIUM
        edge >= MARGINAL_CALL_EDGE  -> HandStrength.DRAWING
        else                        -> HandStrength.WEAK
    }
}

fun resolveStrategy(
    strength: HandStrength,
    tableFactor: TableFactor
): BettingStrategy {
    val raiseThreshold = if (tableFactor.tableIsAggressive) 1 else 2
    if (strength == HandStrength.MONSTER) {
        return  BettingStrategy.AGGRESSIVE
    }
    if (strength == HandStrength.STRONG) {
        return if (!tableFactor.isFacingBet || tableFactor.numOfRaise < raiseThreshold) {
            BettingStrategy.AGGRESSIVE
        } else {
            BettingStrategy.PASSIVE
        }
    }

    val goodTableFactor = !tableFactor.isFacingBet &&
            tableFactor.isLatePosition &&
            tableFactor.numOfRaise == 0

    if (strength == HandStrength.MEDIUM) {
        if (goodTableFactor) {
            return BettingStrategy.AGGRESSIVE
        }
        return if (!tableFactor.isFacingBet || tableFactor.numOfRaise < raiseThreshold) {
            BettingStrategy.PASSIVE
        } else {
            BettingStrategy.DROP
        }
    }

    if (strength == HandStrength.DRAWING) {
        return if (!tableFactor.isFacingBet || tableFactor.numOfRaise == 0) {
            BettingStrategy.PASSIVE
        } else {
            BettingStrategy.DROP
        }
    }

    return when {
        goodTableFactor &&
                (tableFactor.semiBluffPotential ||
                        tableFactor.tableIsPassive) -> BettingStrategy.AGGRESSIVE
        !tableFactor.isFacingBet                    -> BettingStrategy.PASSIVE
        else                                        -> BettingStrategy.DROP
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