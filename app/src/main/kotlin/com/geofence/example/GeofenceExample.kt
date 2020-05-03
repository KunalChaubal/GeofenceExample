package com.geofence.example

import android.app.Application
import com.geofence.example.ui.mainModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Application class
 */
class GeofenceExample : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(applicationContext)
            modules(listOf(mainModule))
        }
    }
}