
package com.guardexa.device.compatibility

import android.os.Build

enum class ManufacturerFamily {
    HONOR,
    HUAWEI,
    SAMSUNG,
    XIAOMI,
    OPPO,
    ONEPLUS,
    GOOGLE,
    MOTOROLA,
    OTHER
}

data class CompatibilityAdvice(
    val manufacturer: ManufacturerFamily,
    val requiresBatteryReview: Boolean,
    val requiresAutoStartReview: Boolean,
    val notesCode: String
)

class ManufacturerCompatibility {
    fun detect(): CompatibilityAdvice {
        val value = Build.MANUFACTURER.orEmpty().lowercase()

        val family = when {
            "honor" in value -> ManufacturerFamily.HONOR
            "huawei" in value -> ManufacturerFamily.HUAWEI
            "samsung" in value -> ManufacturerFamily.SAMSUNG
            "xiaomi" in value || "redmi" in value ->
                ManufacturerFamily.XIAOMI
            "oppo" in value -> ManufacturerFamily.OPPO
            "oneplus" in value -> ManufacturerFamily.ONEPLUS
            "google" in value -> ManufacturerFamily.GOOGLE
            "motorola" in value -> ManufacturerFamily.MOTOROLA
            else -> ManufacturerFamily.OTHER
        }

        return when (family) {
            ManufacturerFamily.HONOR ->
                CompatibilityAdvice(
                    manufacturer = family,
                    requiresBatteryReview = true,
                    requiresAutoStartReview = true,
                    notesCode = "compat_honor_background_settings"
                )

            ManufacturerFamily.HUAWEI,
            ManufacturerFamily.XIAOMI,
            ManufacturerFamily.OPPO ->
                CompatibilityAdvice(
                    manufacturer = family,
                    requiresBatteryReview = true,
                    requiresAutoStartReview = true,
                    notesCode = "compat_vendor_background_settings"
                )

            ManufacturerFamily.SAMSUNG,
            ManufacturerFamily.ONEPLUS,
            ManufacturerFamily.MOTOROLA ->
                CompatibilityAdvice(
                    manufacturer = family,
                    requiresBatteryReview = true,
                    requiresAutoStartReview = false,
                    notesCode = "compat_battery_settings"
                )

            ManufacturerFamily.GOOGLE,
            ManufacturerFamily.OTHER ->
                CompatibilityAdvice(
                    manufacturer = family,
                    requiresBatteryReview = false,
                    requiresAutoStartReview = false,
                    notesCode = "compat_standard"
                )
        }
    }
}
