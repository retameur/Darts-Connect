package com.retameur.dartsconnect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CricketGameScreen(game: CricketGame) {

    val round by game.currentRound

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))
        Text("Round: ${round + 1}", style = MaterialTheme.typography.h4)
        Spacer(Modifier.weight(1f))
        RoundHits(game.roundHits.value)
        Spacer(Modifier.weight(1f))
        CricketScoreBoard(game)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = {
                game.cancelLastEvent()
            }) {
                Icon(Icons.Rounded.History, null, Modifier.size(40.dp))
            }

            IconButton(onClick = {
                game.nextPlayer()
            }) {
                Icon(Icons.Rounded.SkipNext, null, Modifier.size(40.dp))
            }
        }
    }
}

@Composable
fun CricketScoreBoard(game: CricketGame) {

    val slices = listOf(20, 19, 18, 17, 16, 15, 25)

    Row(
        Modifier.border(2.dp,  Color.Black)
    ) {
        Column {
            slices.forEach { slice ->
                val text = if(slice != 25) "${slice}" else "Bull"
                CricketNumberHeaderCell(text, onClick = {
                    game.hit(BoardCell(slice, 1))
                })
            }
            CricketScoreHeaderCell()
        }

        game.playerStates.forEachIndexed { index, state ->
            val bgColor = if(index == game.currentPlayer.value) MaterialTheme.colors.primary.copy(alpha = 0.2f) else Color.Transparent
            Column(
                Modifier.background(bgColor)
            ) {
                slices.forEach { slice ->
                    CricketNumberCell(state.numbers[slice]!!)
                }
                CricketScoreCell(state.score)
            }
        }
    }
}

@Composable
fun RoundHits(hits: List<BoardCell?>) {
    Row {
        Text("${hits[0] ?: "*"}")
        Text(" | ")
        Text("${hits[1] ?: "*"}")
        Text(" | ")
        Text("${hits[2] ?: "*"}")
    }
}

@Composable
fun CricketNumberHeaderCell(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp, 60.dp)
            .border(0.5.dp,  Color.Black)
            .wrapContentHeight(Alignment.CenterVertically),
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            elevation = ButtonDefaults.elevation(0.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent
            ),
        ) {
            Text(text, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CricketScoreHeaderCell() {
    Text("Score",
        Modifier
            .size(60.dp, 60.dp)
            .border(0.5.dp,  Color.Black)
            .wrapContentHeight(Alignment.CenterVertically),
        textAlign = TextAlign.Center,
    )
}

@Composable
fun CricketNumberCell(nbHits: Int) {
    Box(
        modifier = Modifier
            .size(60.dp, 60.dp)
            .border(0.5.dp,  Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (nbHits) {
            1 -> Icon(Icons.Outlined.Remove, null)
            2 -> Icon(Icons.Outlined.Add, null)
            3 -> Icon(Icons.Outlined.AddCircleOutline, null)
        }
    }
}

@Composable
fun CricketScoreCell(score: Int) {
    Box(
        modifier = Modifier
            .size(60.dp, 60.dp)
            .border(0.5.dp,  Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(score.toString(), textAlign = TextAlign.Center)
    }
}