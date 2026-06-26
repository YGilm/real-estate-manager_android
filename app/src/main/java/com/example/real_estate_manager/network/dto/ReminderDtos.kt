package com.example.real_estate_manager.network.dto

import com.google.gson.annotations.SerializedName

data class ReminderDto(

    val id: String,

    @SerializedName(value = "propertyId", alternate = ["property_id"])
    val propertyId: String?,

    val title: String,

    val message: String?,

    @SerializedName(value = "reminderType", alternate = ["type"])
    val type: String,

    @SerializedName(value = "scheduleMode", alternate = ["schedule_mode"])
    val scheduleMode: String,

    // Django сейчас отдает ISO строку в oneTimeAt/one_time_at.
    @SerializedName(value = "oneTimeAt", alternate = ["one_time_at"])
    val oneTimeAt: String?,

    @SerializedName("range_start_at")
    val rangeStartAt: Long?,

    @SerializedName("range_end_at")
    val rangeEndAt: Long?,

    @SerializedName("day_of_month")
    val dayOfMonth: Int?,

    @SerializedName("range_start_day")
    val rangeStartDay: Int?,

    @SerializedName("range_end_day")
    val rangeEndDay: Int?,

    @SerializedName("repeat_every_days")
    val repeatEveryDays: Int?,

    @SerializedName("offset_days")
    val offsetDays: Int?,

    val hour: Int = 0,

    val minute: Int = 0,

    val enabled: Boolean,

    @SerializedName(value = "nextTriggerAtMs", alternate = ["next_trigger_at_ms"])
    val nextTriggerAt: Long?,

    @SerializedName(value = "lastFiredAtMs", alternate = ["last_fired_at_ms"])
    val lastFiredAt: Long?,

    @SerializedName(value = "createdAt", alternate = ["created_at_ms"])
    val createdAt: Long,

    @SerializedName(value = "updatedAt", alternate = ["updated_at_ms"])
    val updatedAt: Long
)

data class ReminderRequestDto(
    val id: String?,
    @SerializedName("property_id") val propertyId: String?,
    val title: String,
    val message: String?,
    val type: String,
    @SerializedName("schedule_mode") val scheduleMode: String,
    @SerializedName("one_time_at") val oneTimeAt: String?,
    @SerializedName("range_start_at") val rangeStartAt: Long?,
    @SerializedName("range_end_at") val rangeEndAt: Long?,
    @SerializedName("day_of_month") val dayOfMonth: Int?,
    @SerializedName("range_start_day") val rangeStartDay: Int?,
    @SerializedName("range_end_day") val rangeEndDay: Int?,
    @SerializedName("repeat_every_days") val repeatEveryDays: Int?,
    @SerializedName("offset_days") val offsetDays: Int?,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    @SerializedName("next_trigger_at_ms") val nextTriggerAt: Long?
)
