package com.example.texasholdem

sealed class ActionType(
    val name: String,
    val payNow: Int = 0,
    val paid: Int = 0
) {
    class SmallBlind() : ActionType(
        name = "Small Blind",
        payNow = SMALL_BLIND,
        paid = SMALL_BLIND
    )
    class BigBlind() : ActionType(
        name = "Big Blind",
        payNow = BIG_BLIND,
        paid = BIG_BLIND
    )

    class Check(currentBet: Int = 0) : ActionType(name = "Check", paid = currentBet)
    class Bet(bet: Int) : ActionType(name = "Bet $bet",payNow = bet, paid = bet)

    class Call(currentBet: Int, prevPaid: Int) : ActionType(
        name = "Call ${currentBet - prevPaid}",
        payNow = currentBet - prevPaid,
        paid = currentBet
    )

    class Raise(raiseTo: Int, prevPaid: Int) : ActionType(
        name = "Raise to $raiseTo",
        payNow = raiseTo - prevPaid,
        paid = raiseTo
    )

    class Fold : ActionType("Fold")
    class NoAction : ActionType("")

    // don't want use kotlin serialization here because I don't need Json
    fun serialize() = "($name:$payNow:$paid)"

    companion object {
        fun unserialize(saved: String): ActionType {
            if (saved.isEmpty()) {
                return NoAction()
            }
            val list = saved.splitItems(':')
            require(list.size == 3)
            val name = list[0]
            val payNow = list[1].toInt()
            val paid = list[2].toInt()
            return when {
                name.isEmpty() -> NoAction()
                name == "Fold" -> Fold()
                name == "Check" -> Check(paid)
                name.startsWith("Small") -> SmallBlind()
                name.startsWith("Big") -> BigBlind()
                name.startsWith("Bet") -> Bet(paid)
                name.startsWith("Call") -> Call(paid,paid - payNow)
                name.startsWith("Raise") -> Raise(paid,paid - payNow)
                else -> throw IllegalArgumentException()
            }
        }
    }
}