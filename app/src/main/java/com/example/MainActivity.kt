package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.ui.FundTrackApp
import com.example.ui.viewmodel.FundTrackViewModel

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    enableHighRefreshRate()

    val app = application as FundTrackApplication
    val viewModel =
      ViewModelProvider(
        this,
        FundTrackViewModel.Factory(app.appContainer.repository),
      )[FundTrackViewModel::class.java]

    setContent { FundTrackApp(viewModel = viewModel) }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    enableHighRefreshRate()
  }

  private fun enableHighRefreshRate() {
    val currentDisplay =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display
      } else {
        @Suppress("DEPRECATION") windowManager.defaultDisplay
      } ?: return

    val activeMode = currentDisplay.mode
    val highestRateMode =
      currentDisplay.supportedModes
        .filter {
          it.physicalWidth == activeMode.physicalWidth &&
            it.physicalHeight == activeMode.physicalHeight
        }
        .maxByOrNull { it.refreshRate } ?: return

    val params = window.attributes
    if (
      params.preferredDisplayModeId != highestRateMode.modeId ||
        params.preferredRefreshRate != highestRateMode.refreshRate
    ) {
      params.preferredDisplayModeId = highestRateMode.modeId
      params.preferredRefreshRate = highestRateMode.refreshRate
      window.attributes = params
    }
  }
}
