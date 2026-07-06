package com.example.texasholdem

fun calcIncompleteCombination(
    community: List<Card>,
    pocket: List<Card>,
    combination: Combination
): IncompleteCombination {
    require(pocket.size == 2 && community.size in 3..5)
    val allCards = (community + pocket).sorted()
    return when (allCards.size) {
        5 -> calcIncompleteCombinationFiveCards(allCards, combination)
        6 -> calcIncompleteCombinationSixCards(allCards, pocket, combination)
        7 -> IncompleteCombination()
        else -> throw IllegalStateException("Invalid number of cards")
    }
}

fun calcIncompleteCombinationFiveCards(
    cards: List<Card>,
    combination: Combination
): IncompleteCombination {
    require(cards.size == 5)
    if (combination.type >= CombinationType.TWO_PAIRS ||
        (combination.type == CombinationType.PAIR && combination.highRank >= CardRank.EIGHT)) {
        return IncompleteCombination()
    }
    findFourToStraightFlush(cards)?.let {
        return IncompleteCombination(IncompleteCombinationType.FOUR_TO_STRAIGHT_FLUSH, it)
    }
    findThreeToStraightFlush(cards)?.let {
        return IncompleteCombination(IncompleteCombinationType.THREE_TO_STRAIGHT_FLUSH, it)
    }
    findFourToFlush(cards)?.let {
        return IncompleteCombination(IncompleteCombinationType.FOUR_TO_FLUSH, it)
    }
    findFourToStraightOpen(cards)?.let {
        return IncompleteCombination(IncompleteCombinationType.FOUR_TO_STRAIGHT_OPEN, it)
    }
    findFourToStraight(cards)?.let {
        return IncompleteCombination(IncompleteCombinationType.FOUR_TO_STRAIGHT, it)
    }
    return IncompleteCombination()
}

fun calcIncompleteCombinationSixCards(
    cards: List<Card>,
    pocket: List<Card>,
    combination: Combination
): IncompleteCombination {
    require(cards.size == 6)
    var bestCombination = IncompleteCombination()
    cards.forEach { card ->
        calcIncompleteCombinationFiveCards(cards - card, combination).let {
            if (it.compareWithPocket(bestCombination, pocket) > 0) {
                bestCombination = it
            }
        }
    }
    return bestCombination
}

// Functions to find incomplete combination. Return set of cards to draw or null
private fun findFourToStraightFlush(cards: List<Card>): List<Card>? {
    val sameSuitCards = findSameSuit(cards, 4) ?: return null
    return if (sameSuitCards[3].rank.ordinal - sameSuitCards[0].rank.ordinal != 3 ||
        sameSuitCards[3].rank == CardRank.ACE
    ) null else sameSuitCards
}

private fun findThreeToStraightFlush(cards: List<Card>): List<Card>? {
    val sameSuitCards = findSameSuit(cards, 3) ?: return null
    return if (sameSuitCards[2].rank.ordinal - sameSuitCards[0].rank.ordinal != 2) null
    else sameSuitCards
}

private fun findFourToFlush(cards: List<Card>): List<Card>? {
    val sameSuitCards = findSameSuit(cards, 4) ?: return null
    return sameSuitCards
}

private fun findFourToStraightOpen(cards: List<Card>): List<Card>? {
    for (skip in cards) {
        val remaining = cards - skip
        val firstCard = remaining[0]
        val ordinalList = remaining.map { it.rank.ordinal - firstCard.rank.ordinal }
        if (ordinalList == listOf(0, 1, 2, 3) && remaining[3].rank != CardRank.ACE) {
            return remaining
        }
    }
    return null
}

private fun findFourToStraight(cards: List<Card>): List<Card>? {
    for (skip in cards) {
        val remaining = (cards - skip)
        val ranks = remaining.map { it.rank }
        if (ranks[3].ordinal - ranks[0].ordinal <= 4) return remaining
        if (ranks[3] == CardRank.ACE && ranks[2] <= CardRank.FIVE) return remaining
    }
    return null
}

private fun findSameSuit(cards: List<Card>, numCards: Int ): List<Card>? {
    require(numCards > 1)
    val lastIndex = 5 - numCards
    for (i in 0..lastIndex) {
        val sameSuitCards = cards.filter { it.suit == cards[i].suit }
        if (sameSuitCards.size == numCards) {
            return sameSuitCards
        }
    }
    return null
}