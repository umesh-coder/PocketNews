package com.example.pocketnews.util

import kotlinx.coroutines.CoroutineScope

fun <T> debounce(
    delayMillis: Long = 800L,
    scope: CoroutineScope,
    action: (T) -> Unit
): (T) -> Unit {
    return {}
}