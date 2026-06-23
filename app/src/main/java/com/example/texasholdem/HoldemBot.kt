package com.example.texasholdem

/* Ключевое архитектурное упрощение: раньше (в ответе про BoardDanger) приходилось вручную определять опасность борда — пара/трипс/флеш-дро на доске, скрытый сет vs трипс на виду.
 Теперь это не нужно — equity, посчитанная методом Монте-Карло, уже учитывает это автоматически: если твоя лучшая комбинация целиком на борде, у случайного оппонента тот же доступ
 к этим картам, и его симулированная рука будет чаще равна или сильнее — это естественно снижает equity без отдельной логики.
*/

fun selectBotStrategy(
    preFlopStrength: HandStrength,
    equity: Float,
    potOdds: Float,
    cards: List<Card>,
    communityCards: List<Card>,
    numOfRaise: Int,
    isLatePosition: Boolean,
    isFacingBet: Boolean,
    round: RoundType,
    history: List<Pair<Int, ActionType>>,       // история всей раздачи
    roundHistory: List<Pair<Int, ActionType>>   // история текущего раунда торгов
): BettingStrategy {

    val strength = if (round == RoundType.PRE_FLOP) {
        preFlopStrength
    } else {
        equityToStrength(equity, potOdds)
    }

    val tableIsAggressive = isAggressiveTable(history)
    val tableIsPassive = isPassiveRound(roundHistory)

    val semiBluffPotential = round != RoundType.PRE_FLOP && round != RoundType.RIVER &&
            hasStrongDraw(currentIncompleteType(cards, communityCards))

    return resolveStrategy(
        strength = strength,
        numOfRaise = numOfRaise,
        isLatePosition = isLatePosition,
        isFacingBet = isFacingBet,
        tableIsAggressive = tableIsAggressive,
        tableIsPassive = tableIsPassive,
        semiBluffPotential = semiBluffPotential
    )
}

// --- Equity > HandStrength ------------------------------------------

private const val MONSTER_EQUITY = 0.85f
private const val VALUE_RAISE_EDGE = 0.30f
private const val MEDIUM_EDGE = 0.10f
private const val MARGINAL_CALL_EDGE = 0f

private fun equityToStrength(equity: Float, potOdds: Float): HandStrength {
    val edge = equity - potOdds // насколько equity превышает требуемый минимум
    return when {
        equity >= MONSTER_EQUITY  -> HandStrength.MONSTER
        edge >= VALUE_RAISE_EDGE  -> HandStrength.STRONG
        edge >= MEDIUM_EDGE       -> HandStrength.MEDIUM
        edge >= MARGINAL_CALL_EDGE -> HandStrength.DRAWING // математически на грани выгодности
        else                       -> HandStrength.WEAK
    }
}

// --- Сигналы со стола ------------------------------------------------

private fun isAggressiveTable(history: List<Pair<Int, ActionType>>): Boolean =
    history.count { it.second is ActionType.Raise } > 1

private fun isPassiveRound(roundHistory: List<Pair<Int, ActionType>>): Boolean =
    roundHistory.none { it.second is ActionType.Raise } &&
            roundHistory.any { it.second is ActionType.Call }

// --- Полу-блеф на основе драва ---------------------------------------

private fun currentIncompleteType(cards: List<Card>, communityCards: List<Card>): IncompleteCombinationType {
    val combination = calcCombination(communityCards, cards)
    return calcIncompleteCombination(communityCards, cards, combination).type
}

private fun hasStrongDraw(type: IncompleteCombinationType): Boolean =
    type == IncompleteCombinationType.FOUR_TO_FLUSH ||
            type == IncompleteCombinationType.FOUR_TO_STRAIGHT_OPEN

// --- Финальное решение -----------------------------------------------

private fun resolveStrategy(
    strength: HandStrength,
    numOfRaise: Int,
    isLatePosition: Boolean,
    isFacingBet: Boolean,
    tableIsAggressive: Boolean,
    tableIsPassive: Boolean,
    semiBluffPotential: Boolean
): BettingStrategy {

    // На агрессивном столе требуем меньше рейзов чтобы перейти в защитный режим
    val raiseThreshold = if (tableIsAggressive) 1 else 2

    return when (strength) {

        HandStrength.MONSTER -> BettingStrategy.AGGRESSIVE

        HandStrength.STRONG -> when {
            !isFacingBet                  -> BettingStrategy.AGGRESSIVE
            numOfRaise < raiseThreshold    -> BettingStrategy.AGGRESSIVE
            else                            -> BettingStrategy.PASSIVE
        }

        HandStrength.MEDIUM -> when {
            !isFacingBet && isLatePosition && numOfRaise == 0 -> BettingStrategy.AGGRESSIVE
            !isFacingBet                                        -> BettingStrategy.PASSIVE
            numOfRaise < raiseThreshold                          -> BettingStrategy.PASSIVE
            else                                                  -> BettingStrategy.DROP
        }

        HandStrength.DRAWING -> when {
            !isFacingBet      -> BettingStrategy.PASSIVE
            numOfRaise == 0   -> BettingStrategy.PASSIVE // колл оправдан по equity/potOdds
            else               -> BettingStrategy.DROP
        }

        HandStrength.WEAK -> when {
            !isFacingBet && semiBluffPotential && isLatePosition
                    && tableIsPassive && numOfRaise == 0 -> BettingStrategy.AGGRESSIVE // полу-блеф

            !isFacingBet && isLatePosition
                    && tableIsPassive && numOfRaise == 0 -> BettingStrategy.AGGRESSIVE // чистый блеф

            !isFacingBet -> BettingStrategy.PASSIVE
            else          -> BettingStrategy.DROP
        }
    }
}

/* Логика по приоритету
Раунд			Источник силы руки
PRE_FLOP		preFlopStrength — дешёвая эвристика без запуска Монте-Карло
FLOP / TURN / RIVER	equityToStrength(equity, potOdds) — основано на разнице equity - potOdds

semiBluffPotential — добавляет агрессию для слабой по equity руки, если есть сильный незавершённый дро (FOUR_TO_FLUSH/FOUR_TO_STRAIGHT_OPEN)
и стол пассивен и позиция поздняя. Это рационально: даже если equity низкая прямо сейчас, ставка может (а) выиграть банк немедленно через фолд-эквити,
(б) если коллируют — у бота всё ещё есть шанс на улучшение карты на следующей улице.
tableIsAggressive снижает raiseThreshold — на агрессивном столе бот переходит в защитный режим (PASSIVE/DROP) раньше, при меньшем количестве рейзов в раунде.

Допущения
Место				Допущение
ActionType.Raise/Call		Использую те же подтипы что в MainViewModel.kt/History.kt из прошлых сообщений
numOfRaise			Используется как стратегический сигнал давления, не как проверка доступности рейза — проверка canRaise/максимума рейзов, по ранее
				согласованному принципу, остаётся в мапере который превращает BettingStrategy в конкретный ActionType
HandStrength.DRAWING на RIVER	На ривере это уже не "драв" в буквальном смысле (карт больше не будет), а просто маржинальная по equity рука около порога выгодности —
				название сохранено для единообразия enum, но семантика чуть отличается
currentIncompleteType		Вызывает calcCombination/calcIncompleteCombination заново внутри selectBotStrategy — если эти значения уже посчитаны раньше в вызывающем коде,
				лучше передать готовый IncompleteCombinationType как параметр вместо пересчёта
Пороги 0.10f / 0.30f / 0.85f	Подбираются эмпирически под твой % выигрыша от агрессии — стоит протестировать и скорректировать на реальных раздачах
*/