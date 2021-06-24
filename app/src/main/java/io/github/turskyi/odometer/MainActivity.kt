package io.github.turskyi.odometer

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.turskyi.odometer.OdometerService.OdometerBinder
import java.util.*

class MainActivity : Activity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 698
        private const val NOTIFICATION_ID = 423
        const val CHANNEL_ID = "CHANNEL_ID"
    }

    private var odometer: OdometerService? = null
    private var bound = false

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            val odometerBinder: OdometerBinder = binder as OdometerBinder
            odometer = odometerBinder.odometer
            bound = true
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        displayDistance()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(this, OdometerService::class.java)
                    bindService(intent, connection, BIND_AUTO_CREATE)
                } else {
                    createNotificationChannel()
                    /* Create a notification builder */
                    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_menu_compass)
                        .setContentTitle(resources.getString(R.string.app_name))
                        .setContentText(resources.getString(R.string.permission_denied))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(longArrayOf(1000, 1000))
                        .setAutoCancel(true)
                    /* Create an action */
                    val actionIntent = Intent(this, MainActivity::class.java)
                    val actionPendingIntent = PendingIntent.getActivity(
                        this, 0,
                        actionIntent, PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    builder.setContentIntent(actionPendingIntent)
                    /* Issue the notification */
                    val notificationManager =
                        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(
                this,
                OdometerService.PERMISSION_STRING
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(OdometerService.PERMISSION_STRING),
                PERMISSION_REQUEST_CODE
            )
        } else {
            val intent = Intent(this, OdometerService::class.java)
            /* The code Context.BIND_AUTO_CREATE tells Android to create the service
             if it doesn’t already exist. */
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    private fun displayDistance() {
        val distanceInMilesView = findViewById<View>(R.id.distanceInMiles) as TextView
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                var distanceInMiles = 0.0
                if (bound && odometer != null) {
                    distanceInMiles = odometer!!.distanceInMiles
                }
                val distanceStr: String = String.format(
                    Locale.getDefault(),
                    "%1$,.2f miles", distanceInMiles
                )
                distanceInMilesView.text = distanceStr
                /* While MainActivity is running, the displayDistance() method calls
                the OdometerService getMiles() method every second and updates the screen. */
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun createNotificationChannel() {
        /* Create the NotificationChannel, but only on API 26+ because
         the NotificationChannel class is new and not in the support library */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
