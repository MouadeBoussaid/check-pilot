package com.mebiad.checkpilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mebiad.checkpilot.ui.generic.navigation.AppNavigation
import com.mebiad.checkpilot.ui.generic.theme.CheckPilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CheckPilotTheme {
                AppNavigation()
            }
        }
    }
}
