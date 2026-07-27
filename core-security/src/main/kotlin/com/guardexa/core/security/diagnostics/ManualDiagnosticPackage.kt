
package com.guardexa.core.security.diagnostics

import java.io.File
import java.util.Locale

data class DiagnosticField(
    val key: String,
    val value: String
)

data class DiagnosticPackage(
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    val fields: List<DiagnosticField>,
    val reportFile: File
)

class DiagnosticReportBuilder(
    private val retentionMillis: Long = 7L * 24L * 60L * 60L * 1000L
) {
    fun build(
        outputDirectory: File,
        nowEpochMillis: Long,
        fields: List<DiagnosticField>
    ): DiagnosticPackage {
        outputDirectory.mkdirs()

        val safeFields = fields
            .filterNot { it.key.isSensitiveKey() }
            .map { field ->
                field.copy(
                    value = field.value
                        .replace("\r", " ")
                        .replace("\n", " ")
                        .take(500)
                )
            }

        val file = File(
            outputDirectory,
            "guardexa_diagnostic_${nowEpochMillis}.txt"
        )

        val body = buildString {
            appendLine("Guardexa Diagnostic Report")
            appendLine("Created: $nowEpochMillis")
            appendLine("No images, PINs, keys, app usage history, or face data are included.")
            appendLine()

            safeFields.forEach {
                appendLine("${it.key}=${it.value}")
            }
        }

        file.writeText(body)

        return DiagnosticPackage(
            createdAtEpochMillis = nowEpochMillis,
            expiresAtEpochMillis = nowEpochMillis + retentionMillis,
            fields = safeFields,
            reportFile = file
        )
    }

    fun deleteExpired(
        outputDirectory: File,
        nowEpochMillis: Long
    ): Int {
        if (!outputDirectory.exists()) return 0

        var deleted = 0
        outputDirectory
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.startsWith("guardexa_diagnostic_") }
            .forEach { file ->
                val timestamp = file.name
                    .removePrefix("guardexa_diagnostic_")
                    .removeSuffix(".txt")
                    .toLongOrNull()

                if (
                    timestamp != null &&
                    nowEpochMillis - timestamp >= retentionMillis &&
                    file.delete()
                ) {
                    deleted++
                }
            }

        return deleted
    }

    private fun String.isSensitiveKey(): Boolean {
        val normalized = lowercase(Locale.US)
        return normalized.contains("pin") ||
            normalized.contains("password") ||
            normalized.contains("secret") ||
            normalized.contains("token") ||
            normalized.contains("face") ||
            normalized.contains("image") ||
            normalized.contains("application_history")
    }
}
