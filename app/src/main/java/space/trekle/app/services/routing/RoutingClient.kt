package space.trekle.app.services.routing
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class RoutingClient(
    private val baseUrl: String = "https://trekle.space",
    private val client: OkHttpClient = OkHttpClient()
) {

    private val gson = Gson()

    fun route(locations: List<DoubleArray>, costing: String, narrative: Boolean, callback: (RouteResponse?, Throwable?) -> Unit) {
        val url = "$baseUrl/routing/route"
        val json = gson.toJson(RouteRequest(locations, costing, narrative))

        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("Content-Type", "application/json")
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(null, IOException("Unexpected code $response"))
                        return
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val routeResponse = gson.fromJson(responseBody, RouteResponse::class.java)
                        callback(routeResponse, null)
                    } else {
                        callback(null, IOException("Empty response"))
                    }
                }
            }
        })
    }

    private fun RouteRequest(locations: List<DoubleArray>, costing: String, narrative: Boolean): RoutingClient.RouteRequest {
        val locationList = locations.map { Location(it[0], it[1]) }
        return RouteRequest(locationList, costing, narrative)
    }

    data class Location(val lat: Double, val lon: Double)
    data class RouteRequest(val locations: List<Location>, val costing: String, val narrative: Boolean)
}
