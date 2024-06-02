package space.trekle.app.ui.home

import space.trekle.app.services.routing.RouteService

class MapStateMachine(routeService: RouteService, openBottomSheetPoi: (MapEvent) -> Unit) {

    private val routeService: RouteService = routeService
    private val openBottomSheetPoi: (MapEvent) -> Unit = openBottomSheetPoi
    var currentState: MapState = MapState.Idle
        private set

    fun SetState(state: MapState) {
        currentState = state
    }
    fun handleEvent(event: MapEvent) {
        currentState = when (currentState) {
            is MapState.Idle -> handleIdleState(event)
            is MapState.Routing -> handleRoutingState(event)
        }
    }

    private fun handleIdleState(event: MapEvent): MapState {
        return when (event) {
            is MapEvent.LongClick -> {
                // Show bottom sheet with options to start routing
                openBottomSheetPoi(event)
                MapState.Routing
            }
            else -> MapState.Idle
        }
    }

    private fun handleRoutingState(event: MapEvent): MapState {
        return when (event) {
            is MapEvent.Click -> {
                // Add point to the route
                routeService.addPoint(doubleArrayOf(event.point.latitude, event.point.longitude))
                MapState.Routing
            }
            else -> MapState.Routing
        }
    }


}