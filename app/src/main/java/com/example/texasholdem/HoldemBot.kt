package com.example.texasholdem

data class HandEvaluation(
    val strength: HandStrength,
    val drawProbability: Float = 0f,
    val boardDanger: BoardDanger = BoardDanger.SAFE
)

enum class BoardDanger {
    SAFE,           // борд не даёт готовых угроз
    POSSIBLE_BETTER, // борд позволяет оппоненту иметь равную/лучшую комбинацию того же типа
    BOARD_PLAYS      // моя лучшая комбинация полностью на борде — рука бесполезна для торговли
}

private enum class DrawType { NONE, GUTSHOT, OPEN_ENDED, FLUSH, COMBO }
private enum class StraightDraw { NONE, GUTSHOT, OPEN_ENDED }

private fun detectDraw(allCards: List<Card>): DrawType {
    val suitCounts = allCards.groupingBy { it.suit }.eachCount()
    val hasFlushDraw = suitCounts.values.any { it == 4 }
    val straightDraw = detectStraightDraw(allCards.map { it.rank.ordinal }.toSet())

    return when {
        hasFlushDraw && straightDraw != StraightDraw.NONE -> DrawType.COMBO
        hasFlushDraw                                       -> DrawType.FLUSH
        straightDraw == StraightDraw.OPEN_ENDED            -> DrawType.OPEN_ENDED
        straightDraw == StraightDraw.GUTSHOT                -> DrawType.GUTSHOT
        else                                                  -> DrawType.NONE
    }
}

private fun detectStraightDraw(ranks: Set<Int>): StraightDraw {
    val windows = listOf(listOf(14, 2, 3, 4, 5)) + (2..10).map { (it..it + 4).toList() }
    var foundGutshot = false

    for (window in windows) {
        if (window.count { it in ranks } == 4) {
            val missing = window.first { it !in ranks }
            val isEdge = missing == window.first() || missing == window.last()
            if (isEdge) return StraightDraw.OPEN_ENDED
            foundGutshot = true
        }
    }
    return if (foundGutshot) StraightDraw.GUTSHOT else StraightDraw.NONE
}

private fun outsFor(draw: DrawType): Int = when (draw) {
    DrawType.FLUSH      -> 9
    DrawType.OPEN_ENDED -> 8
    DrawType.GUTSHOT    -> 4
    DrawType.COMBO      -> 15 // ≈ 9 флеш + 8 стрит − 2 пересечение (приближение)
    DrawType.NONE       -> 0
}

private fun hitProbability(outs: Int, unseen: Int, cardsToCome: Int): Float {
    if (outs <= 0 || cardsToCome <= 0) return 0f
    val missAll = combinationsL(unseen - outs, cardsToCome).toDouble() /
            combinationsL(unseen, cardsToCome).toDouble()
    return (1.0 - missAll).toFloat()
}

private fun combinationsL(n: Int, k: Int): Long {
    if (k > n || k < 0 || n < 0) return 0L
    var result = 1L
    for (i in 0 until k) result = result * (n - i) / (i + 1)
    return result
}
private fun pocketCardsUsedInBest(
    pocket: List<Card>,
    communityCards: List<Card>,
    best5: List<Card>
): Int = best5.count { card -> pocket.any { it.rank == card.rank && it.suit == card.suit } }

private fun evaluateFiveCards(cards: List<Card>): Combination {
    val ranks = cards.map { it.rank }.sortedDescending()
    val suits = cards.map { it.suit }
    val groups = ranks.groupingBy { it }.eachCount()
        .entries.sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key })

    val isFlush = suits.toSet().size == 1
    val isWheel = ranks.toSet() == setOf(14, 2, 3, 4, 5)
    val isStraight = isWheel || (ranks.toSet().size == 5 && ranks.first() - ranks.last() == 4)
    val topRank = if (isWheel) 5 else ranks.first()

    val type = when {
        isFlush && isStraight && topRank == 14       -> CombinationType.ROYAL_FLUSH
        isFlush && isStraight                         -> CombinationType.STRAIGHT_FLUSH
        groups[0].value == 4                           -> CombinationType.FOUR_OF_A_KIND
        groups[0].value == 3 && groups[1].value == 2   -> CombinationType.FULL_HOUSE
        isFlush                                         -> CombinationType.FLUSH
        isStraight                                       -> CombinationType.STRAIGHT
        groups[0].value == 3                              -> CombinationType.THREE_OF_A_KIND
        groups[0].value == 2 && groups[1].value == 2      -> CombinationType.TWO_PAIRS
        groups[0].value == 2                                -> CombinationType.PAIR
        else                                                  -> CombinationType.HIGH_CARD
    }

    val highRank = when (type) {
        CombinationType.STRAIGHT, CombinationType.STRAIGHT_FLUSH,
        CombinationType.ROYAL_FLUSH -> topRank
        else -> groups[0].key
    }

    return Combination(type, Card(highRank, suits.first()))
}

private fun <T> combinationsOf(list: List<T>, k: Int): List<List<T>> {
    if (k == 0) return listOf(emptyList())
    if (list.isEmpty()) return emptyList()
    val head = list.first()
    val tail = list.drop(1)
    return combinationsOf(tail, k - 1).map { listOf(head) + it } + combinationsOf(tail, k)
}

private fun evaluateBestCombination(allCards: List<Card>): Pair<Combination, List<Card>> {
    return combinationsOf(allCards, 5)
        .map { it to evaluateFiveCards(it) }
        .maxWith(compareBy({ it.second.type.ordinal }, { it.second.highCard.rank }))
        .let { it.second to it.first }
}

private fun madeHandStrength(
    combination: Combination,
    pocketUsed: Int  // сколько pocket cards вошло в лучшую комбинацию (0, 1 или 2)
): HandStrength = when (combination.type) {

    CombinationType.ROYAL_FLUSH, CombinationType.STRAIGHT_FLUSH,
    CombinationType.FOUR_OF_A_KIND -> HandStrength.MONSTER

    CombinationType.FULL_HOUSE -> if (pocketUsed >= 1) HandStrength.MONSTER else HandStrength.STRONG

    CombinationType.FLUSH, CombinationType.STRAIGHT ->
        if (pocketUsed >= 1) HandStrength.STRONG else HandStrength.MEDIUM // борд может дать ту же руку любому

    CombinationType.THREE_OF_A_KIND -> when (pocketUsed) {
        2 -> HandStrength.MONSTER  // pocket pair + community card — скрытый трипс, идеален для торговли
        1 -> HandStrength.STRONG   // community pair + pocket card — виден частично
        else -> HandStrength.MEDIUM // трипс полностью на борде — у всех есть эта тройка
    }

    CombinationType.TWO_PAIRS -> when {
        pocketUsed == 2 && combination.highCard.rank >= 10 -> HandStrength.STRONG
        pocketUsed >= 1                                     -> HandStrength.MEDIUM
        else                                                  -> HandStrength.WEAK // обе пары на борде
    }

    CombinationType.PAIR -> when {
        pocketUsed == 2 && combination.highCard.rank >= 10 -> HandStrength.MEDIUM // карманная пара
        pocketUsed >= 1                                     -> HandStrength.WEAK
        else                                                  -> HandStrength.WEAK // пара полностью на борде
    }

    CombinationType.HIGH_CARD -> HandStrength.WEAK
}

private fun analyzeBoardDanger(
    communityCards: List<Card>,
    myBest: Combination,
    pocketUsed: Int
): BoardDanger {
    if (communityCards.size < 3) return BoardDanger.SAFE

    val boardOnlyBest = bestCombinationFromBoardAlone(communityCards)

    return when {
        // Моя лучшая рука полностью совпадает с тем что даёт борд — бесполезно для торговли
        pocketUsed == 0 -> BoardDanger.BOARD_PLAYS

        // Борд сам по себе уже содержит опасную комбинацию (флеш-/стрит-боард)
        boardOnlyBest.type.ordinal >= CombinationType.STRAIGHT.ordinal &&
                boardOnlyBest.type.ordinal >= myBest.type.ordinal -> BoardDanger.POSSIBLE_BETTER

        // На борде есть пара/трипс — у кого-то может быть фулл-хаус/каре
        hasBoardPair(communityCards) && myBest.type.ordinal < CombinationType.FULL_HOUSE.ordinal ->
            BoardDanger.POSSIBLE_BETTER

        // 3-4 карты одной масти на борде — возможен флеш у оппонента
        hasFlushPotential(communityCards) && myBest.type.ordinal < CombinationType.FLUSH.ordinal ->
            BoardDanger.POSSIBLE_BETTER

        // 3-4 последовательных карты на борде — возможен стрит
        hasStraightPotential(communityCards) && myBest.type.ordinal < CombinationType.STRAIGHT.ordinal ->
            BoardDanger.POSSIBLE_BETTER

        else -> BoardDanger.SAFE
    }
}

private fun bestCombinationFromBoardAlone(communityCards: List<Card>): Combination {
    if (communityCards.size < 5) {
        // Меньше 5 карт на борде — берём лучшую частичную оценку (для FLOP)
        return evaluatePartialCards(communityCards)
    }
    return combinationsOf(communityCards, 5).map { evaluateFiveCards(it) }
        .maxWith(compareBy({ it.type.ordinal }, { it.highCard.rank }))
}

private fun evaluatePartialCards(cards: List<Card>): Combination {
    // На флопе всего 3 community карты — combination напрямую без подвыборки
    return evaluateFiveCards(cards + cards.take(5 - cards.size)) // упрощение для < 5 карт
        .let { Combination(minOf(it.type, CombinationType.THREE_OF_A_KIND), it.highCard) }
}

private fun hasBoardPair(communityCards: List<Card>): Boolean =
    communityCards.groupingBy { it.rank }.eachCount().any { it.value >= 2 }

private fun hasFlushPotential(communityCards: List<Card>): Boolean =
    communityCards.groupingBy { it.suit }.eachCount().any { it.value >= 3 }

private fun hasStraightPotential(communityCards: List<Card>): Boolean {
    val ranks = communityCards.map { it.rank }.toSet()
    val windows = listOf(listOf(14, 2, 3, 4, 5)) + (2..10).map { (it..it + 4).toList() }
    return windows.any { window -> window.count { it in ranks } >= 3 }
}

private fun resolveStrategy(
    evaluation: HandEvaluation,
    numOfRaise: Int,
    isLatePosition: Boolean,
    isFacingBet: Boolean,
    potOdds: Float
): BettingStrategy {
    // Понижаем силу руки если борд опасен
    val adjustedStrength = when (evaluation.boardDanger) {
        BoardDanger.BOARD_PLAYS      -> minOf(evaluation.strength, HandStrength.MEDIUM)
        BoardDanger.POSSIBLE_BETTER  -> downgrade(evaluation.strength)
        BoardDanger.SAFE             -> evaluation.strength
    }

    return when (adjustedStrength) {
        HandStrength.MONSTER -> BettingStrategy.AGGRESSIVE

        HandStrength.STRONG -> when {
            !isFacingBet || numOfRaise == 0 -> BettingStrategy.AGGRESSIVE
            else                              -> BettingStrategy.PASSIVE
        }

        HandStrength.MEDIUM -> when {
            !isFacingBet || numOfRaise <= 1 -> BettingStrategy.PASSIVE
            else                              -> BettingStrategy.DROP
        }

        HandStrength.DRAWING -> {
            val worthIt = evaluation.drawProbability > potOdds
            when {
                !isFacingBet               -> BettingStrategy.PASSIVE
                worthIt && numOfRaise <= 1 -> BettingStrategy.PASSIVE
                else                         -> BettingStrategy.DROP
            }
        }

        HandStrength.WEAK -> when {
            !isFacingBet && isLatePosition && numOfRaise == 0 -> BettingStrategy.AGGRESSIVE
            !isFacingBet                                        -> BettingStrategy.PASSIVE
            else                                                  -> BettingStrategy.DROP
        }
    }
}

private fun downgrade(strength: HandStrength): HandStrength {
    val ordinal = (strength.ordinal - 1).coerceAtLeast(0)
    return HandStrength.entries[ordinal]
}

private fun evaluateHand(
    cards: List<Card>,
    communityCards: List<Card>,
    round: RoundType
): HandEvaluation {
    if (round == RoundType.PRE_FLOP) {
        return HandEvaluation(preFlopStrength(cards))
    }

    val allCards = cards + communityCards
    val (combination, best5) = evaluateBestCombination(allCards)
    val pocketUsed = pocketCardsUsedInBest(cards, communityCards, best5)
    val madeStrength = madeHandStrength(combination, pocketUsed)
    val boardDanger = analyzeBoardDanger(communityCards, combination, pocketUsed)

    if (round == RoundType.RIVER || madeStrength >= HandStrength.STRONG) {
        return HandEvaluation(madeStrength, boardDanger = boardDanger)
    }

    val draw = detectDraw(allCards)
    if (draw == DrawType.NONE) return HandEvaluation(madeStrength, boardDanger = boardDanger)

    val cardsToCome = if (round == RoundType.FLOP) 2 else 1
    val unseen = 52 - allCards.size
    val probability = hitProbability(outsFor(draw), unseen, cardsToCome)

    return HandEvaluation(HandStrength.DRAWING, probability, boardDanger)
}