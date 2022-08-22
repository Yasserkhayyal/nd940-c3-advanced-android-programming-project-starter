package com.udacity.models

sealed class ButtonState {
    object Clicked : ButtonState()
    object Loading : ButtonState()
    object Completed : ButtonState()
    object Failed : ButtonState()
    object NetworkUnavailable : ButtonState()
}