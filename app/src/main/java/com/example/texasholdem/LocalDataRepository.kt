package com.example.texasholdem

import android.content.SharedPreferences
import javax.inject.Inject
import kotlin.random.Random


interface LocalDataRepository {
    var player0Name: String
    var player0Cards: String
    var player0Chips: Int
    var player0IsActive: Boolean
    var player0LastBet: String

    var player1Name: String
    var player1Cards: String
    var player1Chips: Int
    var player1IsActive: Boolean
    var player1LastBet: String

    var player2Name: String
    var player2Cards: String
    var player2Chips: Int
    var player2IsActive: Boolean
    var player2LastBet: String

    var player3Name: String
    var player3Cards: String
    var player3Chips: Int
    var player3IsActive: Boolean
    var player3LastBet: String

    var player4Name: String
    var player4Cards: String
    var player4Chips: Int
    var player4IsActive: Boolean
    var player4LastBet: String

    var player5Name: String
    var player5Cards: String
    var player5Chips: Int
    var player5IsActive: Boolean
    var player5LastBet: String

    var history: String
    var deck: String
    var communityCards: String
    var bankChips: Int
    var dealerIndex: Int
    var isGameStarted: Boolean
    var isResetAvailable: Boolean
    var currentBet: Int
    var numOfRaise: Int
    var playerIndex: Int
    var round: Int

    fun resetGame()
}

class LocalDataRepositoryImpl @Inject constructor(override val prefs: SharedPreferences) :
    LocalDataRepository,
    PrefsOwner
{
    override var player0Name: String by PreferencesDelegate(
        ::player0Name.name,
        DEFAULT_PLAYER_0_NAME
    )
    override var player0Cards: String by PreferencesDelegate(
        ::player0Cards.name,
        ""
    )
    override var player0Chips: Int by PreferencesDelegate(
        ::player0Chips.name,
        DEFAULT_CHIP_NUMBER
    )
    override var player0IsActive: Boolean by PreferencesDelegate(
        ::player0IsActive.name,
        true
    )
    override var player0LastBet: String by PreferencesDelegate(
        ::player0LastBet.name,
        ""
    )

    override var player1Name: String by PreferencesDelegate(
        ::player1Name.name,
        DEFAULT_PLAYER_1_NAME
    )
    override var player1Cards: String by PreferencesDelegate(
        ::player1Cards.name,
        ""
    )
    override var player1Chips: Int by PreferencesDelegate(
        ::player1Chips.name,
        DEFAULT_CHIP_NUMBER
    )
    override var player1IsActive: Boolean by PreferencesDelegate(
        ::player1IsActive.name,
        true
    )
    override var player1LastBet: String by PreferencesDelegate(
        ::player1LastBet.name,
        ""
    )

    override var player2Name: String by PreferencesDelegate(
        ::player2Name.name,
        DEFAULT_PLAYER_2_NAME
    )
    override var player2Cards: String by PreferencesDelegate(
        ::player2Cards.name,
        ""
    )
    override var player2Chips: Int by PreferencesDelegate(
        ::player2Chips.name,
        DEFAULT_CHIP_NUMBER
    )
    override var player2IsActive: Boolean by PreferencesDelegate(
        ::player2IsActive.name,
        true
    )
    override var player2LastBet: String by PreferencesDelegate(
        ::player2LastBet.name,
        ""
    )

    override var player3Name: String by PreferencesDelegate(
        ::player3Name.name,
        DEFAULT_PLAYER_3_NAME
    )
    override var player3Cards: String by PreferencesDelegate(
        ::player3Cards.name,
        ""
    )
    override var player3Chips: Int by PreferencesDelegate(
        ::player3Chips.name,
        DEFAULT_CHIP_NUMBER
    )
    override var player3IsActive: Boolean by PreferencesDelegate(
        ::player3IsActive.name,
        true
    )
    override var player3LastBet: String by PreferencesDelegate(
        ::player3LastBet.name,
        ""
    )

    override var player4Name: String by PreferencesDelegate(
        ::player4Name.name,
        DEFAULT_PLAYER_4_NAME
    )
    override var player4Cards: String by PreferencesDelegate(
        ::player4Cards.name,
        ""
    )
    override var player4Chips: Int by PreferencesDelegate(
        ::player4Chips.name,
        DEFAULT_CHIP_NUMBER
    )
    override var player4IsActive: Boolean by PreferencesDelegate(
        ::player4IsActive.name,
        true
    )
    override var player4LastBet: String by PreferencesDelegate(
        ::player4LastBet.name,
        ""
    )

    override var player5Name: String by PreferencesDelegate(
        ::player5Name.name,
        DEFAULT_PLAYER_5_NAME
    )
    override var player5Cards: String by PreferencesDelegate(
        ::player5Cards.name,
        ""
    )
    override var player5Chips: Int by PreferencesDelegate(
        ::player5Chips.name,
        DEFAULT_CHIP_NUMBER
    )
    override var player5IsActive: Boolean by PreferencesDelegate(
        ::player5IsActive.name,
        true
    )
    override var player5LastBet: String by PreferencesDelegate(
        ::player5LastBet.name,
        ""
    )

    override var history: String by PreferencesDelegate(
        ::history.name,
        ""
    )
    override var deck: String by PreferencesDelegate(
        ::deck.name,
        ""
    )
    override var communityCards: String by PreferencesDelegate(
        ::communityCards.name,
        ""
    )
    override var bankChips: Int by PreferencesDelegate(
        ::bankChips.name,
        0
    )
    override var dealerIndex: Int by PreferencesDelegate(
        ::dealerIndex.name,
        Random.nextInt(6)
    )
    override var isGameStarted: Boolean by PreferencesDelegate(
        ::isGameStarted.name,
        false
    )
    override var isResetAvailable: Boolean by PreferencesDelegate(
        ::isResetAvailable.name,
        true
    )
    override var currentBet: Int by PreferencesDelegate(
        ::currentBet.name,
        0
    )
    override var numOfRaise: Int by PreferencesDelegate(
        ::numOfRaise.name,
        0
    )
    override var playerIndex: Int by PreferencesDelegate(
        ::playerIndex.name,
        0
    )
    override var round: Int by PreferencesDelegate(
        ::round.name,
        0
    )

    override fun resetGame() {
        player0Cards = ""
        player1Cards = ""
        player2Cards = ""
        player3Cards = ""
        player4Cards = ""
        player5Cards = ""

        player0Chips = DEFAULT_CHIP_NUMBER
        player1Chips = DEFAULT_CHIP_NUMBER
        player2Chips = DEFAULT_CHIP_NUMBER
        player3Chips = DEFAULT_CHIP_NUMBER
        player4Chips = DEFAULT_CHIP_NUMBER
        player5Chips = DEFAULT_CHIP_NUMBER

        player0IsActive = true
        player1IsActive = true
        player2IsActive = true
        player3IsActive = true
        player4IsActive = true
        player5IsActive = true

        player0LastBet = ""
        player1LastBet = ""
        player2LastBet = ""
        player3LastBet = ""
        player4LastBet = ""
        player5LastBet = ""

        history = ""
        deck = ""
        communityCards= ""
        bankChips = 0
        dealerIndex = Random.nextInt(6)
        isGameStarted = false
        isResetAvailable = false
        currentBet = 0
        numOfRaise = 0
        playerIndex = 0
        round = 0
    }

    companion object {
        const val DEFAULT_PLAYER_0_NAME = "Me"
        const val DEFAULT_PLAYER_1_NAME = "Lesley Colon"
        const val DEFAULT_PLAYER_2_NAME = "Leon Kim"
        const val DEFAULT_PLAYER_3_NAME = "Vanessa May"
        const val DEFAULT_PLAYER_4_NAME = "Omer Griffin"
        const val DEFAULT_PLAYER_5_NAME = "Yolanda Young"
        const val DEFAULT_CHIP_NUMBER = 1000
    }
}