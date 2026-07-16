package com.example.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NotificationEventBus {
    data class NotificationEvent(
        val message: String,
        val transactionId: Long
    )

    private val _events = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<NotificationEvent> = _events.asSharedFlow()

    fun emitEvent(message: String, transactionId: Long) {
        _events.tryEmit(NotificationEvent(message, transactionId))
    }
}
