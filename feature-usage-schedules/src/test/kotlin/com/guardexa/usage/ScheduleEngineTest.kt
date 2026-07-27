
package com.guardexa.usage

import com.guardexa.usage.engine.ScheduleEngine
import com.guardexa.usage.engine.ScheduleEvaluation
import com.guardexa.usage.model.ScheduleWindow
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleEngineTest {

    private val engine = ScheduleEngine()

    @Test
    fun daytimeWindowAllowsMatchingMinute() {
        val result = engine.evaluate(
            windows = listOf(
                ScheduleWindow(
                    id = "study",
                    profileId = "default",
                    name = "Study",
                    startMinuteOfDay = 7 * 60,
                    endMinuteOfDay = 15 * 60,
                    daysOfWeekMask = 0b0111110,
                    enabled = true,
                    priority = 1
                )
            ),
            dayOfWeekIndex = 1,
            minuteOfDay = 10 * 60
        )

        assertEquals(ScheduleEvaluation.ALLOWED, result)
    }

    @Test
    fun overnightWindowSupportsMidnightCrossing() {
        val window = ScheduleWindow(
            id = "sleep",
            profileId = "default",
            name = "Sleep",
            startMinuteOfDay = 22 * 60,
            endMinuteOfDay = 6 * 60,
            daysOfWeekMask = 0b1111111,
            enabled = true,
            priority = 1
        )

        assertEquals(
            ScheduleEvaluation.ALLOWED,
            engine.evaluate(
                windows = listOf(window),
                dayOfWeekIndex = 2,
                minuteOfDay = 23 * 60
            )
        )

        assertEquals(
            ScheduleEvaluation.ALLOWED,
            engine.evaluate(
                windows = listOf(window),
                dayOfWeekIndex = 2,
                minuteOfDay = 5 * 60
            )
        )
    }
}
