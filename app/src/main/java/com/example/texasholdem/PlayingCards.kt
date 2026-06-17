package com.example.texasholdem


enum class CardSuit {
    SPADE,
    CLUB,
    DIAMOND,
    HEART
}

enum class CardRank {
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    NINE,
    TEN,
    JACK,
    QUEEN,
    KING,
    ACE,
    JOKER
}

data class Card(
    val rank: CardRank,
    val suit: CardSuit = CardSuit.SPADE,
) : Comparable<Card> {
    val faceAssetName = createAssetName(false)
    val backAssetName = createAssetName(true)

    private fun createAssetName(isBack: Boolean): String {
        val prefix ="file:///android_asset/"
        val postfix = ".svg"
        if (isBack) {
            return "${prefix}CARD_BACK$postfix"
        }
        if (rank == CardRank.JOKER) {
            return "${prefix}JOKER-3$postfix"
        }
        val rankNumber = rank.ordinal + 2
        val rankName = when (rank) {
            CardRank.ACE -> "1"
            CardRank.JACK, CardRank.QUEEN, CardRank.KING -> "$rankNumber-${rank.name}"
            else -> "$rankNumber"
        }
        return "$prefix${suit.name}-$rankName$postfix"
    }

    override fun toString(): String =
        "${rank.name}-${suit.name}"

    override fun compareTo(other: Card): Int =
        compareValuesBy(this, other, { it.rank }, { it.suit })
}

private val jokerCard = Card(CardRank.JOKER)

private val standardCards = setOf(
    Card(CardRank.TWO, CardSuit.SPADE),
    Card(CardRank.THREE, CardSuit.SPADE),
    Card(CardRank.FOUR, CardSuit.SPADE),
    Card(CardRank.FIVE, CardSuit.SPADE),
    Card(CardRank.SIX, CardSuit.SPADE),
    Card(CardRank.SEVEN, CardSuit.SPADE),
    Card(CardRank.EIGHT, CardSuit.SPADE),
    Card(CardRank.NINE, CardSuit.SPADE),
    Card(CardRank.TEN, CardSuit.SPADE),
    Card(CardRank.JACK, CardSuit.SPADE),
    Card(CardRank.QUEEN, CardSuit.SPADE),
    Card(CardRank.KING, CardSuit.SPADE),
    Card(CardRank.ACE, CardSuit.SPADE),

    Card(CardRank.TWO, CardSuit.CLUB),
    Card(CardRank.THREE, CardSuit.CLUB),
    Card(CardRank.FOUR, CardSuit.CLUB),
    Card(CardRank.FIVE, CardSuit.CLUB),
    Card(CardRank.SIX, CardSuit.CLUB),
    Card(CardRank.SEVEN, CardSuit.CLUB),
    Card(CardRank.EIGHT, CardSuit.CLUB),
    Card(CardRank.NINE, CardSuit.CLUB),
    Card(CardRank.TEN, CardSuit.CLUB),
    Card(CardRank.JACK, CardSuit.CLUB),
    Card(CardRank.QUEEN, CardSuit.CLUB),
    Card(CardRank.KING, CardSuit.CLUB),
    Card(CardRank.ACE, CardSuit.CLUB),

    Card(CardRank.TWO, CardSuit.DIAMOND),
    Card(CardRank.THREE, CardSuit.DIAMOND),
    Card(CardRank.FOUR, CardSuit.DIAMOND),
    Card(CardRank.FIVE, CardSuit.DIAMOND),
    Card(CardRank.SIX, CardSuit.DIAMOND),
    Card(CardRank.SEVEN, CardSuit.DIAMOND),
    Card(CardRank.EIGHT, CardSuit.DIAMOND),
    Card(CardRank.NINE, CardSuit.DIAMOND),
    Card(CardRank.TEN, CardSuit.DIAMOND),
    Card(CardRank.JACK, CardSuit.DIAMOND),
    Card(CardRank.QUEEN, CardSuit.DIAMOND),
    Card(CardRank.KING, CardSuit.DIAMOND),
    Card(CardRank.ACE, CardSuit.DIAMOND),

    Card(CardRank.TWO, CardSuit.HEART),
    Card(CardRank.THREE, CardSuit.HEART),
    Card(CardRank.FOUR, CardSuit.HEART),
    Card(CardRank.FIVE, CardSuit.HEART),
    Card(CardRank.SIX, CardSuit.HEART),
    Card(CardRank.SEVEN, CardSuit.HEART),
    Card(CardRank.EIGHT, CardSuit.HEART),
    Card(CardRank.NINE, CardSuit.HEART),
    Card(CardRank.TEN, CardSuit.HEART),
    Card(CardRank.JACK, CardSuit.HEART),
    Card(CardRank.QUEEN, CardSuit.HEART),
    Card(CardRank.KING, CardSuit.HEART),
    Card(CardRank.ACE, CardSuit.HEART),
).toList()

@Suppress("unused")
val deckBlackJack by lazy { standardCards }

@Suppress("unused")
val deckPoker by lazy { standardCards }

@Suppress("unused")
val deckPokerWithJokers by lazy { standardCards + jokerCard + jokerCard }

@Suppress("unused")
val deckPreferans by lazy { standardCards.filter { it.rank >= CardRank.SEVEN } }

@Suppress("unused")
val deckDurak by lazy { standardCards.filter { it.rank >= CardRank.SIX } }