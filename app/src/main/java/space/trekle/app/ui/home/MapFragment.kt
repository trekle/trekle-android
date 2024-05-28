package space.trekle.app.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.maplibre.android.MapLibre
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.permissions.PermissionsListener
import org.maplibre.android.location.permissions.PermissionsManager
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import space.trekle.app.R
import space.trekle.app.databinding.FragmentHomeBinding
import space.trekle.app.services.routing.RouteListener
import space.trekle.app.services.routing.RouteResponse
import space.trekle.app.services.routing.RouteService

class MapFragment : Fragment(), OnMapReadyCallback, PermissionsListener, RouteListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private lateinit var mapLibreMap: MapLibreMap
    private lateinit var buttonChangeStyle: Button
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var routeService: RouteService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        routeService = RouteService()
        routeService.addListener(this)

        MapLibre.getInstance(requireContext())
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

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

        return root
    }

    override fun onMapReady(mapLibreMap: MapLibreMap) {
        this.mapLibreMap = mapLibreMap
        mapLibreMap.setStyle(
            Style.Builder().fromUri("https://gist.githubusercontent.com/gnicod/1436a2d32d28e810715078a0ea986313/raw/39545f2b8308c134572354f7688de78dfaef5b00/trekle-style.json")
        ) { style ->
            enableLocationComponent(style)
        }

        mapLibreMap.addOnMapLongClickListener {
            // Handle map long click events
            Toast.makeText(requireContext(), "Map long click at: $it", Toast.LENGTH_SHORT).show()
            Log.d("TrekleApp.map", "Map long click at: $it")
            routeService.addPoint(doubleArrayOf(it.longitude, it.latitude))
            false
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
        TODO("Not yet implemented")
    }

    override fun onRouteChanged(route: RouteResponse?) {
        Log.d("Trekle.HomeFragment", "Route changed: ${route?.trip?.summary}")
    }
}