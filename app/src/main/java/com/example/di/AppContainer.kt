package com.example.di

import android.content.Context
import com.example.data.local.FundTrackDatabase
import com.example.data.local.SelectedBusinessDataStore
import com.example.data.repository.FundTrackRepository

class AppContainer(context: Context) {
  private val database: FundTrackDatabase by lazy { FundTrackDatabase.getInstance(context) }

  private val selectedBusinessDataStore: SelectedBusinessDataStore by lazy {
    SelectedBusinessDataStore(context)
  }

  val repository: FundTrackRepository by lazy {
    FundTrackRepository(
      businessDao = database.businessDao(),
      entryDao = database.entryDao(),
      selectedBusinessDataStore = selectedBusinessDataStore,
    )
  }
}
