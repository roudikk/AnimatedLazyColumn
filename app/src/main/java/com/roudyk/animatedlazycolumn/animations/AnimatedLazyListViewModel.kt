package com.roudyk.animatedlazycolumn.animations

import android.annotation.SuppressLint
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.roudyk.animatedlazycolumn.animations.widgets.AnimatedLazyListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Integer.max

internal enum class AnimatedItemState {
    INITIAL, INSERTED, REMOVED, IDLE, ALL_REMOVED
}

internal data class AnimatedItem<T>(
    val value: AnimatedLazyListItem<T>,
    val state: AnimatedItemState
)

internal class AnimatedLazyListViewModel<T>(
    private val scope: CoroutineScope,
    private val animationDuration: Int
) {

    data class ItemsUpdate<T>(
        val previousList: List<AnimatedLazyListItem<T>>,
        val currentList: List<AnimatedLazyListItem<T>>,
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
                    val allRemoved = currentList.isEmpty() && removedPositions.isNotEmpty()
                    intermediateList.addAll(currentList.mapIndexed { index, item ->
                        AnimatedItem(
                            value = item,
                            state = when {
                                insertedPositions.contains(index)
                                        || movedPositions.find { it.second == index } != null -> {
                                    AnimatedItemState.INSERTED
                                }
                                else -> AnimatedItemState.IDLE
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
                                state = if (allRemoved) {
                                    AnimatedItemState.ALL_REMOVED
                                } else {
                                    AnimatedItemState.REMOVED
                                }
                            )
                        )
                    }
                    if (!allRemoved) {
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

    fun updateList(newList: List<AnimatedLazyListItem<T>>) {
        scope.launch {
            mutex.withLock {
                movedPositions.clear()
                insertedPositions.clear()
                removedPositions.clear()
                itemDiffer.submitList(newList)
            }
        }
    }

    inner class ItemsCallback : DiffUtil.ItemCallback<AnimatedLazyListItem<T>>() {
        override fun areItemsTheSame(
            oldItem: AnimatedLazyListItem<T>,
            newItem: AnimatedLazyListItem<T>
        ): Boolean {
            return oldItem.key == newItem.key
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(
            oldItem: AnimatedLazyListItem<T>,
            newItem: AnimatedLazyListItem<T>
        ): Boolean {
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