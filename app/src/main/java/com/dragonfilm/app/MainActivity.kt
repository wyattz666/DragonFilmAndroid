package com.dragonfilm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dragonfilm.app.ui.navigation.AppNavigation
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DragonFilmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DragonFilmApp

        setContent {
            DragonFilmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DFColor.Bg
                ) {
                    AppNavigation(
                        repository = app.movieRepository,
                        localStore = app.localStore,
                        authManager = app.authManager,
                        cloudSync = app.cloudSync
                    )
                }
            }
        }
    }
}
