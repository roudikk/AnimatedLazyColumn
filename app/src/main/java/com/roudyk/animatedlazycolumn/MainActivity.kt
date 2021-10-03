package com.roudyk.animatedlazycolumn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roudyk.animatedlazycolumn.animations.widgets.AnimatedLazyColumn
import com.roudyk.animatedlazycolumn.animations.AnimatedLazyListItem
import com.roudyk.animatedlazycolumn.animations.widgets.AnimatedLazyRow
import com.roudyk.animatedlazycolumn.ui.theme.AnimatedLazyColumnTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = viewModel<MainViewModel>()
            AnimatedLazyColumnTheme {
                var reverseLayout by remember { mutableStateOf(false) }
                val columnState = rememberLazyListState()
                val topRowState = rememberLazyListState()
                val rowState = rememberLazyListState()
                val items by viewModel.state.collectAsState(emptyList())
                Surface {
                    Column {
                        AnimatedLazyRow(
                            state = topRowState,
                            contentPadding = PaddingValues(start = 16.dp, top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            items = buildList {
                                add(AnimatedLazyListItem(
                                    key = "add-item",
                                    value = "add-item"
                                ) {
                                    Button(onClick = { viewModel.addItem() }) {
                                        Text(text = "Add Item")
                                    }
                                })
                                add(AnimatedLazyListItem(
                                    key = "add-item-end",
                                    value = "add-item-end"
                                ) {
                                    Button(onClick = { viewModel.addItemEnd() }) {
                                        Text(text = "Add Item End")
                                    }
                                })
                                add(AnimatedLazyListItem(
                                    key = "add-random-item",
                                    value = "add-random-item"
                                ) {
                                    Button(onClick = { viewModel.addRandomItem() }) {
                                        Text(text = "Add random item")
                                    }
                                })
                                add(AnimatedLazyListItem(
                                    key = "remove-random-item",
                                    value = "remove-random-item"
                                ) {
                                    Button(onClick = { viewModel.removeRandomItem() }) {
                                        Text(text = "Remove random Item")
                                    }
                                })
                                add(AnimatedLazyListItem(
                                    key = "clear",
                                    value = "clear"
                                ) {
                                    Button(onClick = { viewModel.clearItems() }) {
                                        Text(text = "Clear")
                                    }
                                })
                                add(AnimatedLazyListItem(
                                    key = "reverse-layout",
                                    value = "reverse-layout"
                                ) {
                                    Button(onClick = { reverseLayout = !reverseLayout }) {
                                        Text(text = "Reverse")
                                    }
                                })
                            }
                        )
                        AnimatedLazyRow(
                            state = rowState,
                            modifier = Modifier,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp),
                            reverseLayout = reverseLayout,
                            items = items.map {
                                AnimatedLazyListItem(key = it.id, value = it.text) {
                                    TextItem(viewModel, it, Modifier.width(300.dp))
                                }
                            }
                        )
                        AnimatedLazyColumn(
                            state = columnState,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp),
                            reverseLayout = reverseLayout,
                            items = items.map {
                                AnimatedLazyListItem(key = it.id, value = it.text) {
                                    TextItem(viewModel, it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextItem(viewModel: MainViewModel, item: MainItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = 0.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            var textValue by remember { mutableStateOf("") }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = item.text
                )
                Row {
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            ),
                        value = textValue,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        onValueChange = { textValue = it },
                        label = { Text(text = "Move to") },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.moveToPosition(item, textValue) }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Move"
                                )
                            }
                        }
                    )
                }
            }
            IconButton(
                modifier = Modifier.padding(16.dp),
                onClick = { viewModel.removeItem(item) }
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}