package com.example.travelmateapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.travelmateapp.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarMenu
import com.google.android.material.navigation.NavigationBarMenuView
import java.io.IOException
import java.io.ObjectInput
import java.lang.StringBuilder


@Suppress("DEPRECATION")
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var lastlocation:Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchBar: SearchView
    private lateinit var bottomMenu: BottomNavigationView
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var nearbyPlacesFinder: NearbyPlacesFinder
    //variable de class, équivalent à static en java
    companion object{
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        searchBar = findViewById(R.id.search_bar)
        bottomMenu = findViewById(R.id.bottom_nav)

        searchBar.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            // Recherche une ville rentrée dans la barre de recherche
            override fun onQueryTextSubmit(p0: String?): Boolean {
                mMap.clear()
                val location: String = searchBar.query.toString().trim()
                var addressList: List<Address>? = null

                if(location == "") {
                    Toast.makeText(this@MapsActivity, "Veuillez saisir une localisation s'il vous plait", Toast.LENGTH_SHORT).show()
                }
                else{
                    val geocoder = Geocoder(this@MapsActivity)
                    try{
                        addressList = geocoder.getFromLocationName(location, 1)
                    }catch (e: IOException){
                        e.printStackTrace()
                    }

                    if (addressList != null) {
                        val address =  addressList!![0]
                        latitude = address.latitude
                        longitude = address.longitude
                        val latLng:LatLng = LatLng(address.latitude, address.longitude)
                        mMap.addMarker(MarkerOptions().position(latLng).title(location))
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                    }
                }

                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }
        })

        bottomMenu.setOnItemSelectedListener {item ->
            when(item.itemId){
                R.id.restaurantbtn -> getNearbyPlaces("restaurant")
                R.id.loisirbtn -> getNearbyPlaces("hobbies")
                R.id.shopbtn -> getNearbyPlaces("store")
                R.id.parcbtn -> getNearbyPlaces("park")
                R.id.hotelbtn -> getNearbyPlaces("lodging")
            }
            true
        }

        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    //Désactive les commandes de zoom et appelle la fonction setUpMap + Change vers mode satellite lors du zoom.
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMarkerClickListener(this)
       // mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
        mMap.setOnCameraMoveListener(OnCameraMoveListener {
            val cameraPosition: CameraPosition = mMap.getCameraPosition()
            if (cameraPosition.zoom > 18.0) {
                if (mMap.getMapType() !== GoogleMap.MAP_TYPE_HYBRID) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID)
                }
            } else {
                if (mMap.getMapType() !== GoogleMap.MAP_TYPE_NORMAL) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL)
                }
            }
        })
        setUpMap()
    }

    // Vérifie si les permissions sont accordées et déplace la caméra vers la ville localisée
    private fun setUpMap(){
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)

            return
        }
        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->

            if(location != null){
                lastlocation = location
                latitude = location.latitude
                longitude = location.longitude
                val currentLatLong = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLong)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 12f))
            }
        }
    }

    //Ajoute un marqueur sur une ville donnée et lui ajoute un titre
    private fun placeMarkerOnMap(currentLatLong : LatLng){
        val markerOptions = MarkerOptions().position(currentLatLong)
        markerOptions.title("$currentLatLong")
        mMap.addMarker(markerOptions)
    }


    // Fonction qui se déclenche lors d'un clic sur un marqueur
    override fun onMarkerClick(p0: Marker): Boolean = false

    private fun getNearbyPlaces(placeType: String){
        mMap.clear()

        val stringBuilder: StringBuilder = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?")
        stringBuilder.append("location=$latitude,$longitude")
        stringBuilder.append("&radius=10000")
        if(placeType == "hobbies"){
            stringBuilder.append("&type=museum&type=zoo&type=casino&type=library&type=church&type=stadium&type=mosque")
        }
        else{
            stringBuilder.append("&type=$placeType")
        }
        stringBuilder.append("&sensor=true")
        stringBuilder.append("&key=" + resources.getString(R.string.google_maps_key))

        val url: String = stringBuilder.toString()
        nearbyPlacesFinder = NearbyPlacesFinder()
        nearbyPlacesFinder.exec(mMap, url, placeType)
    }
}

