package com.scenevo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.scenevo.app.navigation.ScenevoNavHost
import com.scenevo.core.designsystem.theme.ScenevoColors
import com.scenevo.core.designsystem.theme.ScenevoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScenevoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ScenevoColors.Ink,
                ) {
                    ScenevoNavHost()
                }
            }
        }
    }
}
