package com.pennywiseai.tracker.presentation.transactions

import androidx.lifecycle.ViewModel
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.export.CsvExporter
import com.pennywiseai.tracker.data.export.ExportResult
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val csvExporter: CsvExporter,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    fun exportTransactions(
        transactions: List<TransactionEntity>,
        fileName: String? = null
    ): Flow<ExportResult> {
        return csvExporter.exportTransactions(transactions, fileName)
    }

    /** @see UserPreferencesRepository.claimSupportNudge — global, frequency-capped. */
    suspend fun claimSupportNudge(): Boolean = userPreferencesRepository.claimSupportNudge()
}
