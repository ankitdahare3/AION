package com.aion.host.devicestatus

import java.util.Locale

/** Pure formatting so it's unit-testable without any Android dependency. `Locale.ROOT` pins the
 * decimal separator to "." regardless of device locale — otherwise a German/French-locale device
 * would render "8,0 GB", inconsistent with every other number in this app. */
internal fun formatGb(bytes: Long): String {
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return String.format(Locale.ROOT, "%.1f GB", gb)
}
