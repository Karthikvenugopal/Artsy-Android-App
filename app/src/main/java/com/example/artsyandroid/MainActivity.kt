package com.example.artsyandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.artsyandroid.network.RetrofitInstance
import com.example.artsyandroid.ui.theme.ArtsyAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ArtsyAndroid)
        super.onCreate(savedInstanceState)

        RetrofitInstance.init(this)

        setContent {
            ArtsyAndroidTheme {
                MyApp()
            }
        }
    }
}