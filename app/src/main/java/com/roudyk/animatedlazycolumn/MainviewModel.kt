package com.roudyk.animatedlazycolumn

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

data class MainItem(
    val id: String,
    val text: String
)

class MainViewModel : ViewModel() {

    private var items = mutableListOf<MainItem>()
    val state = MutableStateFlow(emptyList<MainItem>())

    fun addItem(index: Int = 0) {
        val newItems = mutableListOf<MainItem>()
        newItems.addAll(items)
        newItems.add(
            index, MainItem(
                id = UUID.randomUUID().toString(),
                text = "Item: ${newItems.size}"
            )
        )
        items = newItems
        state.tryEmit(newItems)
    }

    fun addItemEnd() {
        addItem(items.size)
    }

    fun addRandomItem() {
        if (items.size == 0) {
            addItem()
        } else {
            addItem(index = Random().nextInt(items.size))
        }
    }

    fun removeItem(item: MainItem) {
        val newItems = mutableListOf<MainItem>()
        newItems.addAll(items)
        newItems.removeAll { it.id == item.id }
        items = newItems
        state.tryEmit(newItems)
    }

    fun removeRandomItem() {
        if (items.size == 0) {
            return
        } else {
            removeItem(items[Random().nextInt(items.size)])
        }
    }

    fun moveToPosition(item: MainItem, newPositionText: String) {
        val newPosition = newPositionText.toIntOrNull() ?: return
        if (newPosition < 0 || newPosition > items.size) return
        val newItems = mutableListOf<MainItem>()
        val currentPosition = items.indexOf(item)
        newItems.addAll(items)
        newItems.removeAt(currentPosition)
        newItems.add(newPosition, item)
        items = newItems
        state.tryEmit(newItems)
    }

    fun clearItems() {
        val newItems = mutableListOf<MainItem>()
        items = newItems
        state.tryEmit(newItems)
    }
}