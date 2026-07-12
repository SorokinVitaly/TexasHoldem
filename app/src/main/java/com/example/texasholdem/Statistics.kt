package com.example.texasholdem

import kotlin.arrayOf

class Statistics {
    val tables = Array(7) { TableStatistics() }
    val positions = Array(6) { PositionStatistics() }
    val buckets = Array(20) { BucketStatistics() }

    fun registerEvent(
        handPercent: Float,
        position: TablePosition,
        numOfRaise: Int,
        numOfCall: Int,
        isPaid: Boolean,
        availableActions: List<ActionType>,
        action: ActionType,
        strategy: BettingStrategy,
    ) {
        val tableIndex = when {
            numOfRaise == 0 && numOfCall == 0 -> 0
            numOfRaise == 0 && numOfCall == 1 -> 1
            numOfRaise == 0 && numOfCall >= 2 -> 2
            numOfRaise == 1 && !isPaid -> 3
            numOfRaise == 1 && isPaid -> 4
            numOfRaise >= 2 && !isPaid -> 5
            numOfRaise >= 2 && isPaid -> 6
            else -> throw IllegalStateException("Wrong condition")
        }
        val bucketIndex = (handPercent * 20f).toInt()
        val positionIndex = position.ordinal

        val isCanRaise = availableActions.any { it is ActionType.Raise }
        val isRaised = action is ActionType.Raise
        val isCanPay = availableActions.any { it.payNow > 0 }
        val isPaid = action.payNow > 0

        tables[tableIndex].apply {
            buckets[bucketIndex]++
            positions[positionIndex]++
            when (strategy) {
                BettingStrategy.AGGRESSIVE -> strategyAggressive++
                BettingStrategy.PASSIVE -> strategyPassive++
                BettingStrategy.DROP -> strategyDrop++
            }
        }
        positions[positionIndex].apply {
            buckets[bucketIndex]++
            if (isCanRaise) canRaise++
            if (isRaised) raised++
            if (isCanPay) canPay++
            if (isPaid) paid++
        }
        buckets[bucketIndex].apply {
            when (strategy) {
                BettingStrategy.AGGRESSIVE -> strategyAggressive++
                BettingStrategy.PASSIVE -> strategyPassive++
                BettingStrategy.DROP -> strategyDrop++
            }
        }
    }

    override fun toString(): String {
        val allUsage = tables.sumOf { it.allUsage() }
        return buildString {
            append("\nTotal evaluations = $allUsage\n\n")

            append("Frequency of use of tables:\n")
            tables.forEachIndexed { i, table ->
                append("${tableName(i)} = ${freq(table.allUsage(), allUsage)}\n")
            }

            append("\nTables:")
            tables.forEachIndexed { i, table ->
                append("\n${tableName(i)}: ")
                append("$STRATEGY_AGGRESSIVE_NAME = ${freq(table.strategyAggressive, table.allUsage())}, ")
                append("$STRATEGY_PASSIVE_NAME = ${freq(table.strategyPassive, table.allUsage())}, ")
                append("$STRATEGY_DROP_NAME = ${freq(table.strategyDrop, table.allUsage())}\n")
                append("Buckets: ${ table.buckets.joinToString { freq(it, table.allUsage()) } }")
            }

            append("\n\nBuckets strategy:")
            buckets.forEachIndexed { i, bucket ->
                append("\n${bucketName(i)}: ")
                append("$STRATEGY_AGGRESSIVE_NAME = ${freq(bucket.strategyAggressive, bucket.allUsage())}, ")
                append("$STRATEGY_PASSIVE_NAME = ${freq(bucket.strategyPassive, bucket.allUsage())}, ")
                append("$STRATEGY_DROP_NAME = ${freq(bucket.strategyDrop, bucket.allUsage())}")
            }

            append("\n\nPositions:")
            positions.forEachIndexed { i, position ->
                append("\n${positionName(i)}: ")
                append("VPIP = ${freq(position.paid, position.canPay)}, ")
                append("RFR = ${freq(position.raised, position.canRaise)}, ")
                append("Buckets: ${ position.buckets.joinToString { freq(it, position.allUsage()) } }")
            }

            append("\n\nTable x Position matrix:\n${" ".repeat(19)}")
            repeat(6) { i ->
                append(positionName(i))
            }
            tables.forEachIndexed { index, table ->
                append("\n${tableName(index)}: ")
                repeat(6) { i ->
                    append("${freq(table.positions[i], table.allUsage())}, ")
                }
            }
        }
    }

    fun serialize(): String = (
            tables.flatMap { it.toList() } +
            positions.flatMap { it.toList() } +
            buckets.flatMap { it.toList() }
        ).joinToString()

    companion object {
        fun unserialize(saved: String): Statistics {
            val statistics = Statistics()
            if (saved.isEmpty()) {
                return statistics
            }
            val list = saved.split(',').map { it.trim().toInt() }
            var cursor = 0
            return statistics.apply {
                tables.forEach {
                    it.fromList(list.subList(cursor, cursor + it.size))
                    cursor += it.size
                }
                positions.forEach {
                    it.fromList(list.subList(cursor, cursor + it.size))
                    cursor += it.size
                }
                buckets.forEach {
                    it.fromList(list.subList(cursor, cursor + it.size))
                    cursor += it.size
                }
            }
        }

        fun freq(count: Int, all: Int): String {
            val frequency = if (all == 0) 0f else count.toFloat() / all * 100f
            return "%7.3f".format(frequency)
        }

        fun tableName(i: Int): String {
            require(i in TABLE_NAMES.indices)
            return "%-14s".format(TABLE_NAMES[i])
        }

        fun positionName(i: Int): String {
            require(i in TablePosition.entries.indices)
            return "%-9s".format(TablePosition.entries[i].name)
        }

        fun bucketName(i: Int): String {
            require(i in 0..19)
            return "%-6s".format("${i * 5}-${i * 5 + 5}")
        }

        private val TABLE_NAMES = arrayOf(
            "openRaise",
            "oneLimper",
            "manyLimper",
            "oneRaise",
            "oneRaisePaid",
            "manyRaise",
            "manyRaisePaid"
        )

        const val STRATEGY_AGGRESSIVE_NAME = "Aggressive"
        const val STRATEGY_PASSIVE_NAME = "Passive"
        const val STRATEGY_DROP_NAME = "Drop"
    }
}

interface SubStatistics {
    val size: Int
    fun toList(): List<Int>
    fun fromList(list: List<Int>)
}

class TableStatistics : SubStatistics {
    override val size = 29
    var buckets = IntArray(20)
    var positions = IntArray(6)
    var strategyAggressive = 0
    var strategyPassive = 0
    var strategyDrop = 0

    override fun toList(): List<Int> = buckets.toList() + positions.toList() +
        listOf(strategyAggressive, strategyPassive, strategyDrop)

    override fun fromList(list: List<Int>) {
        require(list.size == size)
        buckets = list.subList(0, 20).toIntArray()
        positions = list.subList(20, 26).toIntArray()
        strategyAggressive = list[26]
        strategyPassive = list[27]
        strategyDrop = list[28]
    }

    fun allUsage() = strategyDrop + strategyPassive + strategyAggressive
}

class PositionStatistics : SubStatistics {
    override val size = 24
    var buckets = IntArray(20)
    var canRaise = 0
    var raised = 0
    var canPay = 0
    var paid = 0

    override fun toList(): List<Int> = buckets.toList() + listOf(canRaise, raised, canPay, paid)

    override fun fromList(list: List<Int>) {
        require(list.size == size)
        buckets = list.subList(0, 20).toIntArray()
        canRaise = list[20]
        raised = list[21]
        canPay = list[22]
        paid = list[23]
    }

    fun allUsage() = buckets.sumOf { it }
}

class BucketStatistics : SubStatistics {
    override val size = 3
    var strategyAggressive = 0
    var strategyPassive = 0
    var strategyDrop = 0

    override fun toList(): List<Int> = listOf(strategyAggressive, strategyPassive, strategyDrop)

    override fun fromList(list: List<Int>) {
        require(list.size == size)
        strategyAggressive = list[0]
        strategyPassive = list[1]
        strategyDrop = list[2]
    }

    fun allUsage() = strategyDrop + strategyPassive + strategyAggressive
}