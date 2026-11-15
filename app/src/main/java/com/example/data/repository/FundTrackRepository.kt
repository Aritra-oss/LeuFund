package com.example.data.repository

import com.example.data.local.BusinessDao
import com.example.data.local.BusinessEntity
import com.example.data.local.EntryDao
import com.example.data.local.EntryEntity
import com.example.data.local.EntryType
import com.example.data.local.SelectedBusinessDataStore
import com.example.domain.BusinessThemePalette
import com.example.domain.MpinManager
import com.example.domain.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class FundTrackRepository(
  private val businessDao: BusinessDao,
  private val entryDao: EntryDao,
  private val selectedBusinessDataStore: SelectedBusinessDataStore,
) {
  val openBusinesses: Flow<List<BusinessEntity>> = businessDao.observeOpenBusinesses()

  val closedBusinesses: Flow<List<BusinessEntity>> = businessDao.observeClosedBusinesses()

  val selectedBusinessId: Flow<Long?> = selectedBusinessDataStore.selectedBusinessId

  val themeMode: Flow<ThemeMode> = selectedBusinessDataStore.themeMode

  val lastExportMillis: Flow<Long?> = selectedBusinessDataStore.lastExportMillis

  val biometricLockEnabled: Flow<Boolean> = selectedBusinessDataStore.biometricLockEnabled

  val hasMpin: Flow<Boolean> = selectedBusinessDataStore.hasMpin

  suspend fun setThemeMode(mode: ThemeMode) {
    selectedBusinessDataStore.setThemeMode(mode)
  }

  suspend fun setBiometricLockEnabled(enabled: Boolean) {
    selectedBusinessDataStore.setBiometricLockEnabled(enabled)
  }

  suspend fun setupMpin(pin: String) {
    val hashRecord = withContext(Dispatchers.Default) { MpinManager.hashPin(pin) }
    selectedBusinessDataStore.setMpinHashRecord(hashRecord)
  }

  suspend fun verifyMpin(pin: String): Boolean {
    val storedRecord = selectedBusinessDataStore.mpinHashRecord.firstOrNull() ?: return false
    return withContext(Dispatchers.Default) { MpinManager.verifyPin(pin, storedRecord) }
  }

  suspend fun changeMpin(currentPin: String, newPin: String): Boolean {
    if (!MpinManager.isValidFormat(newPin)) return false
    val verified = verifyMpin(currentPin)
    if (!verified) return false
    setupMpin(newPin)
    return true
  }

  suspend fun recordExportCompleted(timestampMillis: Long = System.currentTimeMillis()) {
    selectedBusinessDataStore.recordExportTimestamp(timestampMillis)
  }

  fun observeEntriesForBusiness(businessId: Long): Flow<List<EntryEntity>> =
    entryDao.observeEntriesForBusiness(businessId)

  suspend fun createBusiness(name: String): Long {
    val cleanName = name.trim()
    val nextThemeId = selectedBusinessDataStore.consumeNextThemeId(BusinessThemePalette.entries.size)
    val newId =
      businessDao.insertBusiness(
        BusinessEntity(
          name = cleanName,
          themeId = nextThemeId,
          isClosed = false,
          createdAt = System.currentTimeMillis(),
        )
      )
    selectedBusinessDataStore.setSelectedBusinessId(newId)
    return newId
  }

  suspend fun selectBusiness(businessId: Long?) {
    selectedBusinessDataStore.setSelectedBusinessId(businessId)
  }

  suspend fun updateBusinessTheme(businessId: Long, themeId: Int) {
    businessDao.updateTheme(businessId = businessId, themeId = themeId)
  }

  suspend fun closeBusiness(businessId: Long) {
    businessDao.setClosed(businessId = businessId, isClosed = true)
  }

  suspend fun reopenBusiness(businessId: Long) {
    businessDao.setClosed(businessId = businessId, isClosed = false)
    selectedBusinessDataStore.setSelectedBusinessId(businessId)
  }

  suspend fun deleteBusiness(businessId: Long) {
    businessDao.deleteBusinessById(businessId)
  }

  suspend fun deleteAllBusinessData(businessId: Long) {
    entryDao.deleteAllForBusiness(businessId)
  }

  suspend fun addEntry(businessId: Long, type: EntryType, name: String, amount: Double): Long {
    return entryDao.insertEntry(
      EntryEntity(
        businessId = businessId,
        type = type,
        name = name.trim(),
        amount = amount,
        createdAt = System.currentTimeMillis(),
      )
    )
  }

  suspend fun restoreEntry(entry: EntryEntity) {
    entryDao.insertEntry(entry)
  }

  suspend fun deleteEntry(entryId: Long) {
    entryDao.deleteEntryById(entryId)
  }

  suspend fun markDueAsReceived(entryId: Long) {
    entryDao.updateEntryType(entryId = entryId, newType = EntryType.REVENUE)
  }

  suspend fun updateEntry(
    existing: EntryEntity,
    newType: EntryType,
    newName: String,
    newAmount: Double,
  ) {
    entryDao.updateEntry(
      existing.copy(
        type = newType,
        name = newName.trim(),
        amount = newAmount,
      )
    )
  }
}
