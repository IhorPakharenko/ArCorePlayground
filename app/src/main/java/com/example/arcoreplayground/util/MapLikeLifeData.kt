package com.example.arcoreplayground.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

inline fun <T, R> Flow<T>.mapLikeLiveData(
    crossinline transform: suspend (value: T) -> R?
): Flow<R> = map(transform).filterNotNull().distinctUntilChanged()