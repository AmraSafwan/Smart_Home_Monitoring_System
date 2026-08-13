package com.example.smarthome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.smarthome.ui.navigation.AppNavigation
import com.example.smarthome.ui.theme.SmartHomeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            SmartHomeTheme {

                AppNavigation()
            }
        }
    }
}