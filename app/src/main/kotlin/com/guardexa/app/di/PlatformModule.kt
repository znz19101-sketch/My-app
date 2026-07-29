
package com.guardexa.app.di

import android.content.Context
import com.guardexa.app.runtime.DefaultProtectionRuntime
import com.guardexa.device.compatibility.ManufacturerCompatibility
import com.guardexa.device.permissions.PermissionOrchestrator
import com.guardexa.device.service.ProtectionRuntime
import com.guardexa.device.usage.ForegroundAppMonitor
import com.guardexa.notifications.AndroidNotificationPublisher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {

    @Provides
    @Singleton
    fun providePermissionOrchestrator(
        @ApplicationContext context: Context
    ): PermissionOrchestrator =
        PermissionOrchestrator(context)

    @Provides
    @Singleton
    fun provideForegroundAppMonitor(
        @ApplicationContext context: Context
    ): ForegroundAppMonitor =
        ForegroundAppMonitor(context)

    @Provides
    @Singleton
    fun provideNotificationPublisher(
        @ApplicationContext context: Context
    ): AndroidNotificationPublisher =
        AndroidNotificationPublisher(context)

    @Provides
    @Singleton
    fun provideManufacturerCompatibility():
        ManufacturerCompatibility =
        ManufacturerCompatibility()
    @Provides
    @Singleton
    fun provideProtectionRuntime(
        runtime: DefaultProtectionRuntime
    ): ProtectionRuntime = runtime

}
