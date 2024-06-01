package space.trekle.app.services.routing

import android.util.Log
import org.json.JSONObject


interface RouteListener {
    fun onPointsChanged(points: List<DoubleArray>)
    fun onRouteChanged(route: RouteResponse?) // Define Route as per your needs
}

class RouteService(autoCalcRoute: Boolean=false) {

    private val points = mutableListOf<DoubleArray>()
    private val listeners = mutableListOf<RouteListener>()
    private val client = RoutingClient()

    fun addListener(listener: RouteListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: RouteListener) {
        listeners.remove(listener)
    }

    fun addPoint(point: DoubleArray) {
        points!!.add(point)
        for (listener in listeners) {
            listener.onPointsChanged(points)
        }
        if (points.size > 1 ){
            calculateRoute()
        }
    }

    fun removePoint(point: DoubleArray) {
        points!!.remove(point)
    }

    fun getPoints(): List<DoubleArray>? {
        return points
    }

    fun clearPoints() {
        points!!.clear()
    }

    fun calculateRoute() {
        Log.d("RouteService", "Calculating route... for points: $points")
        client.route(points, "pedestrian", false) { routeResponse, throwable ->
            if (routeResponse != null) {
                Log.d("RouteService", "Route calculated: $routeResponse")
                for (listener in listeners) {
                    listener.onRouteChanged(routeResponse)
                }
            } else {
                Log.e("RouteService", "Route calculation failed: $throwable")
            }
        }
    }
}