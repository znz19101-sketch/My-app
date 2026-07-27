
package com.guardexa.app.di

import android.content.Context
import androidx.room.Room
import com.guardexa.core.database.AppDatabase
import com.guardexa.database.dao.GuardexaDao
import com.guardexa.database.migration.GuardexaMigrations
import com.guardexa.feature.apps.data.local.AppsDatabaseIntegration
import com.guardexa.feature.apps.data.local.dao.AppsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "guardexa.db"
        )
            .addMigrations(
                AppsDatabaseIntegration.MIGRATION_1_2,
                GuardexaMigrations.MIGRATION_2_3
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideAppsDao(
        database: AppDatabase
    ): AppsDao = database.appsDao()

    @Provides
    fun provideGuardexaDao(
        database: AppDatabase
    ): GuardexaDao = database.guardexaDao()
}
