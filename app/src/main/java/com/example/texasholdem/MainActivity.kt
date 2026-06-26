package com.example.texasholdem

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            MainScreen(state)
        }
    }

    @Composable
    fun MainScreen(state: ScreenState) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    is UiEvent.ShowToast -> {
                        Toast.makeText(
                            context,
                            event.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        Surface(
            color = colorResource(R.color.backGround),
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Bank(
                        bankChips = state.bankChips,
                        communityCards = state.communityCards,
                        modifier = Modifier.constrainAs(createRef()) { centerTo(parent) }
                    )
                    val orientation = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
                    val refs = state.players.indices.map { createRef() }
                    state.players.forEachIndexed { index, player ->
                        val isCardsOpen = when {
                            index == 0 -> true
                            !player.isInGame -> false
                            else -> state.isCardsOpen
                        }
                        Player(
                            playerData = player,
                            isCardsOpen = isCardsOpen,
                            modifier = Modifier.constrainAs(
                                refs[index],
                                playerConstraint(index, orientation, refs)
                            )
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    ActionBar(state)
                }
            }
        }
    }

    @Composable
    fun ActionBar(state: ScreenState) {
        if (state.isActionAvailable) {
            if (state.actionsAvailable.isNotEmpty()) {
                state.actionsAvailable.forEach { action ->
                    AppButton(action.name) { viewModel.onAction(action) }
                }
            } else {
                if (state.isDealAvailable) {
                    AppButton("Deal next", viewModel::onDialNext)
                }
                if (state.isResetAvailable) {
                    AppButton("Reset game", viewModel::onResetGame)
                }
            }
        } else {
            CircularProgressIndicator(
                color = Color.Yellow,
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
            )
        }
    }
}