
package com.guardexa.usage.engine

import com.guardexa.usage.model.ScheduleWindow

enum class ScheduleEvaluation {
    ALLOWED,
    BLOCKED,
    NOT_CONFIGURED
}

class ScheduleEngine {

    fun evaluate(
        windows: List<ScheduleWindow>,
        dayOfWeekIndex: Int,
        minuteOfDay: Int
    ): ScheduleEvaluation {
        val enabled = windows.filter { it.enabled }
        if (enabled.isEmpty()) return ScheduleEvaluation.NOT_CONFIGURED

        val matching = enabled
            .filter { it.includesDay(dayOfWeekIndex) }
            .sortedByDescending { it.priority }

        if (matching.isEmpty()) return ScheduleEvaluation.BLOCKED

        return if (matching.any { it.containsMinute(minuteOfDay) }) {
            ScheduleEvaluation.ALLOWED
        } else {
            ScheduleEvaluation.BLOCKED
        }
    }
}
