package com.example.texasholdem

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!playerData.isActive) {
        return
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = playerData.name,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        Box {
            playerData.cards.forEachIndexed { index, card ->
                val isSelected = card in playerData.selectedCards
                Box(modifier = Modifier
                    .padding(start = (index * 20).dp)
                    .offset(y = if (isSelected) (-10).dp else 0.dp)
                    .animateContentSize()
                ) {
                    Card(
                        card = card,
                        isCardsOpen = isCardsOpen,
                        isSelected = isSelected,
                        onClick = { onCardClick(card) }
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (playerData.isDialer) {
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
            text = playerData.footerText,
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray
        )
    }
}

@Composable
fun Card(
    card: Card,
    isCardsOpen: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val assetName = if (isCardsOpen) card.faceAssetName else card.backAssetName
    val borderColor = if (isSelected) Color.Yellow else Color.Black
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Card(
        border = BorderStroke(width = borderWidth, color = borderColor),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
            .height(cardHeight)
            .width(cardWidth)
            .clickable(onClick = onClick)
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
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
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