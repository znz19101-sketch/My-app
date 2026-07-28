
package com.guardexa.app.di

import com.guardexa.app.runtime.ApplicationStateRepository
import com.guardexa.app.runtime.DefaultApplicationStateRepository
import com.guardexa.database.dao.GuardexaDao
import com.guardexa.database.repository.ActivityLogRepository
import com.guardexa.database.repository.RoomActivityLogRepository
import com.guardexa.feature.apps.data.repository.RoomAppsRepository
import com.guardexa.feature.apps.domain.repository.AppsRepository
import com.guardexa.security.evidence.SecurityEvidenceIndex
import com.guardexa.database.repository.RoomSecurityEvidenceIndex
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EpochTime

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ElapsedRealtime

@Module
@InstallIn(SingletonComponent::class)
object CoreBindingsModule {

    @Provides
    @Singleton
    @EpochTime
    fun provideEpochTime(): () -> Long =
        { System.currentTimeMillis() }

    @Provides
    @Singleton
    @ElapsedRealtime
    fun provideElapsedRealtime(): () -> Long =
        { android.os.SystemClock.elapsedRealtime() }

    @Provides
    @Singleton
    fun provideApplicationStateRepository(
        dao: GuardexaDao,
        @EpochTime epochTime: () -> Long
    ): ApplicationStateRepository =
        DefaultApplicationStateRepository(
            dao = dao,
            epochTime = epochTime
        )

    @Provides
    @Singleton
    fun provideAppsRepository(
        dao: com.guardexa.core.database.apps.dao.AppsDao
    ): AppsRepository =
        RoomAppsRepository(dao)

    @Provides
    @Singleton
    fun provideActivityLogRepository(
        dao: GuardexaDao,
        @EpochTime epochTime: () -> Long
    ): ActivityLogRepository =
        RoomActivityLogRepository(
            dao = dao,
            clock = epochTime
        )

    @Provides
    @Singleton
    fun provideSecurityEvidenceIndex(
        dao: GuardexaDao
    ): SecurityEvidenceIndex =
        RoomSecurityEvidenceIndex(dao)
}
