package com.geofence.example.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.geofence.example.R
import com.geofence.example.base.DataBindingActivity
import com.geofence.example.databinding.ActivityMainBinding
import com.geofence.example.extension.withPermission
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import org.koin.android.ext.android.inject


class MainActivity : DataBindingActivity<ActivityMainBinding>(), OnMapReadyCallback {

    companion object {
        private const val LOCATION_PERMISSION = 101
        private const val LOCATION_REQUEST_CODE = 102
        private const val GEOFENCE_LAT = 19.139941
        private const val GEOFENCE_LONG = 72.872936
        private const val GEOFENCE_RADIUS = 10000.00
        private const val CHANNEL_ID = "222"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_NAME = "PushNotificationChannel"
    }

    private val mainVM by inject<MainVM>()
    private var map: GoogleMap? = null
    private val locationRequest = LocationRequest()
    private var initiateMapZoom = true

    override fun layoutId(): Int = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb.vm = mainVM
        initObservers()
        initMap()
    }

    private fun initObservers() {
        mainVM.showNotificationEvent.observe(this, Observer {
            showNotification()
        })
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun showNotification() {
        val title = resources.getString(R.string.geofence_title)
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel: NotificationChannel?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(
            applicationContext, CHANNEL_ID
        )
            .setContentTitle(title)
            .setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        setupMap(googleMap)
        withPermission(
            LOCATION_PERMISSION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            permissionGranted = {
                showLocationDialog()
            })
    }

    private fun setupMap(googleMap: GoogleMap?) {
        map = googleMap
        map?.uiSettings?.isScrollGesturesEnabled = true
        map?.uiSettings?.setAllGesturesEnabled(true)
        map?.uiSettings?.isMyLocationButtonEnabled = false
        map?.addCircle(
            CircleOptions()
                .center(LatLng(GEOFENCE_LAT, GEOFENCE_LONG))
                .radius(GEOFENCE_RADIUS)
                .strokeColor(ContextCompat.getColor(this, R.color.colorRed))
                .fillColor(ContextCompat.getColor(this, R.color.colorPink))
        )
    }

    private fun showLocationDialog() {
        map?.isMyLocationEnabled = true
        locationRequest.interval = 5 * 1000
        locationRequest.smallestDisplacement = 10f
        locationRequest.fastestInterval = 3 * 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)

        val task = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())

        task.addOnCompleteListener {
            try {
                it.getResult(ApiException::class.java)
                // All location settings are satisfied. The client can initialize location
                // requests here.
                requestMyGpsLocation { location ->
                    if (initiateMapZoom) {
                        initiateMapZoom = false
                        map?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude), 13F
                            )
                        )
                    }
                }
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->
                        try {
                            // Cast to a resolvable exception.
                            val resolvable = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                this,
                                LOCATION_REQUEST_CODE
                            )
                        } catch (e: SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LOCATION_REQUEST_CODE -> when (resultCode) {
                Activity.RESULT_OK -> {
                    requestMyGpsLocation { location ->
                        if (initiateMapZoom) {
                            initiateMapZoom = false
                            map?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude), 13F
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestMyGpsLocation(callback: (location: Location) -> Unit) {
        val client = LocationServices.getFusedLocationProviderClient(this)
        client.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                val location = locationResult?.lastLocation
                if (location != null) {
                    callback.invoke(location)
                    mainVM.checkForGeoFenceEntry(
                        location,
                        GEOFENCE_LAT,
                        GEOFENCE_LONG,
                        GEOFENCE_RADIUS
                    )
                }
            }
        }, null)
    }
}