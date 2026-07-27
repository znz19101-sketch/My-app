
package com.guardexa.privacy

data class PrivacyModeState(
    val enabled: Boolean,
    val hideSecurityImages: Boolean,
    val hideApplicationNames: Boolean,
    val hideEventDetails: Boolean,
    val requireReauthenticationForSensitiveScreens: Boolean
)

class PrivacyModeController(
    initialState: PrivacyModeState = PrivacyModeState(
        enabled = true,
        hideSecurityImages = true,
        hideApplicationNames = true,
        hideEventDetails = true,
        requireReauthenticationForSensitiveScreens = true
    )
) {
    private var state = initialState

    fun current(): PrivacyModeState = state

    fun update(next: PrivacyModeState) {
        state = next
    }

    fun maskApplicationName(value: String): String =
        if (state.enabled && state.hideApplicationNames) {
            "Protected application"
        } else {
            value
        }

    fun maskEventDetails(value: String): String =
        if (state.enabled && state.hideEventDetails) {
            "Protected event"
        } else {
            value
        }
}
