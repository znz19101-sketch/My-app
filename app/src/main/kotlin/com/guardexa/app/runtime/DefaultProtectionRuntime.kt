package com.guardexa.app.runtime

import android.content.Context
import android.content.Intent
import android.util.Log
import com.guardexa.app.ProtectionGateActivity
import com.guardexa.device.service.ProtectionRuntime
import com.guardexa.feature.apps.domain.model.AppAccessPolicy
import com.guardexa.feature.apps.domain.repository.AppsRepository
import com.guardexa.ui.protection.ProtectionScreenReason
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultProtectionRuntime @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val applicationStateRepository: ApplicationStateRepository,
    private val appsRepository: AppsRepository
) : ProtectionRuntime {

    private var lastBlockedPackage: String? = null

    override suspend fun onServiceStarted() {
        Log.i(TAG, "Protection service started")
    }

    override suspend fun onForegroundPackageChanged(
        packageName: String?
    ) {
        if (packageName.isNullOrBlank()) return

        // Never block Guardexa itself.
        if (packageName == context.packageName) {
            lastBlockedPackage = null
            return
        }

        val runtimeState = applicationStateRepository.current()

        if (!runtimeState.protectionEnabled) {
            lastBlockedPackage = null
            return
        }

        val profileId = runtimeState.activeProfileId
            ?: DEFAULT_PROFILE_ID

        val appWithPolicy = appsRepository.getApp(
            packageName = packageName,
            profileId = profileId
        )

        val policy = appWithPolicy?.policy

        Log.d(
            TAG,
            "Foreground package=$packageName policy=${policy?.accessPolicy}"
        )

        when (policy?.accessPolicy) {
            AppAccessPolicy.ALWAYS_BLOCKED -> {
                showProtectionGate(
                    packageName = packageName,
                    reason = ProtectionScreenReason.APPLICATION_BLOCKED
                )
            }

            AppAccessPolicy.GLASSES_REQUIRED -> {
                /*
                 * AI verification will be connected in the next phase.
                 * Do not block until a real verification result is available.
                 */
                Log.d(
                    TAG,
                    "Glasses verification required; AI integration pending"
                )
            }

            else -> {
                lastBlockedPackage = null
            }
        }
    }

    override suspend fun onServiceStopped() {
        lastBlockedPackage = null
        Log.i(TAG, "Protection service stopped")
    }

    private fun showProtectionGate(
        packageName: String,
        reason: ProtectionScreenReason
    ) {
        if (lastBlockedPackage == packageName) return
        lastBlockedPackage = packageName

        val intent = Intent(
            context,
            ProtectionGateActivity::class.java
        ).apply {
            putExtra(
                ProtectionGateActivity.EXTRA_REASON,
                reason.name
            )

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        runCatching {
            context.startActivity(intent)
        }.onFailure { error ->
            lastBlockedPackage = null
            Log.e(
                TAG,
                "Unable to show protection gate",
                error
            )
        }
    }

    private companion object {
        const val TAG = "GuardexaProtection"
        const val DEFAULT_PROFILE_ID = "default-profile"
    }
}
