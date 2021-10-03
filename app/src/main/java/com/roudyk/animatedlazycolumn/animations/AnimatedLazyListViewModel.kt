package com.roudyk.animatedlazycolumn.animations

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class AnimatedItemState {
    INITIAL, INSERTED, REMOVED, IDLE, ALL_REMOVED
}

internal data class AnimatedItem<T>(
    val value: AnimatedLazyListItem<T>,
    val state: AnimatedItemState
)

internal class AnimatedLazyListViewModel<T>(
    private val scope: CoroutineScope,
    private val animationDuration: Int,
    private val reverseLayout: Boolean
) {

    val items = MutableStateFlow<List<AnimatedItem<T>>>(emptyList())

    private var previousList = emptyList<AnimatedLazyListItem<T>>()

    data class ItemsUpdate<T>(
        val insertedPositions: List<Int>,
        val removedPositions: List<Int>,
        val movedPositions: List<Pair<Int, Int>>,
        val changedPositions: List<Int>,
        val previousList: List<AnimatedLazyListItem<T>>,
        val currentList: List<AnimatedLazyListItem<T>>
    )

    private var job: Job? = null
    private val mutex = Mutex()
    private val itemsUpdateFlow = MutableSharedFlow<ItemsUpdate<T>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    init {
        itemsUpdateFlow
            .onEach { (
                          insertedPositions,
                          removedPositions,
                          movedPositions,
                          changedPositions,
                          currentPreviousList,
                          currentList
                      ) ->
                if (insertedPositions.isEmpty() && removedPositions.isEmpty()
                    && movedPositions.isEmpty() && changedPositions.isEmpty()
                ) {
                    return@onEach
                }
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
                        previousList = currentList
                        return@onEach
                    }
                    val intermediateList = mutableListOf<AnimatedItem<T>>()
                    val allRemoved = currentList.isEmpty() && removedPositions.isNotEmpty()
                    intermediateList.addAll(currentList.mapIndexed { index, item ->
                        AnimatedItem(
                            value = item,
                            state = when {
                                insertedPositions.contains(index) ||
                                        movedPositions.find { it.second == index } != null ->
                                    AnimatedItemState.INSERTED
                                else -> AnimatedItemState.IDLE
                            }
                        )
                    })
                    removedPositions.forEach {
                        val index = Integer.max(
                            0,
                            if (it > intermediateList.size) intermediateList.size - 1 else it
                        )
                        intermediateList.add(
                            index,
                            AnimatedItem(
                                value = currentPreviousList[index],
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
                            val item = currentPreviousList[it.first]
                            intermediateList.add(
                                if (it.first > intermediateList.size) {
                                    intermediateList.size
                                } else {
                                    it.first + if (reverseLayout) 1 else -1
                                },
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
                    previousList = currentList
                }
            }
            .launchIn(scope)
    }

    fun updateList(currentList: List<AnimatedLazyListItem<T>>) {
        job?.cancel()
        job = scope.launch {
            mutex.withLock {
                val insertedPositions = mutableListOf<Int>()
                val removedPositions = mutableListOf<Int>()
                val changedPositions = mutableListOf<Int>()
                val movedPositions = mutableListOf<Pair<Int, Int>>()
                val diffResult = DiffUtil.calculateDiff(ItemsCallback(previousList, currentList))
                diffResult.dispatchUpdatesTo(
                    ListCallback(
                        insertedPositions,
                        removedPositions,
                        movedPositions,
                        changedPositions
                    )
                )
                itemsUpdateFlow.emit(
                    ItemsUpdate(
                        insertedPositions = insertedPositions,
                        removedPositions = removedPositions,
                        movedPositions = movedPositions,
                        previousList = previousList,
                        changedPositions = changedPositions,
                        currentList = currentList
                    )
                )
            }
        }
    }

    inner class ItemsCallback(
        private var previousList: List<AnimatedLazyListItem<T>>,
        private var newList: List<AnimatedLazyListItem<T>>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = previousList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return previousList[oldItemPosition].key == newList[newItemPosition].key
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return previousList[oldItemPosition].value == newList[newItemPosition].value
        }
    }

    inner class ListCallback(
        private val insertedPositions: MutableList<Int>,
        private val removedPositions: MutableList<Int>,
        private val movedPositions: MutableList<Pair<Int, Int>>,
        private val changedPositions: MutableList<Int>,
    ) : ListUpdateCallback {

        override fun onInserted(position: Int, count: Int) {
            for (i in 0 until count) {
                insertedPositions.add(position + i)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            for (i in 0 until count) {
                removedPositions.add(position + i)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            if (fromPosition == 0 && toPosition == 1 && reverseLayout) {
                movedPositions.add(toPosition to fromPosition)
            } else {
                movedPositions.add(fromPosition to toPosition)
            }
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            changedPositions.add(position)
        }
    }
}
