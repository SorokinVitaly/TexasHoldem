package com.example.texasholdem

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


class ChenAnalyzer {
    private val strength = Array(13) { IntArray(13) }
    private val flatStrength: List<Int>
    private val values: List<Int>

    init {
        for (high in CardRank.TWO.ordinal ..CardRank.ACE.ordinal) {
            for (low in CardRank.TWO.ordinal ..high) {
                val s = (chenStrength(high, low) * 2.0f).roundToInt()
                strength[low][high] = s + 4
                strength[high][low] = s
            }
        }
        flatStrength = strength.flatMap { it.toList() }.sortedDescending()
        values = strength.flatMap { it.toList() }.sorted().distinct()
    }

    fun calcHandStrength(pocket: List<Card>): Int {
        require(pocket.size == 2)
        val (c1, c2) = pocket
        val high = max(c1.rank.ordinal, c2.rank.ordinal)
        val low =  min(c1.rank.ordinal, c2.rank.ordinal)
        return if (c1.suit == c2.suit) strength[low][high] else strength[high][low]
    }

    fun calcHandPercent(pocket: List<Card>): Float {
        require(pocket.size == 2)
        val (c1, c2) = pocket
        val high = max(c1.rank.ordinal, c2.rank.ordinal)
        val low =  min(c1.rank.ordinal, c2.rank.ordinal)
        val rating = if (c1.suit == c2.suit) strength[low][high] else strength[high][low]
        val count = flatStrength.indexOfFirst { it <= rating }
        return if (count < 0) 1f else count.toFloat() / 169f
    }

    private fun chenStrength(high: Int, low: Int): Float {
        require(high >= low)
        var score = when (high) {
            12 -> 10f
            11 -> 8f
            10 -> 7f
            9 -> 6f
            8 -> 5f
            7 -> 4.5f
            6 -> 4f
            5 -> 3.5f
            4 -> 3f
            3 -> 2.5f
            2 -> 2f
            1 -> 1.5f
            0 -> 1f
            else -> throw IllegalStateException("Wrong card")
        }
        if (high == low) {
            score = maxOf(score * 2, 5f)
            return score
        }
        val gap = high - low - 1
        score += when {
            gap <= 0 -> 0f
            gap == 1 -> -1f
            gap == 2 -> -2f
            gap == 3 -> -4f
            else     -> -5f
        }
        if (gap <= 1 && high < 10) score += 1f
        return score
    }
}