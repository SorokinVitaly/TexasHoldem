package com.example.texasholdem

import javax.inject.Inject


interface History {
    val list: List<Pair<Int, ActionType>>
    fun clear()
    fun add(index: Int, action :ActionType)
    fun isAggressiveTable(): Boolean
}

class HistoryImpl @Inject constructor() : History  {
    override val list = mutableListOf<Pair<Int, ActionType>>()

    override fun clear() = list.clear()

    override fun add(index: Int, action :ActionType) { list.add(index to action) }

    override fun isAggressiveTable(): Boolean = list.count { it.second is ActionType.Raise } > 1
}