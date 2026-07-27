
package com.guardexa.database

import com.guardexa.database.model.LogRetentionPolicy
import kotlin.test.Test
import kotlin.test.assertFails

class RetentionPolicyTest {

    @Test
    fun negativeRetentionIsRejected() {
        assertFails {
            LogRetentionPolicy(
                activityLogRetentionMillis = -1L,
                securityLogRetentionMillis = 0L,
                selfTestRetentionMillis = 0L
            )
        }
    }
}
