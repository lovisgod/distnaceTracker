package com.lovisgod.distancetracker

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.lovisgod.distancetracker.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.view.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var lastLocation: Location
    private var oldLocation = Location(LocationManager.NETWORK_PROVIDER)
    var distanceCovered: Float = 0.toFloat()
    //Receiving Location Updates
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    private lateinit var bind: ActivityMainBinding
    var isPlaying = false


    companion object {

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        private const val REQUEST_CHECK_SETTINGS = 2

        private const val PLACE_PICKER_REQUEST = 3

    }

    private lateinit var mapFragment: SupportMapFragment

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        bind = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        // set up fuselocation client
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                lastLocation = p0.lastLocation
                //Calculate covered DIstance
                val distance: Float = oldLocation.distanceTo(lastLocation)
                distanceCovered = distanceCovered.plus(distance)
                oldLocation = lastLocation
            }
        }

        createLocationRequest()


            bind.fab.icon = getDrawable(R.drawable.ic_start_24dp)
        bind.fab.setOnClickListener {
            if (!isPlaying) {
                bind.timer.base = (SystemClock.elapsedRealtime() - 0)
                bind.timer.start()
                isPlaying = true
                bind.fab.setText("Stop")
                bind.fab.icon = getDrawable(R.drawable.ic_stop_24dp)
            } else {
                bind.timer.stop()
                stopTimer()
                isPlaying = false
                bind.fab.setText("Start")
                    bind.fab.icon = getDrawable(R.drawable.ic_start_24dp)
            }
        }
    }

    override fun onMapReady(p0: GoogleMap?) {
        mMap = p0!!
        //enable the zoom in/zoom out interface on the map
        mMap.uiSettings.isZoomControlsEnabled = true
        //Check Permission to access User location: ACCESS_FINE_LOCATION
        setUpMap()
        //animateZoom to User location
        usersLocation()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        //Before you start requesting for location updates,
        //you need to check the state of the user’s location settings.
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }

        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        this,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    //Receiving Location Updates: From here to onResume + onCreate method
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    // check location permission
    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
    }

    private fun usersLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            //set my location enabled and draw an indication of my current location
            mMap.isMyLocationEnabled = true

            //The Android Maps API provides different map types: MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID
            mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN

            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                // Got last known location. In some rare situations this can be null. so, if it is not null, zoom camera to the location
                location?.let {
                    lastLocation = it
                    val currentLatLng = LatLng(it.latitude, it.longitude)
//                    mMap.addMarker(MarkerOptions().position(currentLatLng).title("present location"))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }

        }

    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }

        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val place = PlacePicker.getPlace(this, data)
            }
        }
    }


    private fun stopTimer(){

        fusedLocationClient.removeLocationUpdates(locationCallback)
        val bottom = DistanceBottomDialog.newInstance(distanceCovered,bind.timer.text.toString())
        bottom?.show(supportFragmentManager.beginTransaction(), "dialog_playback")

    }


}
