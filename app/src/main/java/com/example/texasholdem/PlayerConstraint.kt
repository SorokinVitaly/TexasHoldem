package com.example.texasholdem

import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstrainScope
import androidx.constraintlayout.compose.ConstrainedLayoutReference

fun playerConstraint(
    index: Int,
    orientation: Boolean,
    refs: List<ConstrainedLayoutReference>
): ConstrainScope.() -> Unit = {
    val margin = 2.dp
    if (orientation) {
        when (index) {
            0 -> {
                centerHorizontallyTo(parent)
                bottom.linkTo(parent.bottom, margin = margin)
            }

            1 -> {
                top.linkTo(refs[2].bottom)
                bottom.linkTo(parent.bottom)
                absoluteLeft.linkTo(parent.absoluteLeft, margin = margin)
            }

            2 -> {
                top.linkTo(parent.top)
                bottom.linkTo(refs[1].top)
                absoluteLeft.linkTo(parent.absoluteLeft, margin = margin)
            }

            3 -> {
                centerHorizontallyTo(parent)
                top.linkTo(parent.top, margin = margin)
            }

            4 -> {
                top.linkTo(parent.top)
                bottom.linkTo(refs[5].top)
                absoluteRight.linkTo(parent.absoluteRight, margin = margin)
            }

            5 -> {
                top.linkTo(refs[4].bottom)
                bottom.linkTo(parent.bottom)
                absoluteRight.linkTo(parent.absoluteRight, margin = margin)
            }

            else -> throw IllegalArgumentException("Invalid player index: $index")
        }

    } else {
        when (index) {
            0 -> {
                bottom.linkTo(parent.bottom, margin = margin)
                absoluteLeft.linkTo(refs[1].absoluteRight)
                absoluteRight.linkTo(parent.absoluteRight)
            }

            1 -> {
                bottom.linkTo(parent.bottom, margin = margin)
                absoluteLeft.linkTo(parent.absoluteLeft)
                absoluteRight.linkTo(refs[0].absoluteLeft)
            }

            2 -> {
                centerVerticallyTo(parent)
                absoluteLeft.linkTo(parent.absoluteLeft, margin = margin)
            }

            3 -> {
                top.linkTo(parent.top, margin = margin)
                absoluteLeft.linkTo(parent.absoluteLeft)
                absoluteRight.linkTo(refs[4].absoluteLeft)
            }

            4 -> {
                top.linkTo(parent.top, margin = margin)
                absoluteLeft.linkTo(refs[3].absoluteRight)
                absoluteRight.linkTo(parent.absoluteRight)
            }

            5 -> {
                centerVerticallyTo(parent)
                absoluteRight.linkTo(parent.absoluteRight, margin = margin)
            }

            else -> throw IllegalArgumentException("Invalid player index: $index")
        }
    }
}