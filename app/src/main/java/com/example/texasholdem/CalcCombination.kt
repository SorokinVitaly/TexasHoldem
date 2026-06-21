package com.example.texasholdem

import com.example.texasholdem.Card

fun calcCombinationFiveCards(cards: List<Card>): Combination {
    require(cards.size == 5)

    val ranks = cards.map { it.rank }
    val isFlush = cards.all { it.suit == cards[0].suit }

    fun calcStraight(highRank: CardRank): Combination =
        if (isFlush) {
            if (ranks[0] == CardRank.TEN) {
                Combination(CombinationType.ROYAL_FLUSH, highRank, members = cards)
            } else {
                Combination(CombinationType.STRAIGHT_FLUSH, highRank, members = cards)
            }
        } else {
            Combination(CombinationType.STRAIGHT, highRank, members = cards)
        }

    val ordinalList = ranks.map { it.ordinal - ranks[0].ordinal }
    if (ordinalList == listOf(0, 1, 2, 3, 4)) {
        return calcStraight(ranks[4])
    }
    if (ordinalList == listOf(0, 1, 2, 3, 12)) {
        return calcStraight(ranks[3])
    }
    if (isFlush) {
        return Combination(CombinationType.FLUSH, ranks[4], members = cards)
    }

    val rank1 = ranks[1]
    val rank3 = ranks[3]
    val count1 = ranks.count { it == rank1 }
    val count3 = ranks.count { it == rank3 }
    return when {
        count1 == 4 -> {
            val members = cards.filter { it.rank == rank1 }
            Combination(
                type = CombinationType.FOUR_OF_A_KIND,
                highRank = rank1,
                members = members,
                kickers = Kickers(cards, members)
            )
        }
        count1 == 3  && count3 == 2 -> {
            Combination(
                type = CombinationType.FULL_HOUSE,
                highRank = rank1,
                lowRank =  rank3,
                members = cards
            )
        }
        count1 == 2  && count3 == 3 -> {
            Combination(
                type = CombinationType.FULL_HOUSE,
                highRank = rank3,
                lowRank =  rank1,
                members = cards
            )
        }
        count1 == 3 -> {
            val members = cards.filter { it.rank == rank1 }
            Combination(
                type = CombinationType.THREE_OF_A_KIND,
                highRank = rank1,
                members = members,
                kickers = Kickers(cards, members)
            )
        }
        count3 == 3 -> {
            val members = cards.filter { it.rank == rank3 }
            Combination(
                type = CombinationType.THREE_OF_A_KIND,
                highRank = rank3,
                members = members,
                kickers = Kickers(cards, members)
            )
        }
        count1 == 2  && count3 == 2 -> {
            val members = cards.filter { it.rank == rank1 || it.rank == rank3 }
            Combination(
                type = CombinationType.TWO_PAIRS,
                highRank = rank3,
                lowRank = rank1,
                members = members,
                kickers = Kickers(cards, members)
            )
        }
        count1 == 2 -> {
            val members = cards.filter { it.rank == rank1 }
            Combination(
                type = CombinationType.PAIR,
                highRank = rank1,
                members = members,
                kickers = Kickers(cards, members)
            )
        }
        count3 == 2 -> {
            val members = cards.filter { it.rank == rank3 }
            Combination(
                type = CombinationType.PAIR,
                highRank = rank3,
                members = members,
                kickers = Kickers(cards, members)
            )
        }
        else -> {
            val members = cards.drop(4)
            Combination(
                type = CombinationType.HIGH_CARD,
                highRank = ranks[4],
                members = members,
                kickers = Kickers(cards, members)
            )
        }
    }
}

private val noCombination = Combination(
    type = CombinationType.HIGH_CARD,
    highRank = CardRank.TWO,
    members = emptyList()
)

fun calcCombinationSixCards(cards: List<Card>, pocket: List<Card>): Combination {
    require(cards.size == 6)
    var bestCombination = noCombination
    cards.forEach { card ->
        calcCombinationFiveCards(cards - card).let {
            if (it.compareWithPocket(bestCombination, pocket) > 0) {
                bestCombination = it
            }
        }
    }
    //compareWithPocket(other: Combination, pocket: List<Card>)
    return bestCombination
}

fun calcCombinationSevenCards(cards: List<Card>, pocket: List<Card>): Combination {
    require(cards.size == 7)
    var bestCombination = noCombination
    cards.forEach { card ->
        calcCombinationSixCards(cards - card, pocket).let {
            if (it.compareWithPocket(bestCombination, pocket) > 0) {
                bestCombination = it
            }
        }
    }
    return bestCombination
}