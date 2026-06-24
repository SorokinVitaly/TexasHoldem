package com.example.texasholdem

enum class CombinationType {
    HIGH_CARD,
    PAIR,
    TWO_PAIRS,
    THREE_OF_A_KIND,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    FOUR_OF_A_KIND,
    STRAIGHT_FLUSH,
    ROYAL_FLUSH
}

enum class IncompleteCombinationType {
    NO_INCOMPLETE,
    THREE_TO_STRAIGHT_FLUSH,
    FOUR_TO_STRAIGHT,
    FOUR_TO_STRAIGHT_OPEN,
    FOUR_TO_FLUSH
}

data class Kickers(val list: List<CardRank> = emptyList()) : Comparable<Kickers> {
    constructor(cards: List<Card>, members: List<Card>) : this((cards - members).map { it.rank })

    override fun compareTo(other: Kickers): Int {
        val size = list.size
        if (size != other.list.size) {
            return size - other.list.size
        }
        for (i in (size - 1) downTo 0) {
            val res = list[i].compareTo(other.list[i])
            if (res != 0) return res
        }
        return 0
    }
}

data class Combination(
    val type: CombinationType,
    val highRank: CardRank,
    val lowRank: CardRank = highRank, // lowRank == highRank except cases of FULL_HOUSE or TWO_PAIRS
    val members: List<Card>,
    val kickers: Kickers = Kickers()
) : Comparable<Combination> {

    override fun compareTo(other: Combination): Int =
        compareValuesBy(
            this,
            other,
            { it.type },
            { it.highRank },
            { it.lowRank },
            { it.kickers }
            )

    fun compareWithPocket(other: Combination, pocket: List<Card>): Int {
        val res = compareValuesBy(
            this,
            other,
            { it.type },
            { it.highRank },
            { it.lowRank }
        )
        return if (res != 0) {
            res
        } else {
            (members - pocket).size - (other.members - pocket).size
        }
    }
}

class IncompleteCombination(
    val type: IncompleteCombinationType = IncompleteCombinationType.NO_INCOMPLETE,
    val members: List<Card> = emptyList()
) : Comparable<IncompleteCombination> {

    override fun compareTo(other: IncompleteCombination): Int {
        return type.ordinal - other.type.ordinal
    }

    fun compareWithPocket(other: IncompleteCombination, pocket: List<Card>): Int {
        val res = compareTo(other)
        return if (res != 0) {
            res
        } else {
            (members - pocket).size - (other.members - pocket).size
        }
    }
}

class PreCalculatedData(
    val combination: Combination,
    val incompleteCombination: IncompleteCombination = IncompleteCombination(),
    val opponentsCount: Int = 0,
    val equity: Float = 0f
)