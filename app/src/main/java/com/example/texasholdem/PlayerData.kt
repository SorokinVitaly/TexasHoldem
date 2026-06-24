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

    fun payChips(payed: Int) = copy(chips = chips - payed)
    fun clearCards() = copy(cards = emptyList())
    fun addCard(card: Card) = copy(cards = cards + card)
    fun setDialer() = copy(isDialer = true)
}