package com.roudyk.animatedlazycolumn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import com.roudyk.animatedlazycolumn.ui.theme.AnimatedLazyColumnTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = viewModel<MainViewModel>()
            AnimatedLazyColumnTheme {
                val state = rememberLazyListState()
                val items by viewModel.state.collectAsState(emptyList())
                Surface {
                    Column {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(onClick = { viewModel.addItem() }) {
                                Text(text = "Add Item")
                            }
                            Button(onClick = { viewModel.addItemEnd() }) {
                                Text(text = "Add Item End")
                            }
                            Button(onClick = { viewModel.addRandomItem() }) {
                                Text(text = "Add random item")
                            }
                            Button(onClick = { viewModel.removeRandomItem() }) {
                                Text(text = "Remove random Item")
                            }
                            Button(onClick = { viewModel.clearItems() }) {
                                Text(text = "Clear")
                            }
                        }
                        AnimatedLazyColumn(
                            state = state,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp),
                            reverseLayout = false,
                            items = items.map {
                                KeyItem(key = it.id, value = it.text) {
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
private fun TextItem(viewModel: MainViewModel, item: MainItem) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
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
                        label = { Text(text = "Move to position") },
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