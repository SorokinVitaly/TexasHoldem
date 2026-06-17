package com.example.texasholdem

sealed class ActionType(val name: String, val bet: Int = 0) {
    class SmallBlind(bet: Int) : ActionType("SB", bet)
    class BigBlind(bet: Int) : ActionType("BB", bet)
    class Bet(bet: Int) : ActionType("Bet $bet", bet)
    class Call(bet: Int) : ActionType("Call $bet", bet)
    class Raise(bet: Int) : ActionType("Raise $bet", bet)
    class Check : ActionType("Check")
    class Fold : ActionType("Fold")
    class NoAction : ActionType("", -1)
}