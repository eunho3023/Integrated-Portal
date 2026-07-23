package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            Log.d("MainApplication", "Firebase initialized successfully in Application class")
        } catch (e: Exception) {
            Log.e("MainApplication", "Failed to initialize Firebase in Application class", e)
        }
    }
}
