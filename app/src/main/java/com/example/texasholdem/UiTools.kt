package com.example.texasholdem

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage


@Composable
fun Player(
    playerData: PlayerData,
    isCardsOpen: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = playerData.name,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )

        if (playerData.isActive) {
            Cards(playerData.cards, isCardsOpen)
        } else {
            Spacer(modifier = Modifier
                .height(cardHeight)
                .width(cardWidth)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (playerData.isDealer) {
                DealerButton()
            }
            ChipIcon()
            Text(
                text = playerData.chips.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }

        Text(
            text = playerData.lastBet.name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray
        )
    }
}

@Composable
fun Cards(cards: List<Card>, isCardsOpen: Boolean) =
    Box {
        cards.forEachIndexed { index, card ->
            Box(modifier = Modifier
                .padding(start = (index * 20).dp)
                .animateContentSize()
            ) {
                Card(card, isCardsOpen)
            }
        }
    }

@Composable
fun Card(card: Card, isCardsOpen: Boolean) {
    val (assetName, borderStroke) = if (!isCardsOpen) card.backAssetName to null
    else card.faceAssetName to BorderStroke(width = 1.dp, color = Color.Black)

    Card(
        border = borderStroke,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .height(cardHeight)
            .width(cardWidth)
    ) {
        val imageLoader = (LocalContext.current.applicationContext as MainApp).imageLoader
        AsyncImage(
            model = assetName,
            imageLoader = imageLoader,
            contentDescription = "Card",
        )
    }
}

@Composable
fun AppButton(text: String, onClick: () -> Unit = {}) {
    TextButton(
        onClick = onClick,
        border = BorderStroke(2.dp, Color.Yellow),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = Color.Yellow
        )
    }
}

@Composable
fun ResetGameButton(onResetGame: () -> Unit) {
    var showConfirmation by rememberSaveable { mutableStateOf(false) }
    AppButton("Reset game") { showConfirmation = true }
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Reset game?") },
            text = { Text("Your current progress will be lost") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmation = false
                        onResetGame()
                    }
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DealerButton() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .border(width = 2.dp, color = Color.Black, shape = CircleShape)
            .background(color = Color.White, shape = CircleShape)
    ) {
        Text(
            text = "DEALER",
            color = Color.Black,
            fontSize = 5.sp
        )
    }
}

@Composable
fun ChipIcon() {
    Icon(
        imageVector = ImageVector.vectorResource(R.drawable.poker_chip),
        tint = Color.Red,
        contentDescription = "Poker chip",
        modifier = Modifier.size(24.dp)
    )
}

@Composable
fun Bank(
    bankChips: Int,
    communityCards: List<Card>,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (communityCards.isEmpty()) {
            Spacer(Modifier.height(cardHeight))
        } else {
            Cards(communityCards, true)
        }
        Text(
            text = "Bank",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipIcon()
            Text(
                text = bankChips.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

val cardHeight = 88.dp
val cardWidth = 63.dp