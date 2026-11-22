package com.mebiad.checkpilot.ui.generic.format

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// English whatever the phone's language: the dates match the designs and the crew's paperwork, like "01 Nov 2026".
private val DisplayDate = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private val DisplayDateWithDay = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)

/** The date as the app shows it, like "01 Nov 2026". */
fun LocalDate.toDisplayText(): String = format(DisplayDate)

/** The date with its weekday, like "Sun, 01 Nov 2026". */
fun LocalDate.toDisplayTextWithDay(): String = format(DisplayDateWithDay)
