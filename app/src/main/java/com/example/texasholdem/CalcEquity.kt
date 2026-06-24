package com.example.texasholdem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.random.Random

suspend fun calcEquity(
    pocket: List<Card>,
    community: List<Card>,
    opponentsCount: Int
): Float = withContext(Dispatchers.Default) {
    require(pocket.size == 2)
    require(community.size in 3..5)
    require(opponentsCount >= 1)
    val iterations = 10_000 / opponentsCount
    require(iterations > 1000)
    val remainingDeck = deckPoker - (pocket + community)
    val missingCommunityCount = 5 - community.size
    val cardsNeededPerIteration = missingCommunityCount + opponentsCount * 2
    require(remainingDeck.size >= cardsNeededPerIteration)

    var wins = 0
    var draws = 0

    repeat(iterations) { iteration ->
        if (iteration % 250 == 0) {
            ensureActive()
        }

        val drawn = shuffle(remainingDeck, cardsNeededPerIteration)
        val fullCommunity = community + drawn.take(missingCommunityCount)
        var cursor = missingCommunityCount
        val myCombination = calcCombination(fullCommunity, pocket)

        var lose = false
        var draw = false

        repeat(opponentsCount) {
            if (!lose) {
                val oppPocket = drawn.subList(cursor, cursor + 2)
                cursor += 2

                val oppCombination = calcCombination(fullCommunity, oppPocket)
                val cmp = myCombination.compareTo(oppCombination)

                when {
                    cmp < 0 -> lose = true
                    cmp == 0 -> draw = true
                }
            }
        }

        when {
            lose -> {}
            draw -> draws++
            else -> wins++
        }
    }

    (wins.toFloat() + draws.toFloat()/2) / iterations
}

private fun shuffle(deck: List<Card>, count: Int): List<Card> {
    val pool = deck.toMutableList()
    val result = ArrayList<Card>(count)
    repeat(count) {
        val lastIndex = pool.lastIndex
        val random = Random.nextInt(pool.size)
        result.add(pool[random])
        pool[random] = pool[lastIndex]
        pool.removeAt(lastIndex)
    }
    return result
}