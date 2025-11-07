package com.example.my_project.util

import java.time.format.DateTimeFormatter
import java.util.Locale

val DATE_FMT_DD_MM_YYYY: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())