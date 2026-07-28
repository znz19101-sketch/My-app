
package com.guardexa.feature.apps.di

import android.content.Context
import androidx.room.Room
import com.guardexa.core.database.AppDatabase
import com.guardexa.feature.apps.data.local.AppsLocalDataSource
import com.guardexa.core.database.apps.dao.AppsDao
import com.guardexa.feature.apps.data.repository.RoomAppsRepository
import com.guardexa.feature.apps.device.AndroidInstalledAppsScanner
import com.guardexa.feature.apps.device.InstalledAppsScanner
import com.guardexa.feature.apps.domain.repository.AppsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppsFeatureModule {

    @Provides
    @Singleton
    fun provideInstalledAppsScanner(
        @ApplicationContext context: Context
    ): InstalledAppsScanner =
        AndroidInstalledAppsScanner(context)

    @Provides
    @Singleton
    fun provideAppsDao(
        database: AppDatabase
    ): AppsDao = database.appsDao()

    @Provides
    @Singleton
    fun provideAppsLocalDataSource(
        database: AppDatabase,
        dao: AppsDao,
        scanner: InstalledAppsScanner
    ): AppsLocalDataSource =
        AppsLocalDataSource(
            database = database,
            dao = dao,
            scanner = scanner
        )

    @Provides
    @Singleton
    fun provideAppsRepository(
        dao: AppsDao
    ): AppsRepository =
        RoomAppsRepository(dao)
}
