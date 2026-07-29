package com.guardexa.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.guardexa.ai.camera.CameraFrameConsumer
import com.guardexa.ai.camera.CameraXController
import com.guardexa.ai.core.model.AiPowerState
import com.guardexa.ai.core.model.AiVerificationState
import com.guardexa.ai.core.model.FaceQualityIssue
import com.guardexa.app.runtime.AiVerificationRepository
import com.guardexa.core.ui.theme.GuardexaTheme
import com.guardexa.ui.protection.ProtectionScreen
import com.guardexa.ui.protection.ProtectionScreenReason
import com.guardexa.ui.protection.ProtectionScreenState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProtectionGateActivity : ComponentActivity() {

    @Inject
    lateinit var cameraFrameConsumer: CameraFrameConsumer

    @Inject
    lateinit var aiVerificationRepository: AiVerificationRepository

    private var cameraController: CameraXController? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val initialReason = intent
            .getStringExtra(EXTRA_REASON)
            ?.let { value ->
                runCatching {
                    ProtectionScreenReason.valueOf(value)
                }.getOrNull()
            }
            ?: ProtectionScreenReason.APPLICATION_BLOCKED

        cameraController = CameraXController(
            context = applicationContext,
            lifecycleOwner = this,
            consumer = cameraFrameConsumer,
            powerStateProvider = { AiPowerState.ACTIVE },
            elapsedRealtime = { SystemClock.elapsedRealtime() }
        )

        setContent {
            val snapshot by aiVerificationRepository.state.collectAsState()

            val verificationState = snapshot.result?.state
            val verified = verificationState == AiVerificationState.VERIFIED

            LaunchedEffect(verified) {
                if (verified) {
                    finish()
                }
            }

            val reason = snapshotToReason(
                initialReason = initialReason,
                faceQualityIssue = snapshot.faceQualityIssue,
                verificationState = verificationState
            )

            GuardexaTheme {
                ProtectionScreen(
                    state = ProtectionScreenState(
                        reason = reason,
                        secondsRemaining = null,
                        rechecking = verificationState ==
                            AiVerificationState.UNCERTAIN,
                        allowedAppsAvailable = true,
                        administratorEntryAvailable = true
                    ),
                    onRecheck = {
                        aiVerificationRepository.clear()
                        startCameraWithPermissionCheck()
                    },
                    onOpenAllowedApps = {
                        finish()
                    },
                    onAdministratorEntry = {
                        finish()
                    }
                )
            }
        }

        startCameraWithPermissionCheck()
    }

    override fun onStop() {
        super.onStop()

        lifecycleScope.launch {
            cameraController?.stop()
        }
    }

    override fun onDestroy() {
        cameraController?.release()
        cameraController = null
        super.onDestroy()
    }

    private fun startCameraWithPermissionCheck() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        lifecycleScope.launch {
            runCatching {
                cameraController?.start()
            }
        }
    }

    private fun snapshotToReason(
        initialReason: ProtectionScreenReason,
        faceQualityIssue: FaceQualityIssue,
        verificationState: AiVerificationState?
    ): ProtectionScreenReason {
        return when {
            faceQualityIssue == FaceQualityIssue.NO_FACE ->
                ProtectionScreenReason.FACE_NOT_VISIBLE

            faceQualityIssue == FaceQualityIssue.LOW_LIGHT ->
                ProtectionScreenReason.LOW_LIGHT

            faceQualityIssue == FaceQualityIssue.CAMERA_OBSTRUCTED ->
                ProtectionScreenReason.CAMERA_OBSTRUCTED

            verificationState == AiVerificationState.FAILED ->
                ProtectionScreenReason.GLASSES_REQUIRED

            verificationState == AiVerificationState.INVALID_INPUT ->
                ProtectionScreenReason.FACE_NOT_VISIBLE

            else -> initialReason
        }
    }

    companion object {
        const val EXTRA_REASON = "guardexa_protection_reason"
    }
}
