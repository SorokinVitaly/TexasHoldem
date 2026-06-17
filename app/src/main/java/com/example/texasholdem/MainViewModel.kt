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
    private val combinations = arrayOfNulls<DrawCombination?>(4)

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
            newDeck()
            dealingCards()
            payBlinds()
            currentBet = 0
            numOfRaise = 0
            playerIndex = dealerIndex
            round = RoundType.PRE_FLOP
            history.clear()
            //mainGameLoop()
        }
    }


    private fun newDeck() {
        deck.clear()
        deck.addAll(deckPoker)
        deck.shuffle()
    }

    private suspend fun dealingCards() {
        _state.update { it.updateAllPlayers { clearCards() } }
        repeat(2) {
            repeat(4) { index ->
                if (player(index).isActive) {
                    delay(300L)
                    val card = deck.removeAt(deck.lastIndex)
                    _state.update { it.updatePlayer(index) { addCard(card) } }
                }
            }
        }
        delay(500L)
        _state.update { it.updateAllPlayers { sortCards() } }

        /*repeat(4) { index ->
            if (player(index).isActive) {
                combinations[index] = calcPreDrawCombination(history, player(index).cards)
            }
        }*/
    }

    private suspend fun payBlinds() {
        val dealerIndex = nextInGameIndex(localData.dealerIndex)
        playerIndex = dealerIndex


        repeat(4) { index ->
            if (player(index).isActive) {
                delay(300L)
                _state.update { it.payToBank(index, SMALL_BLIND) }
            }
        }
    }

    private fun nextInGameIndex(index: Int): Int {
        val first = (index + 1) and 3
        var current = first

        while (true) {
            if (player(current).isInGame) {
                return current
            }
            current = (current + 1) and 3
            if (current == first) {
                throw IllegalStateException("Next player not found")
            }
        }
    }

    private fun player(index: Int) = _state.value.players[index]

    /*
    fun onAction(action: ActionType) {
        viewModelScope.launch {
            _state.update { it.copy(isActionAvailable = false, isDrawEnabled = false) }
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

            val endRound = if (round != RoundType.DRAW) {
                inGamePlayers.all { it.lastBet.bet == currentBet }
            } else {
                inGamePlayers.all { it.lastDraw is ActionType.Draw }
            }
            if (endRound) {
                if (endRound()) {
                    return
                }
            }

            playerIndex = nextInGameIndex(playerIndex)
            val availableActions = availableActions(playerIndex)
            if (playerIndex == 0) {
                if (round == RoundType.DRAW) {
                    _state.update { it.copy(isDrawEnabled = true) }
                    _events.emit(UiEvent.ShowToast("Please choose cards to draw"))
                }
                _state.update { it.copy(isActionAvailable = true, actionsAvailable = availableActions) }
                return
            } else {
                val action = botBetting(playerIndex, availableActions)
                applyAction(playerIndex, action)
            }
        }
    }

    private suspend fun endRound(): Boolean {
        val newRound = when (round) {
            RoundType.PRE_DRAW -> {
                RoundType.DRAW
            }
            RoundType.DRAW -> {
                endDrawRound()
                RoundType.POST_DRAW
            }
            RoundType.POST_DRAW -> {
                endPostDrawRound()
                return true
            }
        }
        playerIndex = localData.dealerIndex
        round = newRound
        return false
    }

    private fun endDrawRound() {
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
        repeat(4) { index ->
            if (player(index).isInGame) {
                combinations[index] = DrawCombination(
                    onHandCombination = calcCombination(player(index).cards)
                )
            }
        }
    }

    private suspend fun endPostDrawRound() {
        _state.update { it.copy(isCardsOpen = true) }
        val inGameCombinations = _state.value.players.mapIndexedNotNull { i, playerData ->
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
        val winIndexes = inGameCombinations.filter { it.second == winCombination }.map { it.first }
        takeBank(winIndexes)
        gameOver()
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
            actionsAvailable = emptyList(),
            isDrawEnabled = false,
            isActionAvailable = true,
            isDealAvailable = true,
            isResetAvailable = true
        ) }
    }

    private fun availableActions(playerIndex: Int): List<ActionType> {
        if (round == RoundType.DRAW) {
            return listOf(ActionType.Draw())
        }
        val (betCount, raiseCount) = if (round == RoundType.PRE_DRAW) PRE_DRAW_BET to PRE_DRAW_RAISE else POST_DRAW_BET to POST_DRAW_RAISE
        val chips = player(playerIndex).chips
        val bets = ArrayList<ActionType>()
        if (currentBet == 0) {
            bets.add(ActionType.Check())
            if (chips >= betCount) {
                bets.add(ActionType.Bet(betCount))
            }
            bets.add(ActionType.Fold())
        } else {
            if (chips >= currentBet) {
                bets.add(ActionType.Call(currentBet))
            }
            if (chips >= currentBet + raiseCount && numOfRaise < MAX_NUM_OF_RAISE) {
                bets.add(ActionType.Raise(currentBet + raiseCount))
            }
            bets.add(ActionType.Fold())
        }
        require(bets.isNotEmpty())
        return bets
    }

    private fun botBetting(index: Int, availableActions: List<ActionType>): ActionType {
        val firstAction = availableActions.first()
        return if (firstAction is ActionType.Draw) {
            val combination = combinations[index]
            requireNotNull(combination)
            _state.update { it.updatePlayer(index) { setSelected(combination.cardsForDraw) } }
            firstAction
        } else {
            val isPreDraw = round == RoundType.PRE_DRAW
            val combination = combinations[index]
            requireNotNull(combination)
            val strength = calcHandStrength(combination, isPreDraw)
            val playerCount = _state.value.players.count { it.isActive }
            val positionFromDealer = (index - localData.dealerIndex - 1 + playerCount) % playerCount
            val isLatePosition = positionFromDealer >= 2
            val isFacingBet = currentBet > 0

            val potOdds: Float = currentBet.toFloat() / (_state.value.bankChips + currentBet).toFloat()
            val drawProbability = if (isPreDraw) drawOdds[combination.incompleteCombination] else 0f
            requireNotNull(drawProbability)
            val drawIsWorthIt = drawProbability > 0f && potOdds <= drawProbability

            val strategy = selectBotStrategy(
                strength,
                numOfRaise,
                isLatePosition,
                drawIsWorthIt,
                isFacingBet,
                isPreDraw
            )
            val action = resolveAction(strategy, availableActions)
            log("botBetting: $index -> ${player(index).cards} $strength $strategy ${action.name}")
            action
        }
    }

    private suspend fun applyAction(index: Int, action: ActionType) {
        log("$index: ${action.name}")
        if (action is ActionType.Raise) {
            numOfRaise++
        }
        if (action is ActionType.Draw) {
            val selected = _state.value.players[index].selectedCards
            val newAction = ActionType.Draw(selected.size)
            if (selected.isNotEmpty()) {
                selected.forEach { card ->
                    _state.update { it.updatePlayer(index) { removeCard(card) } }
                    delay(300L)
                }
                delay(500L)
                selected.forEach { card ->
                    val newCard = deck.removeAt(deck.lastIndex)
                    _state.update { it.updatePlayer(index) { addCard(newCard) } }
                    delay(300L)
                }
                _state.update { it.updatePlayer(index) { clearSelected().sortCards() } }
            }
            history.add(index, newAction)
            _state.update { it.updatePlayer(index) { copy(lastDraw = newAction) } }
        } else {
            if (action.bet > 0) {
                currentBet = action.bet
                _state.update { it.payToBank(index, action.bet) }
            }
            history.add(index, action)
            _state.update { it.updatePlayer(index) { copy(lastBet = action) } }
        }
    }

*/
    private fun log(mess: String) = Log.e("GamePlay", mess)

    companion object {
        const val SMALL_BLIND = 1
        const val BIG_BLIND = 2
        const val SMALL_BET = 2
        const val BIG_BET = 4
        const val MAX_NUM_OF_RAISE = 3

        val drawOdds = mapOf(
            IncompleteCombinationType.FOUR_TO_FLUSH to 0.19f,
            IncompleteCombinationType.FOUR_TO_STRAIGHT_OPEN to 0.17f,
            IncompleteCombinationType.FOUR_TO_STRAIGHT to 0.09f,
            IncompleteCombinationType.THREE_TO_STRAIGHT_FLUSH to 0.12f,
            IncompleteCombinationType.NO_INCOMPLETE to 0f
        )
    }
}