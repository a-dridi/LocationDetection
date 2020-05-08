package com.example.locationdetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Float

private const val PERMISSION_REQUEST_CODE = 10

class MainActivity : AppCompatActivity(),
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    OnMapReadyCallback {
    val API_CONNECTION_LOG_TAG = "GoogleAPIConnection"
    val GPS_LOG_TAG = "GPSDection"

    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var gpsLocation: Location
    private var locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    lateinit var gpsLocationManager: LocationManager
    private lateinit var gpsGoogleMap: GoogleMap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadGoogleApiClient()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkLocationDetectionPermission(locationPermissions)) {
                requestAndLoadGpsLocation()

            } else {
                requestPermissions(locationPermissions, PERMISSION_REQUEST_CODE)
            }
        } else {
            requestAndLoadGpsLocation()
        }

        setContentView(R.layout.activity_main)

        val gpsMapFragment = supportFragmentManager
            .findFragmentById(R.id.gpsMapView) as SupportMapFragment
        gpsMapFragment.getMapAsync(this)

    }

    private fun loadGoogleApiClient() {
        googleApiClient =
            GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(LocationServices.API).build()
        googleApiClient!!.connect()
    }

    private fun checkLocationDetectionPermission(permissionArray: Array<String>): Boolean {
        var permissionAllSuccess = true
        for (i in permissionArray.indices) {
            if (checkCallingOrSelfPermission(permissionArray[i]) == PackageManager.PERMISSION_DENIED)
                permissionAllSuccess = false
        }
        return permissionAllSuccess
    }

    /**
     * Creates Location Request and Loads Location
     */
    @SuppressLint("MissingPermission")
    fun requestAndLoadGpsLocation() {
        //If user did activate GPS, then create the locationManager that will be used to automatically detect changes in the GPS location and display the new location
        if (checkGpsActivated()) {
            try {
                gpsLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0F,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location?) {
                            if (location != null) {
                                setNewLocation(location)
                                Log.d(
                                    GPS_LOG_TAG,
                                    "New GPS Latitude : " + location!!.latitude
                                )
                                Log.d(
                                    GPS_LOG_TAG,
                                    "New GPS Longitude : " + location!!.longitude
                                )
                            }
                        }

                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: Bundle?
                        ) {

                        }

                        override fun onProviderEnabled(provider: String?) {

                        }

                        override fun onProviderDisabled(provider: String?) {

                        }

                    })

                //Load the GPS location
                gpsLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val gpsFusedLocationClient: FusedLocationProviderClient =
                    LocationServices.getFusedLocationProviderClient(this)

                gpsFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(object : OnSuccessListener<Location?> {
                        override fun onSuccess(location: Location?) {
                            if (location != null) {
                                Log.d(GPS_LOG_TAG, "Location was detected")
                                Log.d(
                                    GPS_LOG_TAG,
                                    "Lat " + location.latitude + "  Long " + location.longitude
                                )
                                setNewLocation(location)
                            } else {
                                Log.d(GPS_LOG_TAG, "Location was not detected")
                            }
                        }

                    })
                    .addOnFailureListener(object : OnFailureListener {
                        override fun onFailure(e: Exception) {
                            Log.d(GPS_LOG_TAG, "Error: Trying to get last GPS location")
                            e.printStackTrace()
                        }

                    })
            } catch (e: SecurityException) {
                requestPermissions(locationPermissions, PERMISSION_REQUEST_CODE)
            }
        } else {
            errorLocationDetectionDeactivated(this)
        }
    }

    /**
     * Checking if: User activated (turned on) GPS and network functionality
     */
    fun checkGpsActivated(): Boolean {
        gpsLocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }


    fun errorLocationDetectionDeactivated(activity: MainActivity) {
        val locationDetectionDeactivatedDialog = AlertDialog.Builder(activity)
        locationDetectionDeactivatedDialog.setTitle(getString(R.string.errorLocationDetectionDeactivatedTitle))
            .setMessage(getString(R.string.errorLocationDetectionDeactivatedMesssage))
            .setPositiveButton(getString(R.string.errorLocationDetectionDeactivatedOKButton)) { paramDialogInterface, paramInt ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        locationDetectionDeactivatedDialog.show()
    }

    /**
     * Update marker to new location
     */
    fun setNewLocation(
        location: Location
    ) {
        if (::gpsGoogleMap.isInitialized && gpsGoogleMap != null) {
            val gpsPositionLatLng = LatLng(location.latitude, location.longitude)
            latitudeValue.setText(String.format("%.5f", location.latitude))
            longitudeValue.setText(String.format("%.5f", location.longitude))

            gpsGoogleMap.clear()
            gpsGoogleMap.addMarker(
                MarkerOptions().position(gpsPositionLatLng)
                    .title(getString(R.string.my_position_text) + gpsPositionLatLng.latitude + " - " + gpsPositionLatLng.longitude)
            )

            gpsGoogleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    gpsPositionLatLng,
                    20F
                )
            )

        } else {
            Log.d("MapDisplay", "Map could not be loaded")
            errorMapCouldNotBeLoaded(this)
        }
    }

    fun errorMapCouldNotBeLoaded(activity: MainActivity) {
        val errorMapCouldNotBeLoadedDialog = AlertDialog.Builder(activity)
        errorMapCouldNotBeLoadedDialog.setTitle(getString(R.string.errorMapCouldNotBeLoadedTitle))
            .setMessage(getString(R.string.errorMapCouldNotBeLoadedMessage))
            .setPositiveButton(getString(R.string.errorMapCouldNotBeLoadedOKButton)) { paramDialogInterface, paramInt ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        errorMapCouldNotBeLoadedDialog.show()
    }

    /**
     * The permission request for the location detection was displayed. Check location only if user did grant permission
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestAndLoadGpsLocation()
            return
        }
        //Display error if permission was not granted or the gps is deactivated
        fun errorLocationDetectionDeactivated(activity: MainActivity) {
            val locationDetectionDeactivatedDialog = AlertDialog.Builder(activity)
            locationDetectionDeactivatedDialog.setTitle(getString(R.string.errorLocationDetectionDeactivatedTitle))
                .setMessage(getString(R.string.errorLocationDetectionDeactivatedMesssage))
                .setPositiveButton(getString(R.string.errorLocationDetectionDeactivatedOKButton)) { paramDialogInterface, paramInt ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            locationDetectionDeactivatedDialog.show()
        }

    }

    override fun onConnected(p0: Bundle?) {
        Log.i(API_CONNECTION_LOG_TAG, "Location services connection enabled")
        //Save GPS detection in variable
        //Display location in Google Maps Marker
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.i(API_CONNECTION_LOG_TAG, "Location services disconnect. Please reconnect")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.i(API_CONNECTION_LOG_TAG, "Location services connection failed. Please reconnect")
        val connectionFailedDialog = AlertDialog.Builder(this)
        connectionFailedDialog.setTitle(getString(R.string.googleApiConnectionFailedErrorTitle))
            .setMessage(getString(R.string.googleApiConnectionFailedErrorMessage))
            .setPositiveButton(getString(R.string.googleApiConnectionFailedErrorOKButton)) { paramDialogInterface, paramInt ->
            }
        connectionFailedDialog.show()
    }


    override fun onResume() {
        super.onResume()
        loadGoogleApiClient()
        requestAndLoadGpsLocation()
    }

    override fun onPause() {
        super.onPause()
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect()
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        if (map != null) {
            loadMap(map)
        } else {
            errorMapCouldNotBeLoaded(this)
        }
    }

    /**
     * Load map with card topography (card version - normal version)
     */
    fun loadMap(map: GoogleMap) {
        gpsGoogleMap = map
        gpsGoogleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

}
