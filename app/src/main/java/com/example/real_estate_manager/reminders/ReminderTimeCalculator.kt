package com.example.real_estate_manager.reminders

import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

object ReminderTimeCalculator {
    fun nextTriggerAt(rule: ReminderRuleEntity, now: LocalDateTime = LocalDateTime.now()): Long {
        if (!rule.enabled) return Long.MAX_VALUE
        return when (enumValueOf<ReminderScheduleMode>(rule.scheduleMode)) {
            ReminderScheduleMode.ONE_TIME -> rule.oneTimeAt?.takeIf { it > now.toMillis() } ?: Long.MAX_VALUE
            ReminderScheduleMode.DAILY -> nextDaily(rule, now)
            ReminderScheduleMode.MONTHLY -> nextMonthly(rule, now)
            ReminderScheduleMode.DATE_RANGE -> nextDateRange(rule, now)
            ReminderScheduleMode.DAY_OF_MONTH -> nextMonthly(rule, now)
            ReminderScheduleMode.DATE_RANGE_MONTHLY -> nextMonthlyRange(rule, now)
            ReminderScheduleMode.RELATIVE_TO_LEASE_END -> rule.nextTriggerAt
        }
    }

    private fun nextDaily(rule: ReminderRuleEntity, now: LocalDateTime): Long {
        var candidate = now.toLocalDate().atTime(rule.hour.coerceIn(0, 23), rule.minute.coerceIn(0, 59))
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        return candidate.toMillis()
    }

    private fun nextMonthly(rule: ReminderRuleEntity, now: LocalDateTime): Long {
        val day = (rule.dayOfMonth ?: 1).coerceIn(1, 31)
        var month = YearMonth.from(now)
        var candidate = atRuleTime(month.atDay(day.coerceAtMost(month.lengthOfMonth())), rule)
        if (!candidate.isAfter(now)) {
            month = month.plusMonths(1)
            candidate = atRuleTime(month.atDay(day.coerceAtMost(month.lengthOfMonth())), rule)
        }
        return candidate.toMillis()
    }

    private fun nextDateRange(rule: ReminderRuleEntity, now: LocalDateTime): Long {
        val start = rule.rangeStartAt?.toDate() ?: return Long.MAX_VALUE
        val end = rule.rangeEndAt?.toDate() ?: return Long.MAX_VALUE
        val repeat = (rule.repeatEveryDays ?: 1).coerceAtLeast(1)
        var date = if (now.toLocalDate().isAfter(start)) now.toLocalDate() else start
        while (!date.isAfter(end)) {
            val daysFromStart = java.time.temporal.ChronoUnit.DAYS.between(start, date).toInt()
            if (daysFromStart >= 0 && daysFromStart % repeat == 0) {
                val candidate = atRuleTime(date, rule)
                if (candidate.isAfter(now)) return candidate.toMillis()
            }
            date = date.plusDays(1)
        }
        return Long.MAX_VALUE
    }

    private fun nextMonthlyRange(rule: ReminderRuleEntity, now: LocalDateTime): Long {
        val start = (rule.rangeStartDay ?: 1).coerceIn(1, 31)
        val end = (rule.rangeEndDay ?: start).coerceIn(start, 31)
        val repeat = (rule.repeatEveryDays ?: 1).coerceAtLeast(1)
        var month = YearMonth.from(now)
        while (true) {
            val firstDay = start.coerceAtMost(month.lengthOfMonth())
            val lastDay = end.coerceAtMost(month.lengthOfMonth())
            var day = if (month == YearMonth.from(now)) now.dayOfMonth.coerceAtLeast(firstDay) else firstDay
            while (day <= lastDay) {
                val candidate = atRuleTime(month.atDay(day), rule)
                if (candidate.isAfter(now)) return candidate.toMillis()
                day += repeat
            }
            month = month.plusMonths(1)
        }
    }

    private fun atRuleTime(date: LocalDate, rule: ReminderRuleEntity): LocalDateTime =
        date.atTime(rule.hour.coerceIn(0, 23), rule.minute.coerceIn(0, 59))

    private fun Long.toDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    fun LocalDateTime.toMillis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
