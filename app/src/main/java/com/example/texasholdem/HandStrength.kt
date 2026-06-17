package com.example.texasholdem

enum class HandStrength {
    WEAK,
    DRAWING,
    MEDIUM,
    STRONG,
    MONSTER
}

fun calcHandStrength(combination: DrawCombination, isPreDraw: Boolean): HandStrength {
    val rank = combination.onHandCombination.highRank
    val isIncomplete = combination.incompleteCombination != IncompleteCombinationType.NO_INCOMPLETE
    return when (combination.onHandCombination.type) {
        CombinationType.ROYAL_FLUSH,
        CombinationType.STRAIGHT_FLUSH,
        CombinationType.FOUR_OF_A_KIND -> HandStrength.MONSTER

        CombinationType.FULL_HOUSE,
        CombinationType.FLUSH,
        CombinationType.STRAIGHT,
        CombinationType.THREE_OF_A_KIND -> HandStrength.STRONG

        CombinationType.TWO_PAIRS -> if (rank >= CardRank.TEN) HandStrength.STRONG else HandStrength.MEDIUM

        CombinationType.PAIR -> when {
            rank >= CardRank.JACK -> HandStrength.MEDIUM
            rank >= CardRank.EIGHT && isPreDraw -> HandStrength.DRAWING
            else -> HandStrength.WEAK
        }

        CombinationType.HIGH_CARD -> if (isIncomplete) HandStrength.DRAWING else HandStrength.WEAK
    }
}