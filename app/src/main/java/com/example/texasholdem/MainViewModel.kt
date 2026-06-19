package com.example.texasholdem

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.Int


@HiltViewModel
class MainViewModel @Inject constructor(
    private val localData: LocalDataRepository,
    private val history: History
) : ViewModel() {
    private val _state = MutableStateFlow(localData.savedState())
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    enum class RoundType {
        PRE_FLOP,
        FLOP,
        TURN,
        RIVER
    }

    private val deck = mutableListOf<Card>()
    private var currentBet = 0
    private var numOfRaise = 0
    private var playerIndex = 0
    private var round = RoundType.PRE_FLOP
    private val combinations = arrayOfNulls<DrawCombination?>(6)

    init {
        if (localData.isGameStarted) {
            onResetGame()
        }
    }

    fun onResetGame() {
        viewModelScope.launch {
            localData.resetGame()
            _state.update { localData.savedState() }
            val mess = "Game was restarted"
            log(mess)
            delay(500L)
            _events.emit(UiEvent.ShowToast(mess))
        }
    }

    fun onDialNext() {
        viewModelScope.launch {
            _state.update { localData.savedState() }
            localData.isJustReset = false
            localData.isGameStarted = true
            val dealerIndex = nextInGameIndex(localData.dealerIndex)
            localData.dealerIndex = dealerIndex
            _state.update { it.copy(isActionAvailable = false) }
            _state.update { it.updatePlayer(dealerIndex) { setDialer() } }
            currentBet = 0
            numOfRaise = 0
            playerIndex = dealerIndex
            round = RoundType.PRE_FLOP
            history.clear()
            newDeck()
            dealingCards()
            payBlinds()
            mainGameLoop()
        }
    }

    fun onAction(action: ActionType) {
        viewModelScope.launch {
            _state.update { it.copy(isActionAvailable = false) }
            applyAction(0, action)
            mainGameLoop()
        }
    }

    private suspend fun mainGameLoop() {
        while (true) {
            val inGamePlayers = _state.value.players.filter { it.isInGame }
            if (inGamePlayers.size == 1) {
                takeBank(listOf(_state.value.players.indexOfFirst { it.isInGame }))
                gameOver()
                return
            }

            val endRound = inGamePlayers.all {
                it.lastBet !is ActionType.NoAction && it.lastBet.paid == currentBet
            }
            if (endRound) {
                if (endRound()) {
                    return
                }
            }

            playerIndex = nextInGameIndex(playerIndex)
            val availableActions = availableActions(playerIndex)
            if (playerIndex == 0) {
                _state.update { it.copy(isActionAvailable = true, actionsAvailable = availableActions) }
                return
            } else {
                val action = botBetting(playerIndex, availableActions)
                applyAction(playerIndex, action)
            }
        }
    }

    private suspend fun takeBank(winIndexes: List<Int>) {
        require(winIndexes.isNotEmpty())
        val winnersNames = winIndexes.joinToString { player(it).name }
        val mess = "$winnersNames won and take bank ${_state.value.bankChips} chips"
        log(mess)
        _events.emit(UiEvent.ShowToast(mess))

        fun take(index: Int, amount: Int) {
            log("${_state.value.players[index].name} take $amount")
            _state.update { it.takeFromBank(index, amount) }
        }

        val numWinners = winIndexes.size
        val part = _state.value.bankChips / numWinners
        if (part > 0) {
            winIndexes.forEach { index ->
                take(index, part)
            }
        }

        val winIndexesFirst = winIndexes.filter { it > localData.dealerIndex }
        winIndexesFirst.forEach { index ->
            if (_state.value.bankChips > 0) {
                take(index, 1)
            }
        }

        val winIndexesLast = winIndexes.filter { it <= localData.dealerIndex }
        winIndexesLast.forEach { index ->
            if (_state.value.bankChips > 0) {
                take(index, 1)
            }
        }
    }

    private fun gameOver() {
        localData.saveState(_state.value)
        _state.update { it.copy(
            communityCards = emptyList(),
            actionsAvailable = emptyList(),
            isActionAvailable = true,
            isDealAvailable = true,
            isResetAvailable = true
        ) }
    }

    private suspend fun endRound(): Boolean {
        if (round != RoundType.RIVER) {
            val mess = "Start next round!"
            log(mess)
            _events.emit(UiEvent.ShowToast(mess))
            delay(2000L)
        }
        val newRound = when (round) {
            RoundType.PRE_FLOP -> {
                dealingCommunity(3)
                RoundType.FLOP
            }
            RoundType.FLOP -> {
                dealingCommunity(1)
                RoundType.TURN
            }
            RoundType.TURN -> {
                dealingCommunity(1)
                RoundType.RIVER
            }
            RoundType.RIVER -> {
                endRiverRound()
                return true
            }
        }
        playerIndex = localData.dealerIndex
        round = newRound
        numOfRaise = 0
        currentBet = 0
        clearBets()
        return false
    }

    private fun clearBets() {
        _state.update {
            it.copy(
                players = it.players.map { player ->
                    if (player.isInGame) {
                        player.copy(lastBet = ActionType.NoAction())
                    } else {
                        player
                    }
                }
            )
        }
    }

    private suspend fun endRiverRound() {
        _state.update { it.copy(isCardsOpen = true) }
        /*val inGameCombinations = _state.value.players.mapIndexedNotNull { i, playerData ->
            if (playerData.isInGame) {
                val combination = combinations[i]
                requireNotNull(combination)
                i to combination.onHandCombination
            }
            else {
                null
            }
        }
        val winCombination = inGameCombinations.maxBy { it.second }.second
        val winIndexes = inGameCombinations.filter { it.second == winCombination }.map { it.first }*/

        val winIndexes = listOf(0)
        takeBank(winIndexes)
        gameOver()
    }

    private fun newDeck() {
        deck.clear()
        deck.addAll(deckPoker)
        deck.shuffle()
    }

    private suspend fun dealingCards() {
        _state.update { it.updateAllPlayers { clearCards() } }
        repeat(2) {
            repeat(6) { index ->
                if (player(index).isActive) {
                    delay(300L)
                    val card = deck.removeAt(deck.lastIndex)
                    _state.update { it.updatePlayer(index) { addCard(card) } }
                }
            }
        }
        /*repeat(6) { index ->
            if (player(index).isActive) {
                combinations[index] = calcPreDrawCombination(history, player(index).cards)
            }
        }*/
    }

    private suspend fun dealingCommunity(numCards: Int) {
        repeat(numCards) {
            delay(300L)
            val card = deck.removeAt(deck.lastIndex)
            _state.update { it.copy(communityCards = it.communityCards + card) }
        }
    }

    private suspend fun payBlinds() {
        delay(300L)
        playerIndex = nextInGameIndex(playerIndex)
        applyAction(playerIndex, ActionType.SmallBlind())
        delay(300L)
        playerIndex = nextInGameIndex(playerIndex)
        applyAction(playerIndex, ActionType.BigBlind())
    }

    private fun nextInGameIndex(index: Int): Int {
        val first = (index + 1) % 6
        var current = first

        while (true) {
            if (player(current).isInGame) {
                return current
            }
            current = (current + 1) % 6
            if (current == first) {
                throw IllegalStateException("Next player not found")
            }
        }
    }

    private fun availableActions(playerIndex: Int): List<ActionType> {
        val betSize = if (round > RoundType.FLOP) BIG_BET else SMALL_BET
        val chips = player(playerIndex).chips
        val prevPaid = player(playerIndex).lastBet.paid
        val payToCall = currentBet - prevPaid
        val bets = ArrayList<ActionType>()

        if (payToCall == 0) {
            bets.add(ActionType.Check(currentBet))
        } else {
            if (chips >= payToCall) {
                bets.add(ActionType.Call(currentBet, prevPaid))
            }
        }

        if (currentBet == 0) {
            if (chips >= betSize) {
                bets.add(ActionType.Bet(betSize))
            }
        } else {
            val raiseTo = currentBet + betSize
            if (chips >= raiseTo - prevPaid && numOfRaise < MAX_NUM_OF_RAISE) {
                bets.add(ActionType.Raise(raiseTo, prevPaid))
            }
        }

        bets.add(ActionType.Fold())
        return bets
    }

    private fun applyAction(index: Int, action: ActionType) {
        log("$index: ${action.name}")
        if (action is ActionType.Raise) {
            numOfRaise++
        }
        if (action.paid > currentBet) {
            currentBet = action.paid
        }
        if (action.payNow > 0) {
            _state.update { it.payToBank(index, action.payNow) }
        }
        history.add(index, action)
        _state.update { it.updatePlayer(index) { copy(lastBet = action) } }
    }

    private fun botBetting(index: Int, availableActions: List<ActionType>): ActionType {
        return availableActions.random()
    }

    private fun player(index: Int) = _state.value.players[index]

    private fun log(mess: String) = Log.e("GamePlay", mess)
}