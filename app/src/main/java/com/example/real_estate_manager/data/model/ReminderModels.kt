
package com.example.real_estate_manager.data.model

enum class ReminderType {
    READINGS,
    UTILITIES,
    INTERNET,
    RENT,
    LEASE_END
}

enum class ReminderScheduleMode {
    ONE_TIME,
    DAILY,
    MONTHLY,
    DATE_RANGE,
    DAY_OF_MONTH,
    DATE_RANGE_MONTHLY,
    RELATIVE_TO_LEASE_END
}
