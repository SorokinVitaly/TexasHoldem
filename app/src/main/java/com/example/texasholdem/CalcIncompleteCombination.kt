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
    if (combination.type >= CombinationType.TWO_PAIRS) {
        return IncompleteCombination()
    }
    findFourToFlush(cards)?.let {
        return IncompleteCombination(IncompleteCombinationType.FOUR_TO_FLUSH, it)
    }
    findFourToStraightOpen(cards)?.let {
        return IncompleteCombination(IncompleteCombinationType.FOUR_TO_STRAIGHT_OPEN, it)
    }
    if (combination.type == CombinationType.PAIR) {
        return IncompleteCombination()
    }
    findFourToStraight(cards)?.let {
        return IncompleteCombination(IncompleteCombinationType.FOUR_TO_STRAIGHT, it)
    }
    findThreeToStraightFlush(cards)?.let {
        return IncompleteCombination(IncompleteCombinationType.THREE_TO_STRAIGHT_FLUSH, it)
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

// Functions to find incomplete combination. Return members or null
private fun findFourToFlush(cards: List<Card>): List<Card>? {
    val sameSuitCards = findSameSuit(cards, 4) ?: return null
    return sameSuitCards
}

private fun findFourToStraightOpen(cards: List<Card>): List<Card>? {
    for (skip in cards) {
        val remaining = cards.filter { it != skip }
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
        val remaining = cards.filter { it != skip }
        if (remaining[3].rank.ordinal - remaining[0].rank.ordinal <= 4) return remaining
        if (remaining[3].rank == CardRank.ACE && remaining[2].rank <= CardRank.FIVE) return remaining
    }
    return null
}

private fun findThreeToStraightFlush(cards: List<Card>): List<Card>? {
    val sameSuitCards = findSameSuit(cards, 3) ?: return null
    return if (sameSuitCards[2].rank.ordinal - sameSuitCards[0].rank.ordinal > 4) null
    else sameSuitCards
}

private fun findSameSuit(cards: List<Card>, numCards: Int ): List<Card>? {
    for (i in 0 until 5 - numCards) {
        val testList = cards.filter { it.suit == cards[i].suit }
        if (testList.size == numCards) {
            return testList
        }
    }
    return null
}