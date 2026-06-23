package com.example.texasholdem

enum class HandStrength {
    WEAK,
    DRAWING,
    MEDIUM,
    STRONG,
    MONSTER
}

fun calcStrength(pocket: List<Card>, community: List<Card>, round: RoundType): HandStrength {
    require(pocket.size == 2)
    when (round) {
        RoundType.PRE_FLOP -> {
            require(community.isEmpty())
            return preFlopStrength(pocket.sorted())
        }
        RoundType.FLOP -> {
            require(community.size == 3)
        }
        RoundType.TURN -> {
            require(community.size == 4)
        }
        RoundType.RIVER -> {
            require(community.size == 5)
        }
    }



    return HandStrength.WEAK
}

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

fun calcHandStrength(
    combination: Combination,
    incompleteCombination: IncompleteCombination,
    isPreDraw: Boolean
): HandStrength {
    val rank = combination.highRank
    val isIncomplete = incompleteCombination.type != IncompleteCombinationType.NO_INCOMPLETE
    return when (combination.type) {
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