
package com.guardexa.feature.apps.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.guardexa.feature.apps.data.local.AppsLocalDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PackageChangeReceiver(
    private val dataSourceProvider: ((Context) -> AppsLocalDataSource)? = null
) : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.extractPackageName() ?: return
        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                val dataSource = dataSourceProvider?.invoke(context)
                dataSource?.synchronizeSinglePackage(packageName)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun Uri.extractPackageName(): String? =
        schemeSpecificPart?.takeIf { it.isNotBlank() }
}
