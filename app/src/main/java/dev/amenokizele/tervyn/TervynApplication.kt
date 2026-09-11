package dev.amenokizele.tervyn

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.amenokizele.tervyn.data.local.sync.SyncScheduler
import javax.inject.Inject

@HiltAndroidApp
class TervynApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler

    override fun onCreate() {
        super.onCreate()
        syncScheduler.ensurePeriodic()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
