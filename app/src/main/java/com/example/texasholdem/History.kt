package com.example.texasholdem

import javax.inject.Inject


interface History {
    val list: List<Pair<Int, ActionType>>
    fun clear()
    fun add(index: Int, action :ActionType)
    fun startRound()
    fun isAggressiveTable(): Boolean
    fun isPassiveRound(): Boolean
}

class HistoryImpl @Inject constructor() : History  {
    private val startRound = -1 to ActionType.NoAction()

    override val list = mutableListOf<Pair<Int, ActionType>>()

    override fun clear() = list.clear()

    override fun add(index: Int, action: ActionType) { list.add(index to action) }

    override fun startRound() { list.add(startRound) }

    override fun isAggressiveTable(): Boolean =
        list.count { it.second is ActionType.Raise || it.second is ActionType.Bet } > 1

    override fun isPassiveRound(): Boolean {
        val index = list.lastIndexOf(startRound).takeIf { it >= 0 }
            ?: throw IllegalStateException("startRound not found")
        val roundHistory = list.drop(index)
        val hasAggression = roundHistory.any {
            it.second is ActionType.Raise || it.second is ActionType.Bet
        }
        val hasPassiveAction = roundHistory.any {
            it.second is ActionType.Call || it.second is ActionType.Check
        }
        return !hasAggression && hasPassiveAction
    }
}