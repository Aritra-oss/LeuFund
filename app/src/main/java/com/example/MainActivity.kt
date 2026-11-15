package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.example.ui.FundTrackApp
import com.example.ui.viewmodel.FundTrackViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val app = application as FundTrackApplication
    val viewModel =
      ViewModelProvider(
        this,
        FundTrackViewModel.Factory(app.appContainer.repository),
      )[FundTrackViewModel::class.java]

    setContent { FundTrackApp(viewModel = viewModel) }
  }
}
