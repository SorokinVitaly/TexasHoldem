package com.example.texasholdem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.random.Random


suspend fun calcEquity(
    pocket: List<Card>,
    community: List<Card>
): Float = withContext(Dispatchers.Default) {
    require(pocket.size == 2)
    require(community.size in 3..5)
    val iterations = 10_000
    val remainingDeck = deckPoker - (pocket + community)
    val missingCommunityCount = 5 - community.size
    var wins = 0
    var draws = 0

    repeat(iterations) { iteration ->
        if (iteration % 250 == 0) {
            ensureActive()
        }
        val drawn = shuffle(remainingDeck, missingCommunityCount + 2)
        val fullCommunity = community + drawn.take(missingCommunityCount)
        val oppPocket = drawn.takeLast(2)
        val myCombination = calcCombination(fullCommunity, pocket)
        val oppCombination = calcCombination(fullCommunity, oppPocket)

        val cmp = myCombination.compareTo(oppCombination)
        when {
            cmp < 0 -> {}
            cmp == 0 -> draws++
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