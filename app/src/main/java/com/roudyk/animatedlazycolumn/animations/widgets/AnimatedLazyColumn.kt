package com.roudyk.animatedlazycolumn.animations.widgets

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roudyk.animatedlazycolumn.animations.AnimatedLazyListItem
import com.roudyk.animatedlazycolumn.animations.AnimatedLazyListViewModel
import com.roudyk.animatedlazycolumn.animations.animatedLazyListScope
import kotlinx.coroutines.Dispatchers

@Composable
fun <T> AnimatedLazyColumn(
    state: LazyListState,
    modifier: Modifier = Modifier,
    items: List<AnimatedLazyListItem<T>>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    animationDuration: Int = 400,
    initialEnter: EnterTransition = fadeIn(),
    enter: EnterTransition = fadeIn(
        animationSpec = tween(delayMillis = animationDuration / 3),
    ) + expandVertically(
        animationSpec = tween(durationMillis = animationDuration),
        expandFrom = Alignment.Top
    ),
    exit: ExitTransition = fadeOut() + shrinkVertically(
        animationSpec = tween(durationMillis = animationDuration),
        shrinkTowards = Alignment.Top
    ),
    finalExit: ExitTransition = exit
) {
    val scope = rememberCoroutineScope { Dispatchers.Main }
    val viewModel = remember { AnimatedLazyListViewModel<T>(scope, animationDuration) }
    viewModel.updateList(items)
    val currentItems by viewModel.items.collectAsState(emptyList())

    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        content = animatedLazyListScope(
            currentItems = currentItems,
            initialEnter = initialEnter,
            enter = enter,
            exit = exit,
            finalExit = finalExit,
            isVertical = true,
            spacing = verticalArrangement.spacing
        )
    )
}