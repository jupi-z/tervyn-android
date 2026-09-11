package dev.amenokizele.tervyn.data.local.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext context: Context,
    private val config: RemoteApiConfig
) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueueImmediate() {
        if (!config.enabled) return
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            oneTimeRequest()
        )
    }

    fun ensurePeriodic() {
        if (!config.enabled) return
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        )
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    private fun oneTimeRequest() = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(networkConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    companion object {
        const val IMMEDIATE_WORK_NAME = "tervyn-sync-immediate"
        const val PERIODIC_WORK_NAME = "tervyn-sync-periodic"
    }
}
