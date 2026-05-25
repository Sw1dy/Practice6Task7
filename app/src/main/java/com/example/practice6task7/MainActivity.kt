package com.example.practice6task7

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.practice6task7.presentation.BleScannerScreen
import com.example.practice6task7.ui.theme.Practice6Task7Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Practice6Task7Theme {
                BleScannerScreen()
            }
        }
    }
}