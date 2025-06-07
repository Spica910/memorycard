package com.example.mymaterialapp

import android.app.Application
import com.example.mymaterialapp.db.AppDatabase

class MainApplication : Application() {
    // Using by lazy so the database is only created when it's first needed
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
