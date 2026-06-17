package com.example.texasholdem


data class PlayerData(
    val name: String,
    val cards: List<Card> = emptyList(),
    val selectedCards: Set<Card> = emptySet(),
    val chips: Int = 0,
    val isActive: Boolean,
    val isDialer: Boolean = false,
    val lastDraw: ActionType = ActionType.NoAction(),
    val lastBet: ActionType = ActionType.NoAction()
) {
    val isInGame = isActive && lastBet !is ActionType.Fold
    val footerText = if (lastDraw !is ActionType.Draw) {
        ""
    } else {
        "Draw count: ${lastDraw.number} "
    } + lastBet.name

    fun payChips(payed: Int) = copy(chips = chips - payed)
    fun clearCards() = copy(cards = emptyList(), selectedCards = emptySet())
    fun clearSelected() = copy(selectedCards = emptySet())
    fun sortCards() = copy(cards = cards.sorted())
    fun addCard(card: Card) = copy(cards = cards + card)
    fun removeCard(card: Card) = copy(cards = cards - card)
    fun setSelected(cardsForDraw: List<Card>) = copy(selectedCards = cardsForDraw.toSet())
    fun setDialer() = copy(isDialer = true)
}