package com.example.texasholdem

class SavedState(
    val screenState: ScreenState,
    val currentBet: Int,
    val numOfRaise: Int,
    val playerIndex: Int,
    val round: RoundType,
    val deck: List<Card>,
)

fun saveSnapshot(
    localData: LocalDataRepository,
    history: History,
    screenState: ScreenState,
    currentBet: Int,
    numOfRaise: Int,
    playerIndex: Int,
    round: RoundType,
    deck: List<Card>
) {
    val players = screenState.players
    with (localData) {
        player0Name = players[0].name
        player0Cards = Card.serializeList(players[0].cards)
        player0Chips = players[0].chips
        player0IsActive = players[0].isActive
        player0LastBet = players[0].lastBet.serialize()

        player1Name = players[1].name
        player1Cards = Card.serializeList(players[1].cards)
        player1Chips = players[1].chips
        player1IsActive = players[1].isActive
        player1LastBet = players[1].lastBet.serialize()

        player2Name = players[2].name
        player2Cards = Card.serializeList(players[2].cards)
        player2Chips = players[2].chips
        player2IsActive = players[2].isActive
        player2LastBet = players[2].lastBet.serialize()

        player3Name = players[3].name
        player3Cards = Card.serializeList(players[3].cards)
        player3Chips = players[3].chips
        player3IsActive = players[3].isActive
        player3LastBet = players[3].lastBet.serialize()

        player4Name = players[4].name
        player4Cards = Card.serializeList(players[4].cards)
        player4Chips = players[4].chips
        player4IsActive = players[4].isActive
        player4LastBet = players[4].lastBet.serialize()

        player5Name = players[5].name
        player5Cards = Card.serializeList(players[5].cards)
        player5Chips = players[5].chips
        player5IsActive = players[5].isActive
        player5LastBet = players[5].lastBet.serialize()

        communityCards = Card.serializeList(screenState.communityCards)
        bankChips = screenState.bankChips
        isResetAvailable = screenState.isResetAvailable
    }
    localData.currentBet = currentBet
    localData.numOfRaise = numOfRaise
    localData.playerIndex = playerIndex
    localData.round = round.ordinal
    localData.deck = Card.serializeList(deck)
    localData.history = history.serialize()
}

fun restoreSnapshot(
    localData: LocalDataRepository,
    history: History
): SavedState {
    history.unserialize(localData.history)

    val screenState = with (localData) {
        val player0 = PlayerData(
            name = player0Name,
            cards = Card.unserializeList(player0Cards),
            chips = player0Chips,
            isActive = player0IsActive,
            isDialer = dealerIndex == 0,
            lastBet = ActionType.unserialize(player0LastBet)
        )
        val player1 = PlayerData(
            name = player1Name,
            cards = Card.unserializeList(player1Cards),
            chips = player1Chips,
            isActive = player1IsActive,
            isDialer = dealerIndex == 1,
            lastBet = ActionType.unserialize(player1LastBet)
        )
        val player2 = PlayerData(
            name = player2Name,
            cards = Card.unserializeList(player2Cards),
            chips = player2Chips,
            isActive = player2IsActive,
            isDialer = dealerIndex == 2,
            lastBet = ActionType.unserialize(player2LastBet)
        )
        val player3 = PlayerData(
            name = player3Name,
            cards = Card.unserializeList(player3Cards),
            chips = player3Chips,
            isActive = player3IsActive,
            isDialer = dealerIndex == 3,
            lastBet = ActionType.unserialize(player3LastBet)
        )
        val player4 = PlayerData(
            name = player4Name,
            cards = Card.unserializeList(player4Cards),
            chips = player4Chips,
            isActive = player4IsActive,
            isDialer = dealerIndex == 4,
            lastBet = ActionType.unserialize(player4LastBet)
        )
        val player5 = PlayerData(
            name = player5Name,
            cards = Card.unserializeList(player5Cards),
            chips = player5Chips,
            isActive = player5IsActive,
            isDialer = dealerIndex == 5,
            lastBet = ActionType.unserialize(player5LastBet)
        )
        ScreenState(
            players = listOf(player0, player1, player2, player3, player4, player5),
            communityCards = Card.unserializeList(communityCards),
            actionsAvailable = emptyList(),
            bankChips = bankChips,
            isActionAvailable = true,
            isDealAvailable = true,
            isResetAvailable = isResetAvailable,
            isCardsOpen = false
        )
    }

    return SavedState(
        screenState = screenState,
        currentBet = localData.currentBet,
        numOfRaise = localData.numOfRaise,
        playerIndex = localData.playerIndex,
        round = RoundType.entries[localData.round],
        deck = Card.unserializeList(localData.deck)
    )
}