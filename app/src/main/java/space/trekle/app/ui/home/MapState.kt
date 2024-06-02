package space.trekle.app.ui.home

sealed class MapState {
    object Idle : MapState()
    object Routing : MapState()
}