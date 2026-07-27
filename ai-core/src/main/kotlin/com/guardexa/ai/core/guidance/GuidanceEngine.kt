
package com.guardexa.ai.core.guidance

data class GuidanceMessage(
    val code: String,
    val priority: Int,
    val shouldSpeak: Boolean = false,
    val shouldVibrate: Boolean = false
)

class GuidanceEngine {
    fun fromExplanationCode(
        explanationCode: String
    ): GuidanceMessage =
        when (explanationCode) {
            "face_not_found" ->
                GuidanceMessage("guidance_look_at_screen", 80)

            "multiple_faces" ->
                GuidanceMessage("guidance_one_person_only", 90)

            "low_light" ->
                GuidanceMessage("guidance_improve_lighting", 70)

            "camera_obstructed" ->
                GuidanceMessage(
                    code = "guidance_clear_camera",
                    priority = 100,
                    shouldVibrate = true
                )

            "hold_still" ->
                GuidanceMessage("guidance_hold_still", 50)

            "eyes_not_visible" ->
                GuidanceMessage("guidance_show_eyes", 60)

            "spoof_suspected" ->
                GuidanceMessage(
                    code = "guidance_live_check_required",
                    priority = 100,
                    shouldVibrate = true
                )

            "glasses_not_detected" ->
                GuidanceMessage(
                    code = "guidance_wear_glasses",
                    priority = 95,
                    shouldSpeak = false,
                    shouldVibrate = true
                )

            "verification_uncertain",
            "collecting_more_evidence",
            "unstable_result" ->
                GuidanceMessage("guidance_wait_for_verification", 40)

            else ->
                GuidanceMessage("guidance_try_again", 20)
        }
}
