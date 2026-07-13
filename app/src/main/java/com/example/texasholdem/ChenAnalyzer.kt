package com.example.texasholdem

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


typealias ChenStrength = Int

class ChenAnalyzer {
    private val strength = Array(13) { IntArray(13) }
    val values: List<ChenStrength>

    init {
        for (high in CardRank.TWO.ordinal ..CardRank.ACE.ordinal) {
            for (low in CardRank.TWO.ordinal ..high) {
                val s = (chenStrength(high, low) * 2.0f).roundToInt()
                strength[low][high] = s + 4
                strength[high][low] = s
            }
        }
        values = strength.flatMap { it.toList() }.sorted().distinct()
        require(values.size == 27)
    }

    fun calcHandStrength(pocket: List<Card>): ChenStrength {
        require(pocket.size == 2)
        val (c1, c2) = pocket
        val high = max(c1.rank.ordinal, c2.rank.ordinal)
        val low =  min(c1.rank.ordinal, c2.rank.ordinal)
        return if (c1.suit == c2.suit) strength[low][high] else strength[high][low]
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