package com.example.texasholdem

interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
}