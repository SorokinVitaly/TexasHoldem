package com.example.texasholdem

import android.content.SharedPreferences
import javax.inject.Inject
import kotlin.random.Random


interface LocalDataRepository {
    var player0Name: String
    var player1Name: String
    var player2Name: String
    var player3Name: String

    var player0Chips: Int
    var player1Chips: Int
    var player2Chips: Int
    var player3Chips: Int

    var isGameStarted: Boolean
    var isJustReset: Boolean
    var dealerIndex: Int

    fun resetGame()
    fun savedState(): ScreenState
    fun saveState(state: ScreenState)
}

class LocalDataRepositoryImpl @Inject constructor(override val prefs: SharedPreferences) :
    LocalDataRepository,
    PrefsOwner
{
    override var player0Name: String by PreferencesDelegate(
        ::player0Name.name,
        DEFAULT_PLAYER_0_NAME
    )
    override var player1Name: String by PreferencesDelegate(
        ::player1Name.name,
        DEFAULT_PLAYER_1_NAME
    )
    override var player2Name: String by PreferencesDelegate(
        ::player2Name.name,
        DEFAULT_PLAYER_2_NAME
    )
    override var player3Name: String by PreferencesDelegate(
        ::player3Name.name,
        DEFAULT_PLAYER_3_NAME
    )
    override var player0Chips: Int by PreferencesDelegate(
        ::player0Chips.name,
        DEFAULT_CHIP_NUMBER
    )
    override var player1Chips: Int by PreferencesDelegate(
        ::player1Chips.name,
        DEFAULT_CHIP_NUMBER
    )
    override var player2Chips: Int by PreferencesDelegate(
        ::player2Chips.name,
        DEFAULT_CHIP_NUMBER
    )
    override var player3Chips: Int by PreferencesDelegate(
        ::player3Chips.name,
        DEFAULT_CHIP_NUMBER
    )
    override var isGameStarted: Boolean by PreferencesDelegate(
        ::isGameStarted.name,
        false
    )
    override var isJustReset: Boolean by PreferencesDelegate(
        ::isJustReset.name,
        false
    )
    override var dealerIndex: Int by PreferencesDelegate(
        ::dealerIndex.name,
        Random.nextInt(4)
    )

    override fun resetGame() {
        player0Chips = DEFAULT_CHIP_NUMBER
        player1Chips = DEFAULT_CHIP_NUMBER
        player2Chips = DEFAULT_CHIP_NUMBER
        player3Chips = DEFAULT_CHIP_NUMBER
        isGameStarted = false
        isJustReset = true
        dealerIndex = Random.nextInt(4)
    }

    override fun savedState(): ScreenState {
        val player0 = PlayerData(
            name = player0Name,
            chips = player0Chips,
            isActive = player0Chips > 0
        )
        val player1 = PlayerData(
            name = player1Name,
            chips = player1Chips,
            isActive = player1Chips > 0
        )
        val player2 = PlayerData(
            name = player2Name,
            chips = player2Chips,
            isActive = player2Chips > 0
        )
        val player3 = PlayerData(
            name = player3Name,
            chips = player3Chips,
            isActive = player3Chips > 0
        )
        val players = listOf(player0, player1, player2, player3)

        return ScreenState(
            players = players,
            actionsAvailable = emptyList(),
            bankChips = 0,
            isDrawEnabled = false,
            isActionAvailable = true,
            isDealAvailable = ScreenState.isDealAvailable(players),
            isResetAvailable = !isJustReset,
            isCardsOpen = false
        )
    }

    override fun saveState(state: ScreenState) {
        player0Name = state.players[0].name
        player1Name = state.players[1].name
        player2Name = state.players[2].name
        player3Name = state.players[3].name

        player0Chips = state.players[0].chips
        player1Chips = state.players[1].chips
        player2Chips = state.players[2].chips
        player3Chips = state.players[3].chips

        isGameStarted = false
        isJustReset = false
    }

    companion object {
        const val DEFAULT_PLAYER_0_NAME = "Me"
        const val DEFAULT_PLAYER_1_NAME = "Lesley Colon"
        const val DEFAULT_PLAYER_2_NAME = "Leon Kim"
        const val DEFAULT_PLAYER_3_NAME = "Vanessa May"
        const val DEFAULT_CHIP_NUMBER = 1000
    }
}