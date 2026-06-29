package com.example.texasholdem

import javax.inject.Inject


interface History {
    val list: List<Pair<Int, ActionType>>
    fun clear()
    fun startRound()
    fun add(index: Int, action :ActionType)
    fun isAggressiveTable(): Boolean
    fun isPassiveRound(): Boolean
    fun serialize(): String
    fun unserialize(saved: String)
}

class HistoryImpl @Inject constructor() : History  {
    override val list = mutableListOf<Pair<Int, ActionType>>()

    override fun clear() = list.clear()

    override fun startRound() { list.add(-1 to ActionType.NoAction()) }

    override fun add(index: Int, action: ActionType) { list.add(index to action) }

    override fun isAggressiveTable(): Boolean =
        list.count { it.second is ActionType.Raise || it.second is ActionType.Bet } > 1

    override fun isPassiveRound(): Boolean {
        val index = list.indexOfLast { it.first < 0 }.takeIf { it >= 0 }
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

    override fun serialize(): String =
        if (list.isEmpty()) {
            ""
        } else {
            list.joinToString(prefix = "[", postfix = "]") {
                "(${it.first};${it.second.serialize()})"
            }
        }

    override fun unserialize(saved: String) {
        list.clear()
        if (saved.isNotEmpty()) {
            list.addAll(
                saved.splitItems().map {
                    val itemList = it.splitItems(';')
                    require(itemList.size == 2)
                    itemList[0].toInt() to ActionType.unserialize(itemList[1])
                }
            )
        }
    }
}