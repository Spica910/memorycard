package com.example.myandroidapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.myandroidapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example: Set text on a TextView that would be in your layout
        // binding.textView.text = "Hello from Kotlin!"
    }
}
