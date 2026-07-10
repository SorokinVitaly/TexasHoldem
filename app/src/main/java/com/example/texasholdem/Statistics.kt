package com.example.texasholdem

class Statistics {
    val tables = Array(7) { TableStatistics() }
    val positions = Array(7) { PositionStatistics() }
    val buckets = Array(20) { BucketStatistics() }

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
                    it.fromList(list.subList(cursor, cursor + 29))
                    cursor += 29
                }
                positions.forEach {
                    it.fromList(list.subList(cursor, cursor + 24))
                    cursor += 24
                }
                buckets.forEach {
                    it.fromList(list.subList(cursor, cursor + 3))
                    cursor += 3
                }
            }
        }
    }
}

class TableStatistics {
    var buckets = IntArray(20)
    var positions = IntArray(6)
    var strategyAggressive = 0
    var strategyPassive = 0
    var strategyDrop = 0

    fun toList(): List<Int> = buckets.toList() + positions.toList() +
        listOf(strategyAggressive, strategyPassive, strategyDrop)

    fun fromList(list: List<Int>) {
        require(list.size == 29)
        buckets = list.subList(0, 20).toIntArray()
        positions = list.subList(20, 26).toIntArray()
        strategyAggressive = list[26]
        strategyPassive = list[27]
        strategyDrop = list[28]
    }
}

class PositionStatistics {
    var buckets = IntArray(20)
    var canRaise = 0
    var raised = 0
    var canPay = 0
    var paid = 0

    fun toList(): List<Int> = buckets.toList() + listOf(canRaise, raised, canPay, paid)

    fun fromList(list: List<Int>) {
        require(list.size == 24)
        buckets = list.subList(0, 20).toIntArray()
        canRaise = list[20]
        raised = list[21]
        canPay = list[22]
        paid = list[23]
    }
}

class BucketStatistics {
    var strategyAggressive = 0
    var strategyPassive = 0
    var strategyDrop = 0

    fun toList(): List<Int> = listOf(strategyAggressive, strategyPassive, strategyDrop)

    fun fromList(list: List<Int>) {
        require(list.size == 3)
        strategyAggressive = list[0]
        strategyPassive = list[1]
        strategyDrop = list[2]
    }
}