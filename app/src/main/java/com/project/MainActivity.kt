package com.project

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.project.app.TccTwoApp
import com.project.app.configureFirebase

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureFirebase(this)

        val initialPendingUri = intent
            .takeIf { it.action == Intent.ACTION_VIEW }
            ?.data

        setContent {
            TccTwoApp(initialPendingUri = initialPendingUri)
        }
    }
}
