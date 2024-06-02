package space.trekle.app.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.permissions.PermissionsListener
import org.maplibre.android.location.permissions.PermissionsManager
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.utils.PolylineUtils
import space.trekle.app.R
import space.trekle.app.databinding.FragmentHomeBinding
import space.trekle.app.services.routing.RouteListener
import space.trekle.app.services.routing.RouteResponse
import space.trekle.app.services.routing.RouteService
import space.trekle.app.ui.modal.BottomSheetRouting


class MapFragment : Fragment(), OnMapReadyCallback, PermissionsListener, RouteListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    private val ROUTING_LAYER = "route-layer"
    private val ROUTING_SOURCE = "route-source"
    private val ROUTING_COLOR = "#6200EA"

    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private lateinit var mapView: MapView
    private lateinit var mapLibreMap: MapLibreMap
    private lateinit var buttonChangeStyle: Button
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var routeService: RouteService

    private lateinit var stateMachine : MapStateMachine
    private var locationPermissionGranted = false

    private var fusedLocationClient: FusedLocationProviderClient? = null

    private lateinit var root : View

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        routeService = RouteService()
        routeService.addListener(this)

        MapLibre.getInstance(requireContext())
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        root= binding.root

        buttonChangeStyle = root.findViewById(R.id.button_change_style)

        buttonChangeStyle.setOnClickListener {
            mapView.getMapAsync { map ->
                map.getStyle { style ->
                    openBottomSheet(style)
                }
            }
        }

        mapView = root.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        getLocationPermission();

        return root
    }

    override fun onMapReady(mapLibreMap: MapLibreMap) {
        this.mapLibreMap = mapLibreMap
        mapLibreMap.setStyle(
            Style.Builder().fromUri("https://gist.githubusercontent.com/gnicod/1436a2d32d28e810715078a0ea986313/raw/39545f2b8308c134572354f7688de78dfaef5b00/trekle-style.json")
        ) { style ->
            enableLocationComponent(style)
        }

        stateMachine = MapStateMachine(routeService, ::openBottomSheetPoi)


        mapLibreMap.addOnMapLongClickListener { point->
            stateMachine.handleEvent(MapEvent.LongClick(point))
            return@addOnMapLongClickListener false
        }

        mapLibreMap.addOnMapClickListener { point ->
            stateMachine.handleEvent(MapEvent.Click(point))
            return@addOnMapClickListener false
        }

        getDeviceLocation()
    }

    /**
     * Open a bottom sheet to show the POI options
     */
    private fun openBottomSheetPoi(event: MapEvent) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_modal_poi, null)
        val gotoButton = view.findViewById<Button>(R.id.button_routing_goto)
        gotoButton.setOnClickListener(View.OnClickListener {
            // get current position
            Log.d("Trekle.HomeFragment", "Routing to ${event.latLng}")
            mapLibreMap.locationComponent.lastKnownLocation?.let {
                routeService.addPoint(doubleArrayOf(it.latitude, it.longitude))
                routeService.addPoint(doubleArrayOf(event.latLng.latitude, event.latLng.longitude))
            }
            dialog.dismiss()
        })
        dialog.setContentView(view)
        dialog.show()
    }


    private fun openBottomSheetRouting(route: RouteResponse) {
        val composeView = root.findViewById<ComposeView>(R.id.composeView)
        composeView.setContent {
            BottomSheetRouting(route, removeRoute = ::removeRoute, cancelPrevious = ::cancelPrevious)
        }
    }

    private fun cancelPrevious() {
        mapLibreMap.clear()
        routeService.removeLastPoint()
    }

    private fun removeRoute() {
        routeService.clearPoints()
        stateMachine.SetState(MapState.Idle)
        mapLibreMap.style?.removeLayer(ROUTING_LAYER)
        mapLibreMap.style?.removeSource(ROUTING_SOURCE)
        mapLibreMap.clear()
    }

    private fun getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                fusedLocationClient?.lastLocation?.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        moveCameraToLocation(LatLng(location.latitude, location.longitude))
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }


    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {

            // Get an instance of the component
            val locationComponent = mapLibreMap.locationComponent

            // Activate with a built LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(requireContext(), loadedMapStyle).build()
            )

            locationComponent.isLocationComponentEnabled = true
            // Set the component's camera mode
            locationComponent.cameraMode = org.maplibre.android.location.modes.CameraMode.TRACKING

            // Set the component's render mode
            locationComponent.renderMode = org.maplibre.android.location.modes.RenderMode.COMPASS
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(requireActivity())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        // Present toast or dialog
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapView.getMapAsync { map -> map.getStyle { style -> enableLocationComponent(style) } }
        } else {
            // Permission not granted
        }
    }

    private fun openBottomSheetPointSelector() {
        // Implement this method to open a bottom sheet to select a point

    }


    private fun openBottomSheet(style: Style) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_layers_selector_layout, null)
        dialog.setContentView(view)

        val mapStyles = listOf(
            "Satellite" to style.getLayer("satelite"),
            "Topographic" to style.getLayer("topo"),
            "Wikimedia" to style.getLayer("wikimedia"),
        )

        val listView: ListView = view.findViewById(R.id.style_list_view)
        val styleNames = mapStyles.map { it.first }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, styleNames)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedStyle = mapStyles[position]
            for (style in mapStyles) {
                style.second?.setProperties(
                    PropertyFactory.visibility(Property.NONE)
                )
            }
            mapView.getMapAsync { map ->
                selectedStyle.second?.setProperties(
                    PropertyFactory.visibility(Property.VISIBLE)
                )
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onPointsChanged(points: List<DoubleArray>) {
        for (point in points) {
            val markerOptions = MarkerOptions()
                .position(LatLng(point[0], point[1]))
            mapLibreMap.addMarker(markerOptions)
        }
    }


    fun initRouteSource(style: Style, lineString: String) {
        val geoJsonSource = GeoJsonSource(ROUTING_SOURCE, lineString)
        // check if the source is already added to the style

        val source = style.getSourceAs<GeoJsonSource>(ROUTING_SOURCE)
        if (source != null) {
            source.setGeoJson(lineString)
            return
        }
        style.addSource(geoJsonSource)
        val lineLayer = LineLayer(ROUTING_LAYER, ROUTING_SOURCE).apply {
            setProperties(
                PropertyFactory.lineColor(Color.parseColor(ROUTING_COLOR)),
                PropertyFactory.lineWidth(5f)
            )
        }
        style.addLayer(lineLayer)
    }

    private fun moveCameraToLocation(location: LatLng) {
        mapLibreMap.animateCamera(
            newLatLngZoom(
                location, 15.0
            ), 1000
        )
    }

    override fun onRouteChanged(route: RouteResponse?) {
        Log.d("Trekle.HomeFragment", "Route changed: ${route?.trip?.summary}")

        val coordinates = mutableListOf<List<Double>>()
        for (leg in route?.trip?.legs!!) {
            val shape = leg.shape
            PolylineUtils.decode(shape, 6).let { decodedPath ->
                coordinates.addAll(decodedPath.map { listOf(it.longitude(), it.latitude()) })
            }
        }
        val lineString = """
            {
                "type": "Feature",
                "geometry": {
                    "type": "LineString",
                    "coordinates": $coordinates
                },
                "properties": {}
            }
        """.trimIndent()
        activity?.runOnUiThread {
            mapLibreMap.style?.let { initRouteSource(it, lineString) }
            openBottomSheetRouting(route)
        }
    }
}