package com.example.texasholdem

fun calcCombination(cards: List<Card>): Combination {
    require(cards.size == 5)

    val ranks = cards.map { it.rank }
    val isFlush = cards.all { it.suit == cards[0].suit }

    fun calcStraight(highRank: CardRank): Combination =
        if (isFlush) {
            if (ranks[0] == CardRank.TEN) {
                Combination(CombinationType.ROYAL_FLUSH, highRank)
            } else {
                Combination(CombinationType.STRAIGHT_FLUSH, highRank)
            }
        } else {
            Combination(CombinationType.STRAIGHT, highRank)
        }

    val ordinalList = ranks.map { it.ordinal - ranks[0].ordinal }
    if (ordinalList == listOf(0, 1, 2, 3, 4)) {
        return calcStraight(ranks[4])
    }
    if (ordinalList == listOf(0, 1, 2, 3, 12)) {
        return calcStraight(ranks[3])
    }
    if (isFlush) {
        return Combination(CombinationType.FLUSH, ranks[4])
    }

    val rank1 = ranks[1]
    val rank3 = ranks[3]
    val count1 = ranks.count { it == rank1 }
    val count3 = ranks.count { it == rank3 }
    return when {
        count1 == 4 -> {
            Combination(
                type = CombinationType.FOUR_OF_A_KIND,
                highRank = rank1
            )
        }
        count1 == 3  && count3 == 2 -> {
            Combination(
                type = CombinationType.FULL_HOUSE,
                highRank = rank1,
                lowRank =  rank3
            )
        }
        count1 == 2  && count3 == 3 -> {
            Combination(
                type = CombinationType.FULL_HOUSE,
                highRank = rank3,
                lowRank =  rank1
            )
        }
        count1 == 3 -> {
            Combination(
                type = CombinationType.THREE_OF_A_KIND,
                highRank = rank1
            )
        }
        count3 == 3 -> {
            Combination(
                type = CombinationType.THREE_OF_A_KIND,
                highRank = rank3
            )
        }
        count1 == 2  && count3 == 2 -> {
            Combination(
                type = CombinationType.TWO_PAIRS,
                highRank = rank3,
                lowRank = rank1,
                kickers = Kickers(ranks.filter { it != rank1 && it != rank3 })
            )
        }
        count1 == 2 -> {
            Combination(
                type = CombinationType.PAIR,
                highRank = rank1,
                kickers = Kickers(ranks.filter { it != rank1 })
            )
        }
        count3 == 2 -> {
            Combination(
                type = CombinationType.PAIR,
                highRank = rank3,
                kickers = Kickers(ranks.filter { it != rank3 })
            )
        }
        else -> {
            Combination(
                type = CombinationType.HIGH_CARD,
                highRank = ranks[4],
                kickers = Kickers(ranks.take(4))
            )
        }
    }
}