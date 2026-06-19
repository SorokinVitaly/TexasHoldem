package com.example.texasholdem

import androidx.compose.runtime.Immutable


@Immutable
data class ScreenState(
    val players: List<PlayerData>,
    val communityCards: List<Card> = emptyList(),
    val actionsAvailable: List<ActionType>,
    val bankChips: Int,
    val isActionAvailable: Boolean,
    val isDealAvailable: Boolean,
    val isResetAvailable: Boolean,
    val isCardsOpen: Boolean
) {
    init {
        if (players.size != 6) {
            throw IllegalStateException("Wrong number of players")
        }
    }

    fun updateAllPlayers(update: PlayerData.() -> PlayerData) =
        copy(players = players.map { it.update() })

    fun updatePlayer(index: Int, update: PlayerData.() -> PlayerData) =
        copy(players = updateOnePlayer(index, update))

    fun payToBank(index: Int, amount: Int) =
        copy(
            players = updateOnePlayer(index) { payChips(amount) },
            bankChips = bankChips + amount
        )

    fun takeFromBank(index: Int, amount: Int) = payToBank(index, -amount)

    private fun updateOnePlayer(index: Int, update: PlayerData.() -> PlayerData) =
        players.mapIndexed { i, player ->
            if (index == i) player.update() else player
        }

    companion object {
        fun isDealAvailable(players: List<PlayerData>): Boolean =
            players.first().isActive && players.count { it.isActive } > 1
    }
}