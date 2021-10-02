package com.roudyk.animatedlazycolumn.animations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

internal fun <T> animatedLazyListScope(
    currentItems: List<AnimatedItem<T>>,
    initialEnter: EnterTransition,
    enter: EnterTransition,
    exit: ExitTransition,
    finalExit: ExitTransition,
    isVertical: Boolean,
    spacing: Dp
): LazyListScope.() -> Unit = {
    currentItems.forEachIndexed { index, item ->
        item(key = item.value.key) {
            val transitionState = remember("${item.value.key}-$index") {
                MutableTransitionState(
                    when (item.state) {
                        AnimatedItemState.INITIAL -> false
                        AnimatedItemState.INSERTED -> false
                        AnimatedItemState.REMOVED -> true
                        AnimatedItemState.IDLE -> true
                        AnimatedItemState.ALL_REMOVED -> true
                    }
                )
            }
            transitionState.targetState = when (item.state) {
                AnimatedItemState.INITIAL -> true
                AnimatedItemState.INSERTED -> true
                AnimatedItemState.REMOVED -> false
                AnimatedItemState.IDLE -> true
                AnimatedItemState.ALL_REMOVED -> false
            }

            AnimatedVisibility(
                visibleState = transitionState,
                enter = when (item.state) {
                    AnimatedItemState.INITIAL -> initialEnter
                    else -> enter
                },
                exit = when (item.state) {
                    AnimatedItemState.ALL_REMOVED -> finalExit
                    else -> exit
                }
            ) {
                Box(
                    modifier = Modifier.let {
                        if (isVertical) {
                            it.padding(bottom = spacing)
                        } else {
                            it.padding(end = spacing)
                        }
                    }
                ) {
                    item.value.composable()
                }
            }
        }
    }
}