package com.example.real_estate_manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.real_estate_manager.auth.UserSession
import com.example.real_estate_manager.data.RealEstateRepository
import com.example.real_estate_manager.data.model.FieldEntry
import com.example.real_estate_manager.data.model.ProviderWidget
import com.example.real_estate_manager.data.model.ProviderWidgetType
import com.example.real_estate_manager.data.model.WidgetField
import com.example.real_estate_manager.data.model.WidgetFieldType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class TemplateField(
    val name: String,
    val fieldType: WidgetFieldType,
    val unit: String?,
    val sortOrder: Int
)

enum class WidgetTemplate(
    val key: String,
    val title: String,
    val type: ProviderWidgetType,
    val fields: List<TemplateField>
) {
    MOSENERGO_SINGLE(
        key = "mosenergo_single",
        title = "Мосэнерго",
        type = ProviderWidgetType.METER_PROVIDER,
        fields = listOf(
            TemplateField("Показание", WidgetFieldType.METER, "кВт·ч", 0)
        )
    ),
    MOSENERGO_DAYNIGHT(
        key = "mosenergo_daynight",
        title = "Мосэнерго",
        type = ProviderWidgetType.METER_PROVIDER,
        fields = listOf(
            TemplateField("День", WidgetFieldType.METER, "кВт·ч", 0),
            TemplateField("Ночь", WidgetFieldType.METER, "кВт·ч", 1)
        )
    ),
    MOSENERGO_THREETARIFF(
        key = "mosenergo_threetariff",
        title = "Мосэнерго",
        type = ProviderWidgetType.METER_PROVIDER,
        fields = listOf(
            TemplateField("Т1", WidgetFieldType.METER, "кВт·ч", 0),
            TemplateField("Т2", WidgetFieldType.METER, "кВт·ч", 1),
            TemplateField("Т3", WidgetFieldType.METER, "кВт·ч", 2)
        )
    ),
    UK_WATER(
        key = "uk_water",
        title = "Вода",
        type = ProviderWidgetType.METER_PROVIDER,
        fields = listOf(
            TemplateField("ХВС", WidgetFieldType.METER, "м³", 0),
            TemplateField("ГВС", WidgetFieldType.METER, "м³", 1)
        )
    ),
    RENT(
        key = "rent",
        title = "Аренда",
        type = ProviderWidgetType.PAYMENT,
        fields = listOf(
            TemplateField("Сумма", WidgetFieldType.MONEY, "руб.", 0),
            TemplateField("Дедлайн", WidgetFieldType.TEXT, null, 1),
            TemplateField("Статус", WidgetFieldType.STATUS, null, 2),
            TemplateField("Контакт", WidgetFieldType.TEXT, null, 3)
        )
    ),
    CUSTOM(
        key = "custom",
        title = "Произвольный",
        type = ProviderWidgetType.CUSTOM,
        fields = listOf(
            TemplateField("Поле", WidgetFieldType.TEXT, null, 0)
        )
    )
}

data class ReadingsUiState(
    val widgets: List<ProviderWidget> = emptyList(),
    val fields: List<WidgetField> = emptyList(),
    val entries: List<FieldEntry> = emptyList()
)

data class FieldEntryInput(
    val fieldId: String,
    val fieldType: WidgetFieldType,
    val valueNumber: Double?,
    val valueText: String?,
    val status: String?
)

data class CustomWidgetField(
    val id: String,
    val name: String,
    val fieldType: WidgetFieldType,
    val unit: String?,
    val sortOrder: Int
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class PropertyReadingsViewModel @Inject constructor(
    private val repo: RealEstateRepository,
    session: UserSession
) : ViewModel() {

    private val userIdFlow: StateFlow<String?> =
        session.userIdFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val propertyIdFlow = MutableStateFlow<String?>(null)

    fun bind(propertyId: String) {
        if (propertyIdFlow.value != propertyId) {
            propertyIdFlow.value = propertyId
        }
    }

    val uiState: StateFlow<ReadingsUiState> =
        combine(userIdFlow, propertyIdFlow) { uid, pid -> uid to pid }
            .flatMapLatest { (uid, pid) ->
                if (uid == null || pid == null) {
                    flowOf(ReadingsUiState())
                } else {
                    combine(
                        repo.providerWidgets(uid, pid),
                        repo.widgetFields(uid, pid),
                        repo.fieldEntries(uid, pid)
                    ) { widgets, fields, entries ->
                        ReadingsUiState(widgets = widgets, fields = fields, entries = entries)
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ReadingsUiState()
            )

    fun addTemplate(template: WidgetTemplate) {
        val uid = userIdFlow.value ?: return
        val pid = propertyIdFlow.value ?: return

        val widgetId = UUID.randomUUID().toString()
        val widget = ProviderWidget(
            id = widgetId,
            propertyId = pid,
            type = template.type,
            title = template.title,
            templateKey = template.key
        )
        val fields = template.fields.map { field ->
            WidgetField(
                id = UUID.randomUUID().toString(),
                widgetId = widgetId,
                name = field.name,
                fieldType = field.fieldType,
                unit = field.unit,
                sortOrder = field.sortOrder
            )
        }

        viewModelScope.launch {
            repo.addProviderWidget(uid, widget, fields)
        }
    }

    fun addCustomWidget(title: String, fields: List<CustomWidgetField>) {
        val uid = userIdFlow.value ?: return
        val pid = propertyIdFlow.value ?: return
        val widgetId = UUID.randomUUID().toString()
        val widget = ProviderWidget(
            id = widgetId,
            propertyId = pid,
            type = ProviderWidgetType.CUSTOM,
            title = title,
            templateKey = WidgetTemplate.CUSTOM.key
        )
        val mapped = fields.map { field ->
            WidgetField(
                id = field.id,
                widgetId = widgetId,
                name = field.name,
                fieldType = field.fieldType,
                unit = field.unit,
                sortOrder = field.sortOrder
            )
        }
        viewModelScope.launch {
            repo.addProviderWidget(uid, widget, mapped)
        }
    }

    fun saveEntries(year: Int, month: Int, inputs: List<FieldEntryInput>) {
        val uid = userIdFlow.value ?: return
        val createdAt = System.currentTimeMillis()

        val entries = inputs.map { input ->
            FieldEntry(
                id = "${input.fieldId}_${year}_${month}",
                fieldId = input.fieldId,
                periodYear = year,
                periodMonth = month,
                valueNumber = input.valueNumber,
                valueText = input.valueText,
                status = input.status,
                createdAt = createdAt
            )
        }

        viewModelScope.launch {
            repo.upsertFieldEntries(uid, entries)
        }
    }

    fun updateWidget(widgetId: String, title: String, fields: List<CustomWidgetField>?) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.updateProviderWidgetTitle(uid, widgetId, title)
            if (fields != null) {
                val mapped = fields.map { field ->
                    WidgetField(
                        id = field.id,
                        widgetId = widgetId,
                        name = field.name,
                        fieldType = field.fieldType,
                        unit = field.unit,
                        sortOrder = field.sortOrder
                    )
                }
                repo.updateWidgetFields(uid, widgetId, mapped)
            }
        }
    }

    fun archiveWidget(widgetId: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.setProviderWidgetArchived(uid, widgetId, true)
        }
    }
}
