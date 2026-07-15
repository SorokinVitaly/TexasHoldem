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
    private val history: History,
    private val chenAnalyzer: ChenAnalyzer
) : ViewModel() {
    private val savedState = loadSavedState()

    private val _state = MutableStateFlow(savedState.screenState)
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    private val statistics = savedState.statistics
    private val deck = savedState.deck.toMutableList()
    private var currentBet = savedState.currentBet
    private var numOfRaise = savedState.numOfRaise
    private var numOfCall = savedState.numOfCall
    private var playerIndex = savedState.playerIndex
    private var round = savedState.round
    private val tablePositions = arrayOfNulls<TablePosition?>(PLAYERS_NUMBER)
    private val preCalculatedData = arrayOfNulls<PreCalculatedData?>(PLAYERS_NUMBER)

    init {
        if (localData.isGameStarted &&
            state.value.players.all { it.cards.size == 2 || !it.isActive }
        ) {
            viewModelScope.launch {
                _state.update { it.copy(isActionAvailable = false) }
                calculatePositions()
                if (round != RoundType.PRE_FLOP) {
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

    fun onDealNext() {
        viewModelScope.launch {
            localData.isResetAvailable = true
            localData.isGameStarted = true
            currentBet = 0
            numOfRaise = 0
            numOfCall = 0
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
            calculatePositions()
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
                        isDealer = i == localData.dealerIndex
                    )
                }
            )
        }
    }

    private suspend fun mainGameLoop() {
        while (true) {
            val inGamePlayers = state.value.players.filter { it.isInGame }
            if (inGamePlayers.size == 1) {
                takeBank(listOf(state.value.players.indexOfFirst { it.isInGame }))
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
        logAndShow("$winnersNames won and take bank ${state.value.bankChips} chips")

        fun take(index: Int, amount: Int) {
            _state.update { it.takeFromBank(index, amount) }
        }

        val numWinners = winIndexes.size
        val part = state.value.bankChips / numWinners
        if (part > 0) {
            winIndexes.forEach { index ->
                take(index, part)
            }
        }

        val winIndexesFirst = winIndexes.filter { it > localData.dealerIndex }
        winIndexesFirst.forEach { index ->
            if (state.value.bankChips > 0) {
                take(index, 1)
            }
        }

        val winIndexesLast = winIndexes.filter { it <= localData.dealerIndex }
        winIndexesLast.forEach { index ->
            if (state.value.bankChips > 0) {
                take(index, 1)
            }
        }
    }

    private fun gameOver() {
        val isPlayerActive = state.value.players.map { it.chips >= BIG_BLIND }
        val isDealAvailable = isPlayerActive[0] && isPlayerActive.count { it } > 1
        _state.update {
            it.copy(
                actionsAvailable = emptyList(),
                isActionAvailable = true,
                isDealAvailable = isDealAvailable,
                isResetAvailable = true,
                players = it.players.mapIndexed { i, player ->
                    player.copy(isActive = isPlayerActive[i])
                }
            )
        }
        localData.dealerIndex = nextPlayerIndex(localData.dealerIndex) { isActive }
        localData.isGameStarted = false
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
        numOfCall = 0
        currentBet = 0
        history.startRound()
        clearBets()
        return false
    }

    private suspend fun endRiverRound() {
        _state.update { it.copy(isCardsOpen = true) }
        val inGameCombinations = state.value.players.mapIndexedNotNull { i, playerData ->
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
        repeat(PLAYERS_NUMBER) { index ->
            player(index).apply {
                if (isActive) {
                    action(index)
                }
            }
        }
    }

    private fun calculatePositions() {
        // Positions are calculated only for 6 players table
        val availablePositions = TablePosition.entries.toMutableList()
        val numPlayers = state.value.players.count { it.isActive }
        if (numPlayers < 6) {
            availablePositions.remove(TablePosition.HJ)
        }
        if (numPlayers < 5) {
            availablePositions.remove(TablePosition.UTG)
        }
        if (numPlayers < 4) {
            availablePositions.remove(TablePosition.CO)
        }
        val iterator = availablePositions.iterator()
        var index = localData.dealerIndex
        while (iterator.hasNext()) {
            tablePositions[index] = iterator.next()
            index = nextPlayerIndex(index) { isActive }
        }
    }

    private suspend fun preCalculateData() = coroutineScope {
        val community = state.value.communityCards
        val opponentsCount = state.value.players.count { it.isInGame } - 1
        state.value.players.mapIndexedNotNull { i, playerData ->
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
        val first = (index + 1) % PLAYERS_NUMBER
        var current = first

        while (true) {
            if (player(current).predicate()) {
                return current
            }
            current = (current + 1) % PLAYERS_NUMBER
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
        if (action is ActionType.Call) {
            numOfCall++
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
        val player = player(index)
        val position = tablePositions[index]
        requireNotNull(position)

        if (round == RoundType.PRE_FLOP) {
            val handStrength = chenAnalyzer.calcHandStrength(player.cards)
            val lastBet = player.lastBet
            val isPaid = lastBet.paid > 0 &&
                    lastBet !is ActionType.SmallBlind &&
                    lastBet !is ActionType.BigBlind
            val strategy = selectPreFlopStrategy(handStrength, position, numOfRaise, numOfCall, isPaid)
            val action = resolveAction(strategy, availableActions)
            statistics.registerEvent(
                handStrength,
                position,
                numOfRaise,
                numOfCall,
                isPaid,
                availableActions,
                action,
                strategy
            )
            return action
        } else {
            val data = preCalculatedData[index]
            requireNotNull(data)
            val prevPaid = player.lastBet.paid
            val isFacingBet = currentBet > prevPaid
            val strategy = selectPostFlopStrategy(data.equity, isFacingBet, numOfRaise, position)
            return resolveAction(strategy, availableActions)
        }
    }

    private fun player(index: Int) = state.value.players[index]

    private fun saveState() {
        val savedState = SavedState(
            state.value,
            currentBet,
            numOfRaise,
            numOfCall,
            playerIndex,
            round,
            deck,
            statistics
        )
        saveSnapshot(localData, history, savedState)
    }

    private fun loadSavedState(): SavedState =
        try {
            restoreSnapshot(localData, history, chenAnalyzer.values)
        } catch(_: Exception) {
            log("Local data is broken. Game was restarted")
            localData.resetGame()
            restoreSnapshot(localData, history, chenAnalyzer.values)
        }

    private fun log(mess: String) = Log.e("GamePlay", mess)

    private suspend fun logAndShow(mess: String) {
        log(mess)
        _events.emit(UiEvent.ShowToast(mess))
    }
}