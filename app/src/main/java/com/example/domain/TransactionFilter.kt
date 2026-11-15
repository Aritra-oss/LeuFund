package com.example.domain

import com.example.data.local.EntryEntity

data class FilteredEntries(
  val investments: List<EntryEntity>,
  val revenues: List<EntryEntity>,
  val dues: List<EntryEntity>,
) {
  val totalCount: Int
    get() = investments.size + revenues.size + dues.size
}

object TransactionFilter {
  fun filterByName(
    investments: List<EntryEntity>,
    revenues: List<EntryEntity>,
    dues: List<EntryEntity>,
    query: String,
  ): FilteredEntries {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
      return FilteredEntries(investments = investments, revenues = revenues, dues = dues)
    }
    return FilteredEntries(
      investments = investments.filter { it.name.contains(trimmed, ignoreCase = true) },
      revenues = revenues.filter { it.name.contains(trimmed, ignoreCase = true) },
      dues = dues.filter { it.name.contains(trimmed, ignoreCase = true) },
    )
  }
}
