package com.example.artsyandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ArtsyAndroid)  // note the underscore in generated R.style
        super.onCreate(savedInstanceState)
        setContent { MyApp() }
    }
}
