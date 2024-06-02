package space.trekle.app.ui.home

import org.maplibre.android.geometry.LatLng

sealed class MapEvent {
    data class LongClick(val point: LatLng) : MapEvent()
    data class Click(val point: LatLng) : MapEvent()
    val latLng: LatLng
        get() = when (this) {
            is LongClick -> point
            is Click -> point
        }
}