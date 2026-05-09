package com.example.real_estate_manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.real_estate_manager.auth.UserSession
import com.example.real_estate_manager.data.RealEstateRepository
import com.example.real_estate_manager.data.ReminderRepository
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.model.Property
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import com.example.real_estate_manager.data.model.ReminderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModel @Inject constructor(
    private val reminders: ReminderRepository,
    private val realEstateRepository: RealEstateRepository,
    session: UserSession
) : ViewModel() {

    private val userIdFlow: StateFlow<String?> =
        session.userIdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val properties: StateFlow<List<Property>> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else realEstateRepository.properties(uid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rules(propertyId: String?): Flow<List<ReminderRuleEntity>> =
        userIdFlow.flatMapLatest { uid ->
            when {
                uid == null -> flowOf(emptyList())
                propertyId.isNullOrBlank() -> reminders.observeAll(uid)
                else -> reminders.observeForProperty(uid, propertyId)
            }
        }

    fun createRule(
        propertyId: String?,
        title: String,
        message: String?,
        type: ReminderType,
        scheduleMode: ReminderScheduleMode,
        oneTimeAt: Long?,
        rangeStartAt: Long?,
        rangeEndAt: Long?,
        dayOfMonth: Int?,
        rangeStartDay: Int?,
        rangeEndDay: Int?,
        repeatEveryDays: Int?,
        offsetDays: Int?,
        hour: Int,
        minute: Int,
        enabled: Boolean = true
    ) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            reminders.createRule(
                userId = uid,
                propertyId = propertyId,
                title = title,
                message = message,
                type = type,
                scheduleMode = scheduleMode,
                oneTimeAt = oneTimeAt,
                rangeStartAt = rangeStartAt,
                rangeEndAt = rangeEndAt,
                dayOfMonth = dayOfMonth,
                rangeStartDay = rangeStartDay,
                rangeEndDay = rangeEndDay,
                repeatEveryDays = repeatEveryDays,
                offsetDays = offsetDays,
                hour = hour,
                minute = minute,
                enabled = enabled
            )
        }
    }

    fun createDefaultLeaseEndReminders(propertyId: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            reminders.createDefaultLeaseEndReminders(uid, propertyId)
        }
    }

    fun updateRule(
        rule: ReminderRuleEntity,
        title: String,
        message: String?,
        scheduleMode: ReminderScheduleMode,
        oneTimeAt: Long?,
        rangeStartAt: Long?,
        rangeEndAt: Long?,
        dayOfMonth: Int?,
        repeatEveryDays: Int?,
        hour: Int,
        minute: Int,
        enabled: Boolean
    ) {
        viewModelScope.launch {
            val updatedBase = rule.copy(
                title = title,
                message = message,
                scheduleMode = scheduleMode.name,
                oneTimeAt = oneTimeAt,
                rangeStartAt = rangeStartAt,
                rangeEndAt = rangeEndAt,
                dayOfMonth = dayOfMonth,
                repeatEveryDays = repeatEveryDays,
                hour = hour,
                minute = minute,
                enabled = enabled,
                updatedAt = System.currentTimeMillis()
            )
            val next = if (enabled) reminders.recomputeNextTriggerAt(updatedBase, LocalDateTime.now()) else Long.MAX_VALUE
            reminders.upsert(updatedBase.copy(nextTriggerAt = next))
        }
    }

    fun setEnabled(rule: ReminderRuleEntity, enabled: Boolean) {
        viewModelScope.launch {
            val updatedBase = rule.copy(enabled = enabled, updatedAt = System.currentTimeMillis())
            val next = if (enabled) reminders.recomputeNextTriggerAt(updatedBase, LocalDateTime.now()) else Long.MAX_VALUE
            reminders.upsert(updatedBase.copy(nextTriggerAt = next))
        }
    }

    fun delete(rule: ReminderRuleEntity) {
        viewModelScope.launch {
            reminders.deleteById(rule.userId, rule.id)
        }
    }

}
