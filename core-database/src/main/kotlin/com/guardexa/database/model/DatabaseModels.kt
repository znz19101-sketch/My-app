
package com.guardexa.database.model

enum class LogSeverity {
    INFO,
    NOTICE,
    WARNING,
    CRITICAL
}

enum class SelfTestLevel {
    PASSED,
    WARNING,
    RECOVERABLE_FAILURE,
    SAFE_MODE_REQUIRED,
    CRITICAL_FAILURE
}

data class LogRetentionPolicy(
    val activityLogRetentionMillis: Long,
    val securityLogRetentionMillis: Long,
    val selfTestRetentionMillis: Long,
    val diagnosticRetentionMillis: Long = 7L * 24L * 60L * 60L * 1000L
) {
    init {
        require(activityLogRetentionMillis >= 0L)
        require(securityLogRetentionMillis >= 0L)
        require(selfTestRetentionMillis >= 0L)
    }
}
