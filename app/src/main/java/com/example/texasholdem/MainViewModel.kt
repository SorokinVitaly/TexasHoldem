package com.example.texasholdem

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val localData: LocalDataRepository,
    private val history: History
) : ViewModel() {
    private val savedState = loadSavedState()

    private val _state = MutableStateFlow(savedState.screenState)
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    private val deck = savedState.deck.toMutableList()
    private var currentBet = savedState.currentBet
    private var numOfRaise = savedState.numOfRaise
    private var playerIndex = savedState.playerIndex
    private var round = savedState.round
    private val preFlopStrength = arrayOfNulls<HandStrength?>(6)
    private val preCalculatedData = arrayOfNulls<PreCalculatedData?>(6)

    init {
        if (localData.isGameStarted &&
            state.value.players.all { it.cards.size == 2 || !it.isActive }
        ) {
            viewModelScope.launch {
                _state.update { it.copy(isActionAvailable = false) }
                if (round == RoundType.PRE_FLOP) {
                    preCalculatePreFlop()
                } else {
                    preCalculateData()
                }
                mainGameLoop()
            }
        }
    }

    fun onResetGame() {
        viewModelScope.launch {
            localData.resetGame()
            _state.update { loadSavedState().screenState }
            delay(500L)
            logAndShow("Game was restarted")
        }
    }

    fun onDialNext() {
        viewModelScope.launch {
            localData.isResetAvailable = true
            localData.isGameStarted = true
            currentBet = 0
            numOfRaise = 0
            playerIndex = localData.dealerIndex
            round = RoundType.PRE_FLOP
            history.clear()
            history.startRound()
            if (deck.size != 52) {
                deck.clear()
                deck.addAll(deckPoker)
                deck.shuffle()
            }
            initialState()
            saveState()
            dealingCards()
            preCalculatePreFlop()
            payBlinds()
            mainGameLoop()
        }
    }

    fun onAction(action: ActionType) {
        viewModelScope.launch {
            _state.update { it.copy(isActionAvailable = false) }
            applyAction(0, action)
            saveState()
            mainGameLoop()
        }
    }

    private fun initialState() {
        _state.update {
            it.copy(
                communityCards = emptyList(),
                actionsAvailable = emptyList(),
                bankChips = 0,
                isActionAvailable = false,
                isDealAvailable = true,
                isResetAvailable = true,
                isCardsOpen = false,
                players = it.players.mapIndexed { i, player ->
                    player.copy(
                        cards = emptyList(),
                        lastBet = ActionType.NoAction(),
                        isDialer = i == localData.dealerIndex
                    )
                }
            )
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

            val endRoundDetected = inGamePlayers.all {
                it.lastBet.paid == currentBet &&
                        it.lastBet !is ActionType.NoAction &&
                        it.lastBet !is ActionType.SmallBlind &&
                        it.lastBet !is ActionType.BigBlind
            }
            if (endRoundDetected) {
                if (endRound()) {
                    return
                }
            }

            playerIndex = nextPlayerIndex(playerIndex) { isInGame }
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
        logAndShow("$winnersNames won and take bank ${_state.value.bankChips} chips")

        fun take(index: Int, amount: Int) {
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
        localData.dealerIndex = nextPlayerIndex(localData.dealerIndex) { isActive }
        localData.isGameStarted = false
        _state.update { it.copy(
            actionsAvailable = emptyList(),
            isActionAvailable = true,
            isDealAvailable = true,
            isResetAvailable = true
        ) }
        saveState()
    }

    private suspend fun endRound(): Boolean {
        val newRound = when (round) {
            RoundType.PRE_FLOP -> RoundType.FLOP
            RoundType.FLOP -> RoundType.TURN
            else -> RoundType.RIVER
        }
        if (round != RoundType.RIVER) {
            logAndShow("Start ${newRound.name} round!")
        } else {
            endRiverRound()
            return true
        }
        dealingCommunity(if (round == RoundType.PRE_FLOP) 3 else 1)
        preCalculateData()
        round = newRound
        playerIndex = localData.dealerIndex
        numOfRaise = 0
        currentBet = 0
        history.startRound()
        clearBets()
        return false
    }

    private suspend fun endRiverRound() {
        _state.update { it.copy(isCardsOpen = true) }
        val inGameCombinations = _state.value.players.mapIndexedNotNull { i, playerData ->
            if (playerData.isInGame) {
                val data = preCalculatedData[i]
                requireNotNull(data)
                i to data.combination
            } else {
                null
            }
        }
        val winCombination = inGameCombinations.maxBy { it.second }.second

        log("winCombination: $winCombination")
        log("other combinations:")
        inGameCombinations.filter { it.second < winCombination }.forEach {
            log("${it.first}: ${it.second}")
        }

        val winIndexes = inGameCombinations.filter {
            it.second.compareTo(winCombination) == 0
        }.map { it.first }
        takeBank(winIndexes)
        gameOver()
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

    private suspend fun forEachActivePlayer(action: suspend PlayerData.(Int) -> Unit) {
        repeat(6) { index ->
            player(index).apply {
                if (isActive) {
                    action(index)
                }
            }
        }
    }

    private suspend fun preCalculatePreFlop() =
        forEachActivePlayer { index ->
            if (index > 0) {
                preFlopStrength[index] = preFlopStrength(cards)
            }
        }


    private suspend fun preCalculateData() = coroutineScope {
        val community = _state.value.communityCards
        val opponentsCount = _state.value.players.count { it.isInGame } - 1
        _state.value.players.mapIndexedNotNull { i, playerData ->
            if (playerData.isInGame) {
                launch {
                    val pocket = playerData.cards
                    val combination = calcCombination(community, pocket)
                    preCalculatedData[i] = if (i == 0) {
                        PreCalculatedData(combination)
                    } else {
                        PreCalculatedData(
                            combination = combination,
                            incompleteCombination = calcIncompleteCombination(
                                community,
                                pocket,
                                combination
                            ),
                            opponentsCount = opponentsCount,
                            equity = calcEquity(pocket, community, opponentsCount)
                        )
                    }
                }
            } else {
                null
            }
        }
    }

    private suspend fun dealingCards() {
        _state.update { it.updateAllPlayers { clearCards() } }
        repeat(2) {
            forEachActivePlayer { index ->
                delay(300L)
                val card = deck.removeAt(deck.lastIndex)
                _state.update { it.updatePlayer(index) { addCard(card) } }
            }
        }
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
        playerIndex = nextPlayerIndex(playerIndex) { isActive }
        applyAction(playerIndex, ActionType.SmallBlind())
        delay(300L)
        playerIndex = nextPlayerIndex(playerIndex) { isActive }
        applyAction(playerIndex, ActionType.BigBlind())
    }

    private fun nextPlayerIndex(index: Int, predicate: PlayerData.() -> Boolean): Int {
        val first = (index + 1) % 6
        var current = first

        while (true) {
            if (player(current).predicate()) {
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
        val (strength, potentialBluff) = if (round == RoundType.PRE_FLOP) {
            val preFlopStrength = preFlopStrength[index]
            requireNotNull(preFlopStrength)
            preFlopStrength to false
        } else {
            val data = preCalculatedData[index]
            requireNotNull(data)
            val bankChips = _state.value.bankChips
            val prevPaid = player(index).lastBet.paid
            val payToCall = currentBet - prevPaid
            val potOdds = if (bankChips + payToCall == 0) 0f
            else payToCall.toFloat() / (bankChips + payToCall)
            val hasStrongDraw = data.incompleteCombination.type >= IncompleteCombinationType.FOUR_TO_STRAIGHT_OPEN
            equityToStrength(data.equity, potOdds) to hasStrongDraw
        }

        val playerCount = _state.value.players.count { it.isActive }
        val positionFromDealer = (index - localData.dealerIndex - 1 + playerCount) % playerCount
        val tableFactor = TableFactor(
            numOfRaise = numOfRaise,
            isLatePosition = positionFromDealer >= 2,
            isFacingBet = currentBet > 0,
            semiBluffPotential = potentialBluff,
            tableIsAggressive = history.isAggressiveTable(),
            tableIsPassive = history.isPassiveRound()
        )
        val strategy = resolveStrategy(strength, tableFactor)
        return resolveAction(strategy, availableActions)
    }

    private fun player(index: Int) = _state.value.players[index]

    private fun saveState() {
        val savedState = SavedState(
            state.value,
            currentBet,
            numOfRaise,
            playerIndex,
            round,
            deck
        )
        saveSnapshot(localData, history, savedState)
    }

    private fun loadSavedState(): SavedState =
        try {
            restoreSnapshot(localData, history)
        } catch(_: Exception) {
            log("Local data is broken. Game was restarted")
            localData.resetGame()
            restoreSnapshot(localData, history)
        }

    private fun log(mess: String) = Log.e("GamePlay", mess)

    private suspend fun logAndShow(mess: String) {
        log(mess)
        _events.emit(UiEvent.ShowToast(mess))
    }
}