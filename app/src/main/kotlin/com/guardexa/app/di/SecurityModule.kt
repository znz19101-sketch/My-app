
package com.guardexa.app.di

import android.content.Context
import com.guardexa.core.security.crypto.AndroidKeystoreCrypto
import com.guardexa.core.security.integrity.IntegrityVerifier
import com.guardexa.core.security.pin.PinHasher
import com.guardexa.core.security.pin.PinPolicy
import com.guardexa.privacy.PrivacyModeController
import com.guardexa.security.evidence.SecurityEvidenceIndex
import com.guardexa.security.evidence.SecurityEvidenceStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun providePinPolicy(): PinPolicy = PinPolicy()

    @Provides
    @Singleton
    fun providePinHasher(): PinHasher = PinHasher()

    @Provides
    @Singleton
    fun provideKeystoreCrypto(): AndroidKeystoreCrypto =
        AndroidKeystoreCrypto()

    @Provides
    @Singleton
    fun provideIntegrityVerifier(): IntegrityVerifier =
        IntegrityVerifier()

    @Provides
    @Singleton
    fun providePrivacyModeController(): PrivacyModeController =
        PrivacyModeController()

    @Provides
    @Singleton
    fun provideSecurityEvidenceStore(
        @ApplicationContext context: Context,
        index: SecurityEvidenceIndex,
        crypto: AndroidKeystoreCrypto
    ): SecurityEvidenceStore =
        SecurityEvidenceStore(
            directory = File(context.filesDir, "security_evidence"),
            index = index,
            crypto = crypto,
            maximumImages = 30,
            targetWidth = 320,
            jpegQuality = 55
        )
}
