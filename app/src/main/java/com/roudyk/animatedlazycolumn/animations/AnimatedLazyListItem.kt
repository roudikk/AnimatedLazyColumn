package com.roudyk.animatedlazycolumn.animations

import androidx.compose.runtime.Composable

data class AnimatedLazyListItem<out T>(
    val key: String,
    val value: T? = null,
    val composable: @Composable () -> Unit
)