package io.github.turskyi.odometer

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.core.content.ContextCompat

class OdometerService : Service() {

    companion object {
        private var distanceInMeters = 0.0
        private var lastLocation: Location? = null
        const val PERMISSION_STRING = Manifest.permission.ACCESS_FINE_LOCATION
    }

    private val binder: IBinder = OdometerBinder()
    private var listener: LocationListener? = null
    private var locManager: LocationManager? = null

    inner class OdometerBinder : Binder() {
        val odometer: OdometerService
            get() = this@OdometerService
    }

    override fun onCreate() {
        super.onCreate()
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (lastLocation == null) {
                    lastLocation = location
                }
                distanceInMeters += location.distanceTo(lastLocation).toDouble()
                lastLocation = location
            }

            override fun onProviderDisabled(arg0: String) {}
            override fun onProviderEnabled(arg0: String) {}
            override fun onStatusChanged(arg0: String, arg1: Int, bundle: Bundle) {}
        }
        locManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val provider = locManager?.getBestProvider(Criteria(), true)
            if (provider != null) {
                /* The requestLocationUpdates() method takes four parameters: a GPS provider,
                 the minimum time interval between location updates in milliseconds,
                 the minimum distance between location updates in
                 meters, and a LocationListener. */
                locManager?.requestLocationUpdates(
                    provider,
                    1000,
                    1f,
                    listener as LocationListener,
                )
            }
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        if (locManager != null && listener != null) {
            if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING)
                == PackageManager.PERMISSION_GRANTED
            ) {
                locManager?.removeUpdates(listener!!)
            }
            locManager = null
            listener = null
        }
    }

    val distanceInMiles: Double
        get() = distanceInMeters / 1609.344

    val distance: Double
        get() = distanceInMeters
}
