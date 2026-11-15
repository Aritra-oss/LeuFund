package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BusinessEntity
import com.example.data.local.EntryEntity
import com.example.data.local.EntryType
import com.example.data.local.FundTrackDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: FundTrackDatabase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, FundTrackDatabase::class.java).build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun appName_matchesLeuFunds() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("LeuFunds", context.getString(R.string.app_name))
  }

  @Test
  fun roomDatabase_startsEmpty_andCascadeDeletesEntries() = runTest {
    assertTrue(db.businessDao().observeOpenBusinesses().first().isEmpty())

    val bizId =
      db.businessDao().insertBusiness(BusinessEntity(name = "Chai Point", themeId = 0))
    val dueId =
      db.entryDao()
        .insertEntry(
          EntryEntity(
            businessId = bizId,
            type = EntryType.DUE,
            name = "Rohan",
            amount = 2500.0,
          )
        )

    db.entryDao().updateEntryType(dueId, EntryType.REVENUE)
    val entriesAfterReceive = db.entryDao().observeEntriesForBusiness(bizId).first()
    assertEquals(1, entriesAfterReceive.size)
    assertEquals(EntryType.REVENUE, entriesAfterReceive[0].type)

    db.businessDao().deleteBusinessById(bizId)
    assertTrue(db.entryDao().observeEntriesForBusiness(bizId).first().isEmpty())
  }
}
