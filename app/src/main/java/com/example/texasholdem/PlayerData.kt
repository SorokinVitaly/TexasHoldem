package com.example.texasholdem


data class PlayerData(
    val name: String,
    val cards: List<Card> = emptyList(),
    val chips: Int = 0,
    val isActive: Boolean,
    val isDialer: Boolean = false,
    val lastBet: ActionType = ActionType.NoAction()
) {
    val isInGame = isActive && lastBet !is ActionType.Fold
    val footerText = lastBet.name

    fun payChips(payed: Int) = copy(chips = chips - payed)
    fun clearCards() = copy(cards = emptyList())
    fun sortCards() = copy(cards = cards.sorted())
    fun addCard(card: Card) = copy(cards = cards + card)
    fun removeCard(card: Card) = copy(cards = cards - card)
    fun setDialer() = copy(isDialer = true)
}