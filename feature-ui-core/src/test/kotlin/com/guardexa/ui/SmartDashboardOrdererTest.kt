
package com.guardexa.ui

import com.guardexa.ui.dashboard.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SmartDashboardOrdererTest {

    @Test
    fun criticalCardsAppearFirst() {
        val orderer = SmartDashboardOrderer()

        val result = orderer.order(
            listOf(
                DashboardCard(
                    type = DashboardCardType.TIME_REMAINING,
                    title = "Time",
                    value = "20 min",
                    priority = 10
                ),
                DashboardCard(
                    type = DashboardCardType.CRITICAL_SECURITY,
                    title = "Security",
                    value = "Action required",
                    priority = 100
                )
            )
        )

        assertEquals(
            DashboardCardType.CRITICAL_SECURITY,
            result.first().type
        )
    }
}
