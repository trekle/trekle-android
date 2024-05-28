package space.trekle.app.services.routing

data class RouteResponse(
    val trip: Trip,
    val status_message: String,
    val status: Int,
    val units: String,
    val language: String
)

data class Trip(
    val locations: List<Location>,
    val legs: List<Leg>,
    val summary: Summary
)

data class Location(
    val type: String,
    val lat: Double,
    val lon: Double,
    val original_index: Int,
    val side_of_street: String? = null
)

data class Leg(
    val summary: Summary,
    val shape: String
)

data class Summary(
    val has_time_restrictions: Boolean,
    val has_toll: Boolean,
    val has_highway: Boolean,
    val has_ferry: Boolean,
    val min_lat: Double,
    val min_lon: Double,
    val max_lat: Double,
    val max_lon: Double,
    val time: Double,
    val length: Double,
    val cost: Double
)
