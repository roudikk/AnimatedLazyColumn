package com.roudyk.animatedlazycolumn

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Integer.max

private enum class AnimatedItemState {
    INITIAL, INSERTED, REMOVED, IDLE
}

private data class AnimatedItem<T>(
    val value: KeyItem<T>,
    val state: AnimatedItemState
)

data class KeyItem<out T>(
    val key: String,
    val value: T? = null,
    val composable: @Composable () -> Unit
)

private class AnimatedColumnViewModel<T>(
    private val scope: CoroutineScope,
    private val animationDuration: Int
) {

    data class ItemsUpdate<T>(
        val previousList: List<KeyItem<T>>,
        val currentList: List<KeyItem<T>>,
        val insertedPositions: List<Int>,
        val removedPositions: List<Int>
    )

    val items = MutableStateFlow<List<AnimatedItem<T>>>(emptyList())

    private val mutex = Mutex()

    private val itemDiffer = AsyncListDiffer(
        ListCallback(),
        AsyncDifferConfig.Builder(ItemsCallback()).build()
    )

    private val diffFlow = MutableSharedFlow<ItemsUpdate<T>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    private val insertedPositions = mutableListOf<Int>()
    private val removedPositions = mutableListOf<Int>()
    private val movedPositions = mutableListOf<Pair<Int, Int>>()

    init {
        itemDiffer.addListListener { previousList, currentList ->
            scope.launch {
                diffFlow.emit(
                    ItemsUpdate(
                        previousList,
                        currentList,
                        insertedPositions,
                        removedPositions
                    )
                )
            }
        }

        diffFlow
            .onEach { (previousList, currentList, insertedPositions, removedPositions) ->
                mutex.withLock {
                    if (items.value.isEmpty()) {
                        items.emit(currentList.map {
                            AnimatedItem(
                                value = it,
                                state = AnimatedItemState.INITIAL
                            )
                        })
                        delay(animationDuration.toLong())
                        items.emit(items.value.map {
                            it.copy(state = AnimatedItemState.IDLE)
                        })
                        return@onEach
                    }
                    val intermediateList = mutableListOf<AnimatedItem<T>>()
                    intermediateList.addAll(currentList.mapIndexed { index, item ->
                        AnimatedItem(
                            value = item,
                            state = if (insertedPositions.contains(index)
                                || movedPositions.find { it.second == index } != null
                            ) {
                                AnimatedItemState.INSERTED
                            } else {
                                AnimatedItemState.IDLE
                            }
                        )
                    })
                    removedPositions.forEach {
                        val index = max(
                            0,
                            if (it > intermediateList.size) intermediateList.size - 1 else it
                        )
                        intermediateList.add(
                            index,
                            AnimatedItem(
                                value = previousList[index],
                                state = AnimatedItemState.REMOVED
                            )
                        )
                    }
                    movedPositions.forEach {
                        val item = previousList[it.first]
                        intermediateList.add(
                            if (it.first > intermediateList.size) intermediateList.size else it.first + 1,
                            AnimatedItem(
                                value = item.copy(key = "${item.key}-temp"),
                                state = AnimatedItemState.REMOVED
                            )
                        )
                    }

                    items.emit(intermediateList.distinctBy { it.value.key })
                    delay(animationDuration.toLong())
                    items.emit(currentList.map {
                        AnimatedItem(
                            value = it,
                            state = AnimatedItemState.IDLE
                        )
                    })
                }
            }
            .launchIn(scope)
    }

    fun updateList(newList: List<KeyItem<T>>) {
        scope.launch {
            mutex.withLock {
                movedPositions.clear()
                insertedPositions.clear()
                removedPositions.clear()
                itemDiffer.submitList(newList)
            }
        }
    }

    inner class ItemsCallback : DiffUtil.ItemCallback<KeyItem<T>>() {
        override fun areItemsTheSame(oldItem: KeyItem<T>, newItem: KeyItem<T>): Boolean {
            return oldItem.key == newItem.key
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: KeyItem<T>, newItem: KeyItem<T>): Boolean {
            return oldItem.value == newItem.value
        }
    }

    inner class ListCallback : ListUpdateCallback {

        override fun onInserted(position: Int, count: Int) {
            scope.launch {
                mutex.withLock {
                    for (i in 0 until count) {
                        insertedPositions.add(position + i)
                    }
                }
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            scope.launch {
                mutex.withLock {
                    for (i in 0 until count) {
                        removedPositions.add(position + i)
                    }
                }
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            scope.launch {
                mutex.withLock {
                    movedPositions.add(fromPosition to toPosition)
                }
            }
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {}
    }
}

@Composable
fun <T> AnimatedLazyColumn(
    state: LazyListState,
    modifier: Modifier = Modifier,
    items: List<KeyItem<T>>,
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
) {
    val scope = rememberCoroutineScope { Dispatchers.Main }
    val viewModel = remember { AnimatedColumnViewModel<T>(scope, animationDuration) }
    viewModel.updateList(items)
    val currentItems by viewModel.items.collectAsState(emptyList())

    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout
    ) {
        currentItems.forEachIndexed { index, item ->
            item(key = item.value.key) {
                val transitionState = remember("${item.value.key}-$index") {
                    MutableTransitionState(
                        when (item.state) {
                            AnimatedItemState.INITIAL -> false
                            AnimatedItemState.INSERTED -> false
                            AnimatedItemState.REMOVED -> true
                            AnimatedItemState.IDLE -> true
                        }
                    )
                }
                transitionState.targetState = when (item.state) {
                    AnimatedItemState.INITIAL -> true
                    AnimatedItemState.INSERTED -> true
                    AnimatedItemState.REMOVED -> false
                    AnimatedItemState.IDLE -> true
                }

                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = when (item.state) {
                        AnimatedItemState.INITIAL -> initialEnter
                        else -> enter
                    },
                    exit = exit
                ) {
                    Box(
                        modifier = Modifier.padding(bottom = verticalArrangement.spacing)
                    ) {
                        item.value.composable()
                    }
                }
            }
        }
    }
}