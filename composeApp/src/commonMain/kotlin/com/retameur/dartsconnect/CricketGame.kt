package com.retameur.dartsconnect

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


class CricketGame(
    val nbPlayers: Int
) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var events = mutableListOf<BoardCell?>()

    var roundHits = mutableStateOf<List<BoardCell?>>(listOf(null, null, null))

    var currentPlayer = mutableStateOf(0)
    var currentRound = mutableStateOf(0)
    var playerStates = mutableStateListOf<CricketPlayerState>()

    init {
        require(nbPlayers > 0) { "Number of players must be greater than zero" }

        for (i in 0 until nbPlayers) {
            playerStates.add(CricketPlayerState())
        }

        scope.launch {
            DartboardDartsConnect.onHit.collect { cell ->
                hit(cell)
            }
        }

        scope.launch {
            DartboardDartsConnect.onRedButton.drop(1).collect { _ ->
                nextPlayer()
            }
        }
    }

    fun hit(cell: BoardCell) {
        println(cell)
        events.add(cell)

        if(nbThrows() >= 3) {
            events.add(null)
        }

        computeScores()
    }

    fun nextPlayer() {
        events.add(null)
        computeScores()
    }
    
    fun cancelLastEvent() {
        if(events.count() > 0) {
            events.removeLast()
            computeScores()
        }
    }

    private fun nbThrows(): Int {
        var counter = 0
        for(i in events.count() - 1 downTo 0) {
            if(events[i] == null) return counter
            counter += 1
        }
        return counter
    }

    private fun computeScores() {
        var player = 0
        var round = 0
        val states = mutableListOf<CricketPlayerState>()

        for (i in 0 until nbPlayers) {
            states.add(CricketPlayerState())
        }

        for(event in events) {
            if(event == null) {
                player = (player + 1)
                if(player >= nbPlayers) {
                    round += 1
                    player = 0
                }
                continue
            }

            val points = states[player].add(event.number, event.multiplier)
            for(state in states) {
                if(!state.isClosed(event.number)) {
                    state.score += points
                }
            }
        }

        this.currentRound.value = round
        this.currentPlayer.value = player

        val lastRoundStart =  events.indexOfLast { it == null } + 1

        if(lastRoundStart >= events.count()) {
            this.roundHits.value = listOf(null, null, null)
        } else {
            val hits = events.subList(lastRoundStart, events.lastIndex + 1).toMutableList()
            while(hits.count() < 3) {
                hits += null
            }
            this.roundHits.value = hits
        }

        this.playerStates.clear()
        this.playerStates.addAll(states)
    }
}

class CricketPlayerState() {
    var numbers = mutableMapOf(
        20 to 0,
        19 to 0,
        18 to 0,
        17 to 0,
        16 to 0,
        15 to 0,
        25 to 0
    )

    var score = 0

    fun add(number: Int, multiplier: Int): Int {
        if(!numbers.containsKey(number)) return 0

        val remainingToClose = 3 - numbers[number]!!
        val extra = max(0,multiplier - remainingToClose)
        numbers[number] = min(3, numbers[number]!! + multiplier)
        return extra * number
    }

    fun isClosed(number: Int): Boolean {
        if(!numbers.containsKey(number)) return false

        return numbers[number] == 3
    }
}

