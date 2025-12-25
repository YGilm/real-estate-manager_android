package com.example.real_estate_manager.util

import java.time.format.DateTimeFormatter
import java.util.Locale

val DATE_FMT_DD_MM_YYYY: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())
