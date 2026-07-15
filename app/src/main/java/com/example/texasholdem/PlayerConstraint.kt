package com.example.texasholdem

import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstrainScope


fun playerConstraint(index: Int, portrait: Boolean): ConstrainScope.() -> Unit = {
    val margin = 2.dp
    if (portrait) {
        when (index) {
            0 -> {
                centerHorizontallyTo(parent)
                bottom.linkTo(parent.bottom, margin = margin)
            }
            1, 2 -> {
                absoluteLeft.linkTo(parent.absoluteLeft, margin = margin)
            }
            3 -> {
                centerHorizontallyTo(parent)
                top.linkTo(parent.top, margin = margin)
            }
            4, 5 -> {
                absoluteRight.linkTo(parent.absoluteRight, margin = margin)
            }
            else -> throw IllegalArgumentException("Invalid player index: $index")
        }
    } else {
        when (index) {
            0, 1 -> {
                bottom.linkTo(parent.bottom, margin = margin)
            }
            2 -> {
                centerVerticallyTo(parent)
                absoluteLeft.linkTo(parent.absoluteLeft, margin = margin)
            }
            3, 4 -> {
                top.linkTo(parent.top, margin = margin)
            }
            5 -> {
                centerVerticallyTo(parent)
                absoluteRight.linkTo(parent.absoluteRight, margin = margin)
            }
            else -> throw IllegalArgumentException("Invalid player index: $index")
        }
    }
}